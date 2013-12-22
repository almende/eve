package com.almende.eve.agent;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.net.ProtocolException;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.almende.eve.agent.annotation.ThreadSafe;
import com.almende.eve.agent.callback.AsyncCallback;
import com.almende.eve.agent.callback.CallbackInterface;
import com.almende.eve.agent.callback.CallbackService;
import com.almende.eve.agent.callback.SyncCallback;
import com.almende.eve.agent.log.EventLogger;
import com.almende.eve.agent.proxy.AsyncProxy;
import com.almende.eve.config.Config;
import com.almende.eve.rpc.RequestParams;
import com.almende.eve.rpc.annotation.Sender;
import com.almende.eve.rpc.jsonrpc.JSONRPC;
import com.almende.eve.rpc.jsonrpc.JSONRPCException;
import com.almende.eve.rpc.jsonrpc.JSONRPCException.CODE;
import com.almende.eve.rpc.jsonrpc.JSONRequest;
import com.almende.eve.rpc.jsonrpc.JSONResponse;
import com.almende.eve.rpc.jsonrpc.jackson.JOM;
import com.almende.eve.scheduler.Scheduler;
import com.almende.eve.scheduler.SchedulerFactory;
import com.almende.eve.state.State;
import com.almende.eve.state.StateFactory;
import com.almende.eve.state.TypedKey;
import com.almende.eve.transport.TransportService;
import com.almende.eve.transport.http.HttpService;
import com.almende.util.ClassUtil;
import com.almende.util.ObjectCache;
import com.almende.util.TypeUtil;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

public final class AgentHost implements AgentHostInterface {
	
	private static final Logger																	LOG					= Logger.getLogger(AgentHost.class
																															.getSimpleName());
	private static final AgentHost																HOST				= new AgentHost();
	private final ConcurrentHashMap<String, TransportService>									transportServices	= new ConcurrentHashMap<String, TransportService>();
	private final ConcurrentHashMap<String, CallbackInterface>									callbacks			= new ConcurrentHashMap<String, CallbackInterface>();
	private StateFactory																		stateFactory		= null;
	private SchedulerFactory																	schedulerFactory	= null;
	private Config																				config				= null;
	private final EventLogger																	eventLogger			= new EventLogger(
																															this);
	private boolean																				doesShortcut		= true;
	
	private final ConcurrentHashMap<String, ConcurrentHashMap<TypedKey<?>, WeakReference<?>>>	refStore			= new ConcurrentHashMap<String, ConcurrentHashMap<TypedKey<?>, WeakReference<?>>>();
	
	private static final RequestParams															EVEREQUESTPARAMS	= new RequestParams();
	static {
		EVEREQUESTPARAMS.put(Sender.class, null);
	}
	
	private AgentHost() {
		this.addTransportService(new HttpService());
	}
	
	/**
	 * Get the shared AgentHost instance
	 * 
	 * @return factory Returns the host instance
	 */
	public static AgentHost getInstance() {
		return HOST;
	}
	
	@Override
	// TODO: prevent duplication of Services
	public void loadConfig(Config config) {
		HOST.setConfig(config);
		if (config != null) {
			ObjectCache.get("agents").configCache(config);
			// initialize all factories for state, transport, and scheduler
			// important to initialize in the correct order: cache first,
			// then the state and transport services, and lastly scheduler.
			HOST.setStateFactory(config);
			HOST.addTransportServices(config);
			HOST.setSchedulerFactory(config);
			HOST.addAgents(config);
		}
	}
	
	@Override
	public void signalAgents(AgentSignal<?> event) {
		if (stateFactory != null) {
			Iterator<String> iter = stateFactory.getAllAgentIds();
			if (iter != null) {
				while (iter.hasNext()) {
					try {
						Agent agent = getAgent(iter.next());
						if (agent != null) {
							agent.signalAgent(event);
						}
					} catch (Exception e) {
						LOG.log(Level.WARNING, "Couldn't signal agent.", e);
					}
				}
			}
		}
	}
	
	@Override
	public Agent getAgent(String agentId) throws JSONRPCException,
			ClassNotFoundException, InstantiationException,
			IllegalAccessException, InvocationTargetException,
			NoSuchMethodException, IOException {
		
		if (agentId == null) {
			return null;
		}
		
		// Check if agent is instantiated already, returning if it is:
		Agent agent = ObjectCache.get("agents").get(agentId, Agent.class);
		if (agent != null) {
			return agent;
		}
		// No agent found, normal initialization:
		
		// load the State
		State state = null;
		if (getStateFactory() == null) {
			return null;
		}
		state = getStateFactory().get(agentId);
		if (state == null) {
			// agent does not exist
			return null;
		}
		state.init();
		
		// read the agents class name from state
		Class<?> agentType = state.getAgentType();
		if (agentType == null) {
			throw new JSONRPCException("Cannot instantiate agent. "
					+ "Class information missing in the agents state "
					+ "(agentId='" + agentId + "')");
		}
		
		if (!Agent.class.isAssignableFrom(agentType)) {
			// Found state info not representing an Agent, like e.g. TokenStore
			// or CookieStore.
			return null;
		}
		
		// instantiate the agent
		agent = (Agent) agentType.getConstructor().newInstance();
		agent.constr(this, state);
		agent.signalAgent(new AgentSignal<Void>(AgentSignal.INIT));
		
		if (agentType.isAnnotationPresent(ThreadSafe.class)
				&& agentType.getAnnotation(ThreadSafe.class).value()) {
			ObjectCache.get("agents").put(agentId, agent);
		}
		
		return agent;
	}
	
	@Deprecated
	@Override
	public <T extends AgentInterface> T createAgentProxy(final URI receiverUrl,
			Class<T> agentInterface) {
		return createAgentProxy(null, receiverUrl, agentInterface);
	}
	
	@SuppressWarnings("unchecked")
	@Override
	public <T extends AgentInterface> T createAgentProxy(
			final AgentInterface sender, final URI receiverUrl,
			Class<T> agentInterface) {
		if (!ClassUtil.hasInterface(agentInterface, AgentInterface.class)) {
			throw new IllegalArgumentException("agentInterface must extend "
					+ AgentInterface.class.getName());
		}
		
		// TODO: In the new model the proxy agents need to have an adres as
		// well! This will enforce usage of the agentCache!
		final String proxyId = "proxy_"
				+ (sender != null ? sender.getId() + "_" : "")
				+ agentInterface.getCanonicalName().replace(' ', '_');
		T proxy = ObjectCache.get("agents").get(proxyId, agentInterface);
		if (proxy != null) {
			return proxy;
		}
		
		// http://docs.oracle.com/javase/1.4.2/docs/guide/reflection/proxy.html
		proxy = (T) Proxy.newProxyInstance(agentInterface.getClassLoader(),
				new Class[] { agentInterface }, new InvocationHandler() {
					public Object invoke(Object proxy, Method method,
							Object[] args) throws JSONRPCException,
							JsonParseException, JsonMappingException,
							IOException {
						
						AgentInterface agent = sender;
						if (agent == null) {
							agent = (T) proxy;
						}
						
						// TODO: if method calls for Namespace getter, return
						// new proxy for subtype. All calls to that proxy need
						// to add namespace to method name for JSON-RPC.
						if (method.getName().equals("getId")) {
							return proxyId;
						} else if (method.getName().equals("receive")
								&& args.length > 1) {
							JSONResponse response = null;
							if (args[0] != null) {
								if (args[0] instanceof String) {
									String message = (String) args[0];
									if (message.startsWith("{")
											|| message.trim().startsWith("{")) {
										
										ObjectNode json = JOM.getInstance()
												.readValue(message,
														ObjectNode.class);
										if (JSONRPC.isResponse(json)) {
											response = new JSONResponse(json);
										}
									}
								} else if (args[0] instanceof ObjectNode) {
									ObjectNode json = (ObjectNode) args[0];
									if (JSONRPC.isResponse(json)) {
										response = new JSONResponse(json);
									}
								} else if (args[0] instanceof JSONResponse) {
									response = (JSONResponse) args[0];
								} else {
									LOG.warning("Strange:"
											+ args[0]
											+ " "
											+ args[0].getClass()
													.getCanonicalName());
								}
							}
							if (response != null) {
								
								if (callbacks != null) {
									JsonNode id = null;
									if (response.getId() != null) {
										id = response.getId();
									}
									CallbackInterface callbacks = getCallbackService(proxyId);
									AsyncCallback<JSONResponse> callback = callbacks.get(id);
									if (callback != null) {
										if (response.getError() != null) {
											callback.onFailure(response
													.getError());
										} else {
											callback.onSuccess(response);
										}
									}
								}
							}
							return null;
						} else {
							
							JSONRequest request = JSONRPC.createRequest(method,
									args);
							
							SyncCallback<JSONResponse> callback = new SyncCallback<JSONResponse>();
							CallbackInterface callbacks = getCallbackService(proxyId);
							callbacks.store(request.getId(),callback);
							
							try {
								sendAsync(receiverUrl, request, agent, null);
							} catch (IOException e1) {
								throw new JSONRPCException(
										CODE.REMOTE_EXCEPTION, "", e1);
							}
							
							JSONResponse response;
							try {
								response = callback.get();
							} catch (Exception e) {
								throw new JSONRPCException(
										CODE.REMOTE_EXCEPTION, "", e);
							}
							JSONRPCException err = response.getError();
							if (err != null) {
								throw err;
							} else if (response.getResult() != null
									&& !method.getReturnType()
											.equals(Void.TYPE)) {
								return TypeUtil.inject(response.getResult(),
										method.getGenericReturnType());
							} else {
								return null;
							}
						}
					}
				});
		
		ObjectCache.get("agents").put(proxyId, proxy);
		
		return proxy;
	}
	
	@Deprecated
	@Override
	public <T extends AgentInterface> AsyncProxy<T> createAsyncAgentProxy(
			final URI receiverUrl, Class<T> agentInterface) {
		return createAsyncAgentProxy(null, receiverUrl, agentInterface);
	}
	
	@Override
	public <T extends AgentInterface> AsyncProxy<T> createAsyncAgentProxy(
			final AgentInterface sender, final URI receiverUrl,
			Class<T> agentInterface) {
		return new AsyncProxy<T>(createAgentProxy(sender, receiverUrl,
				agentInterface));
	}
	
	@SuppressWarnings("unchecked")
	@Override
	public <T extends Agent> T createAgent(String agentType, String agentId)
			throws JSONRPCException, InstantiationException,
			IllegalAccessException, InvocationTargetException,
			NoSuchMethodException, ClassNotFoundException, IOException {
		return (T) createAgent((Class<T>) Class.forName(agentType), agentId);
	}
	
	@SuppressWarnings("unchecked")
	@Override
	public <T extends Agent> T createAgent(Class<T> agentType, String agentId)
			throws JSONRPCException, InstantiationException,
			IllegalAccessException, InvocationTargetException,
			NoSuchMethodException, IOException {
		if (!ClassUtil.hasSuperClass(agentType, Agent.class)) {
			return (T) createAspectAgent(agentType, agentId);
		}
		
		// validate the Eve agent and output as warnings
		List<String> errors = JSONRPC.validate(agentType, EVEREQUESTPARAMS);
		for (String error : errors) {
			LOG.warning("Validation error class: " + agentType.getName()
					+ ", message: " + error);
		}
		
		// create the state
		State state = getStateFactory().create(agentId);
		state.setAgentType(agentType);
		state.init();
		
		// instantiate the agent
		T agent = (T) agentType.getConstructor().newInstance();
		agent.constr(this, state);
		agent.signalAgent(new AgentSignal<Void>(AgentSignal.CREATE));
		agent.signalAgent(new AgentSignal<Void>(AgentSignal.INIT));
		
		if (agentType.isAnnotationPresent(ThreadSafe.class)
				&& agentType.getAnnotation(ThreadSafe.class).value()) {
			ObjectCache.get("agents").put(agentId, agent);
		}
		
		return agent;
	}
	
	@Override
	public <T> AspectAgent<T> createAspectAgent(Class<? extends T> aspect,
			String agentId) throws JSONRPCException, InstantiationException,
			IllegalAccessException, InvocationTargetException,
			NoSuchMethodException, IOException {
		@SuppressWarnings("unchecked")
		AspectAgent<T> result = createAgent(AspectAgent.class, agentId);
		result.init(aspect);
		return result;
	}
	
	@Override
	public void deleteAgent(String agentId) {
		if (agentId == null) {
			return;
		}
		Agent agent = null;
		try {
			agent = getAgent(agentId);
		} catch (Exception e) {
			LOG.log(Level.WARNING, "Couldn't get agent to delete.", e);
		}
		if (agent != null) {
			if (getScheduler(agent) != null) {
				schedulerFactory.destroyScheduler(agentId);
			}
			try {
				// get the agent and execute the delete method
				agent.signalAgent(new AgentSignal<Void>(AgentSignal.DESTROY));
				agent.signalAgent(new AgentSignal<Void>(AgentSignal.DELETE));
				ObjectCache.get("agents").delete(agentId);
				agent = null;
			} catch (Exception e) {
				LOG.log(Level.WARNING, "Error deleting agent:" + agentId, e);
			}
		}
		// delete the state, even if the agent.destroy or agent.delete
		// failed.
		getStateFactory().delete(agentId);
	}
	
	@Override
	public boolean hasAgent(String agentId) throws JSONRPCException {
		return getStateFactory().exists(agentId);
	}
	
	@Override
	public EventLogger getEventLogger() {
		return eventLogger;
	}
	
	@Override
	public void receive(String receiverId, Object message, String senderUrl,
			String tag) {
		AgentInterface receiver = null;
		try {
			receiver = getAgent(receiverId);
			if (receiver == null) {
				// Check if there might be a proxy in the objectcache:
				receiver = ObjectCache.get("agents").get(receiverId,
						AgentInterface.class);
			}
			if (receiver != null) {
				URI senderUri = null;
				if (senderUrl != null) {
					senderUri = URI.create(senderUrl);
				}
				receiver.receive(message, senderUri, tag);
			}
		} catch (Exception e) {
			LOG.log(Level.WARNING, "", e);
			if (senderUrl != null) {
				try {
					sendAsync(URI.create(senderUrl), new JSONRPCException(
							CODE.REMOTE_EXCEPTION, "", e), receiver, tag);
				} catch (Exception e1) {
					LOG.log(Level.WARNING,
							"Couldn't send exception to remote agent!", e1);
				}
			}
		}
	}
	
	@Override
	public void sendAsync(final URI receiverUrl, final Object message,
			final AgentInterface sender, final String tag) throws IOException {
		final String receiverId = getAgentId(receiverUrl.toASCIIString());
		String protocol = receiverUrl.getScheme();
		if (("local".equals(protocol)) || (doesShortcut && receiverId != null)) {
			// local shortcut
			new Thread(new Runnable() {
				@Override
				public void run() {
					String senderUrl = null;
					if (sender != null) {
						senderUrl = getSenderUrl(sender.getId(),
								receiverUrl.toASCIIString()).toASCIIString();
					}
					receive(receiverId, message, senderUrl, tag);
				}
			}).start();
		} else {
			TransportService service = null;
			URI senderUrl = null;
			if (sender != null) {
				senderUrl = getSenderUrl(sender.getId(),
						receiverUrl.toASCIIString());
			}
			service = getTransportService(protocol);
			if (service != null) {
				service.sendAsync(senderUrl.toASCIIString(),
						receiverUrl.toASCIIString(), message, tag);
			} else {
				throw new ProtocolException(
						"No transport service configured for protocol '"
								+ protocol + "'.");
			}
		}
	}
	
	@Override
	public String getAgentId(String agentUrl) {
		if (agentUrl.startsWith("local:")) {
			return agentUrl.replaceFirst("local:/?/?", "");
		}
		for (TransportService service : transportServices.values()) {
			String agentId = service.getAgentId(agentUrl);
			if (agentId != null) {
				return agentId;
			}
		}
		return null;
	}
	
	@Override
	public URI getSenderUrl(String agentId, String receiverUrl) {
		if (receiverUrl.startsWith("local:")) {
			return URI.create("local:" + agentId);
		}
		for (TransportService service : transportServices.values()) {
			List<String> protocols = service.getProtocols();
			for (String protocol : protocols) {
				if (receiverUrl.startsWith(protocol + ":")) {
					String senderUrl = service.getAgentUrl(agentId);
					if (senderUrl != null) {
						return URI.create(senderUrl);
					}
				}
			}
		}
		return null;
	}
	
	@Override
	public void setConfig(Config config) {
		this.config = config;
	}
	
	@Override
	public Config getConfig() {
		return config;
	}
	
	@Override
	public <T> T getRef(String agentId, TypedKey<T> key) {
		ConcurrentHashMap<TypedKey<?>, WeakReference<?>> objects = refStore
				.get(agentId);
		if (objects != null) {
			return TypeUtil.inject(objects.get(key).get(), key.getType());
		}
		
		return null;
	}
	
	@Override
	public <T> void putRef(String agentId, TypedKey<T> key, T value) {
		synchronized (refStore) {
			ConcurrentHashMap<TypedKey<?>, WeakReference<?>> objects = refStore
					.get(agentId);
			if (objects == null) {
				objects = new ConcurrentHashMap<TypedKey<?>, WeakReference<?>>();
			}
			objects.put(key, new WeakReference<Object>(value));
			refStore.put(agentId, objects);
		}
	}
	
	@Override
	public boolean isDoesShortcut() {
		return doesShortcut;
	}
	
	@Override
	public void setDoesShortcut(boolean doesShortcut) {
		this.doesShortcut = doesShortcut;
	}
	
	public StateFactory getStateFactoryFromConfig(Config config,
			String configName) {
		StateFactory result = null;
		// get the class name from the config file
		// first read from the environment specific configuration,
		// if not found read from the global configuration
		
		String className = config.get(configName, "class");
		if (className == null) {
			if (!configName.equals("state")) {
				// Provide fallback to state if other type doesn't exist;
				configName = "state";
				className = config.get(configName, "class");
			}
			if (className == null) {
				throw new IllegalArgumentException("Config parameter '"
						+ config + ".class' missing in Eve configuration.");
			}
		}
		
		try {
			// get the class
			Class<?> stateClass = Class.forName(className);
			if (!ClassUtil.hasInterface(stateClass, StateFactory.class)) {
				throw new IllegalArgumentException("State factory class "
						+ stateClass.getName() + " must extend "
						+ State.class.getName());
			}
			
			// instantiate the state factory
			Map<String, Object> params = config.get(configName);
			result = (StateFactory) stateClass.getConstructor(Map.class)
					.newInstance(params);
			
			LOG.info("Initialized state factory: " + result.toString());
		} catch (Exception e) {
			LOG.log(Level.WARNING, "", e);
		}
		return result;
	}
	
	@Override
	public void setStateFactory(Config config) {
		if (this.stateFactory != null) {
			LOG.warning("Not loading statefactory from config, there is already a statefactory available.");
			return;
		}
		
		setStateFactory(getStateFactoryFromConfig(config, "state"));
	}
	
	@Override
	public void addAgents(Config config) {
		Map<String, String> agents = config.get("bootstrap", "agents");
		if (agents != null) {
			for (Entry<String, String> entry : agents.entrySet()) {
				String agentId = entry.getKey();
				String agentType = entry.getValue();
				try {
					Agent agent = getAgent(agentId);
					if (agent == null) {
						// agent does not yet exist. create it
						agent = createAgent(agentType, agentId);
						LOG.info("Bootstrap created agent id=" + agentId
								+ ", type=" + agentType);
					}
				} catch (Exception e) {
					LOG.log(Level.WARNING, "", e);
				}
			}
		}
	}
	
	@Override
	public void setStateFactory(StateFactory stateFactory) {
		if (this.stateFactory != null) {
			LOG.warning("Not setting new stateFactory, there is already a factory initialized.");
			return;
		}
		this.stateFactory = stateFactory;
		HOST.signalAgents(new AgentSignal<StateFactory>(
				AgentSignal.SETSTATEFACTORY, stateFactory));
		
	}
	
	@Override
	public StateFactory getStateFactory() {
		if (stateFactory == null) {
			LOG.warning("No state factory initialized.");
		}
		return stateFactory;
	}
	
	@Override
	public void setSchedulerFactory(Config config) {
		// get the class name from the config file
		// first read from the environment specific configuration,
		// if not found read from the global configuration
		String className = config.get("scheduler", "class");
		if (className == null) {
			throw new IllegalArgumentException(
					"Config parameter 'scheduler.class' missing in Eve configuration.");
		}
		
		// read all scheduler params (will be fed to the scheduler factory
		// on construction)
		Map<String, Object> params = config.get("scheduler");
		
		try {
			// get the class
			Class<?> schedulerClass = Class.forName(className);
			if (!ClassUtil.hasInterface(schedulerClass, SchedulerFactory.class)) {
				throw new IllegalArgumentException("Scheduler class "
						+ schedulerClass.getName() + " must implement "
						+ SchedulerFactory.class.getName());
			}
			
			// initialize the scheduler factory
			SchedulerFactory sf = (SchedulerFactory) schedulerClass
					.getConstructor(AgentHost.class, Map.class).newInstance(
							this, params);
			
			setSchedulerFactory(sf);
			
			LOG.info("Initialized scheduler factory: "
					+ sf.getClass().getName());
		} catch (Exception e) {
			LOG.log(Level.WARNING, "", e);
		}
	}
	
	@Override
	public void addTransportServices(Config config) {
		if (config == null) {
			Exception e = new Exception("Configuration uninitialized");
			LOG.log(Level.WARNING, "", e);
			return;
		}
		
		// read global service params
		List<Map<String, Object>> allTransportParams = config
				.get("transport_services");
		if (allTransportParams == null) {
			// TODO: cleanup some day. deprecated since 2013-01-17
			allTransportParams = config.get("services");
			if (allTransportParams != null) {
				LOG.warning("Property 'services' is deprecated. Use 'transport_services' instead.");
			}
		}
		
		if (allTransportParams != null) {
			int index = 0;
			for (Map<String, Object> transportParams : allTransportParams) {
				String className = (String) transportParams.get("class");
				try {
					if (className != null) {
						
						// Recognize known classes by their short name,
						// and replace the short name for the full class path
						className = Config.map(className);
						// get class
						Class<?> transportClass = Class.forName(className);
						if (!ClassUtil.hasInterface(transportClass,
								TransportService.class)) {
							throw new IllegalArgumentException(
									"TransportService class "
											+ transportClass.getName()
											+ " must implement "
											+ TransportService.class.getName());
						}
						
						// initialize the transport service
						TransportService transport = (TransportService) transportClass
								.getConstructor(AgentHost.class, Map.class)
								.newInstance(this, transportParams);
						
						// register the service with the agent factory
						addTransportService(transport);
					} else {
						LOG.warning("Cannot load transport service at index "
								+ index + ": no class defined.");
					}
				} catch (Exception e) {
					LOG.log(Level.WARNING, "Cannot load service at index "
							+ index, e);
				}
				index++;
			}
		}
	}
	
	@Override
	public void addTransportService(TransportService transportService) {
		if (!transportServices.contains(transportService.getKey())) {
			transportServices.put(transportService.getKey(), transportService);
			LOG.info("Registered transport service: "
					+ transportService.toString());
			if (HOST != null) {
				HOST.signalAgents(new AgentSignal<TransportService>(
						AgentSignal.ADDTRANSPORTSERVICE, transportService));
			}
		} else {
			LOG.warning("Not adding transport service, as it already exists.");
		}
	}
	
	@Override
	public void removeTransportService(TransportService transportService) {
		transportServices.remove(transportService);
		LOG.info("Unregistered transport service "
				+ transportService.toString());
		HOST.signalAgents(new AgentSignal<TransportService>(
				AgentSignal.DELTRANSPORTSERVICE, transportService));
		
	}
	
	@Override
	public List<TransportService> getTransportServices() {
		// TODO: check efficiency of this method, is there something simpler?
		return Collections.list(Collections.enumeration(transportServices
				.values()));
	}
	
	@Override
	public List<TransportService> getTransportServices(String protocol) {
		List<TransportService> filteredServices = new ArrayList<TransportService>();
		
		for (TransportService service : transportServices.values()) {
			List<String> protocols = service.getProtocols();
			if (protocols.contains(protocol)) {
				filteredServices.add(service);
			}
		}
		
		return filteredServices;
	}
	
	@Override
	public TransportService getTransportService(String protocol) {
		List<TransportService> services = getTransportServices(protocol);
		if (services.size() > 0) {
			return services.get(0);
		}
		return null;
	}
	
	@Override
	public List<Object> getMethods(Agent agent) {
		return JSONRPC.describe(agent, EVEREQUESTPARAMS);
	}
	
	@Override
	public void setSchedulerFactory(SchedulerFactory schedulerFactory) {
		if (this.schedulerFactory != null) {
			LOG.warning("Replacing earlier schedulerFactory.");
		}
		this.schedulerFactory = schedulerFactory;
		HOST.signalAgents(new AgentSignal<SchedulerFactory>(
				AgentSignal.SETSCHEDULERFACTORY, schedulerFactory));
	}
	
	@Override
	public Scheduler getScheduler(Agent agent) {
		if (schedulerFactory == null) {
			return null;
		}
		return schedulerFactory.getScheduler(agent);
	}
	
	public synchronized CallbackInterface getCallbackService(String id) {
		CallbackInterface result = callbacks.get(id);
		if (result == null) {
			result = new CallbackService();
			callbacks.put(id, result);
		}
		return result;
	}
	
}
