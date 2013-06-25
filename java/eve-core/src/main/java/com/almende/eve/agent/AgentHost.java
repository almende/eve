package com.almende.eve.agent;

import java.io.IOException;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.net.ProtocolException;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.almende.eve.agent.annotation.ThreadSafe;
import com.almende.eve.agent.log.EventLogger;
import com.almende.eve.agent.proxy.AsyncProxy;
import com.almende.eve.config.Config;
import com.almende.eve.rpc.RequestParams;
import com.almende.eve.rpc.annotation.Sender;
import com.almende.eve.rpc.jsonrpc.JSONRPC;
import com.almende.eve.rpc.jsonrpc.JSONRPCException;
import com.almende.eve.rpc.jsonrpc.JSONRequest;
import com.almende.eve.rpc.jsonrpc.JSONResponse;
import com.almende.eve.scheduler.Scheduler;
import com.almende.eve.scheduler.SchedulerFactory;
import com.almende.eve.state.State;
import com.almende.eve.state.StateFactory;
import com.almende.eve.transport.AsyncCallback;
import com.almende.eve.transport.TransportService;
import com.almende.eve.transport.http.HttpService;
import com.almende.util.ClassUtil;
import com.almende.util.TypeUtil;

public final class AgentHost implements AgentHostInterface {
	
	private static final Logger							LOG					= Logger.getLogger(AgentHost.class
																					.getSimpleName());
	private static final AgentHost						FACTORY				= new AgentHost();
	private ConcurrentHashMap<String, TransportService>	transportServices	= new ConcurrentHashMap<String, TransportService>();
	private StateFactory								stateFactory		= null;
	private SchedulerFactory							schedulerFactory	= null;
	private Config										config				= null;
	private EventLogger									eventLogger			= new EventLogger(
																					this);
	private boolean										doesShortcut		= true;
	
	/*
	 * Several classname maps for configuration conveniency:
	 */
	private static final Map<String, String>			STATE_FACTORIES		= new HashMap<String, String>();
	private static final Map<String, String>			SCHEDULERS			= new HashMap<String, String>();
	private static final Map<String, String>			TRANSPORT_SERVICES	= new HashMap<String, String>();
	private static final RequestParams					EVEREQUESTPARAMS	= new RequestParams();
	static {
		STATE_FACTORIES.put("FileStateFactory",
				"com.almende.eve.state.FileStateFactory");
		STATE_FACTORIES.put("MemoryStateFactory",
				"com.almende.eve.state.MemoryStateFactory");
		STATE_FACTORIES.put("DatastoreStateFactory",
				"com.almende.eve.state.google.DatastoreStateFactory");
	}
	static {
		SCHEDULERS.put("RunnableSchedulerFactory",
				"com.almende.eve.scheduler.RunnableSchedulerFactory");
		SCHEDULERS.put("ClockSchedulerFactory",
				"com.almende.eve.scheduler.ClockSchedulerFactory");
		SCHEDULERS.put("GaeSchedulerFactory",
				"com.almende.eve.scheduler.google.GaeSchedulerFactory");
	}
	static {
		TRANSPORT_SERVICES.put("XmppService",
				"com.almende.eve.transport.xmpp.XmppService");
		TRANSPORT_SERVICES.put("HttpService",
				"com.almende.eve.transport.http.HttpService");
	}
	static {
		EVEREQUESTPARAMS.put(Sender.class, null);
	}
	
	private AgentHost() {
		this.addTransportService(new HttpService());
	}
	
	/**
	 * Get the shared AgentHost instance
	 * 
	 * @return factory Returns the factory instance
	 */
	public static AgentHost getInstance() {
		return FACTORY;
	}
	
	@Override
	// TODO: prevent duplication of Services
	public void loadConfig(Config config) {
		FACTORY.setConfig(config);
		if (config != null) {
			AgentCache.configCache(config);
			// initialize all factories for state, transport, and scheduler
			// important to initialize in the correct order: cache first,
			// then the state and transport services, and lastly scheduler.
			FACTORY.setStateFactory(config);
			FACTORY.addTransportServices(config);
			FACTORY.setSchedulerFactory(config);
			FACTORY.addAgents(config);
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
		Agent agent = AgentCache.get(agentId);
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
		
		// instantiate the agent
		agent = (Agent) agentType.getConstructor().newInstance();
		agent.constr(this, state);
		agent.signalAgent(new AgentSignal<Void>("init"));
		
		if (agentType.isAnnotationPresent(ThreadSafe.class)
				&& agentType.getAnnotation(ThreadSafe.class).value()) {
			AgentCache.put(agentId, agent);
		}
		
		return agent;
	}
	
	@Deprecated
	@Override
	public <T> T createAgentProxy(final URI receiverUrl, Class<T> agentInterface) {
		return createAgentProxy(null, receiverUrl, agentInterface);
	}
	
	@SuppressWarnings("unchecked")
	@Override
	public <T> T createAgentProxy(final AgentInterface sender,
			final URI receiverUrl, Class<T> agentInterface) {
		if (!ClassUtil.hasInterface(agentInterface, AgentInterface.class)) {
			throw new IllegalArgumentException("agentInterface must extend "
					+ AgentInterface.class.getName());
		}
		
		// http://docs.oracle.com/javase/1.4.2/docs/guide/reflection/proxy.html
		T proxy = (T) Proxy.newProxyInstance(agentInterface.getClassLoader(),
				new Class[] { agentInterface }, new InvocationHandler() {
					public Object invoke(Object proxy, Method method,
							Object[] args) throws ProtocolException,
							JSONRPCException {
						JSONRequest request = JSONRPC.createRequest(method,
								args);
						JSONResponse response = send(sender, receiverUrl,
								request);
						
						JSONRPCException err = response.getError();
						if (err != null) {
							throw err;
						} else if (response.getResult() != null
								&& !method.getReturnType().equals(Void.TYPE)) {
							return TypeUtil.inject(
									method.getGenericReturnType(),
									response.getResult());
						} else {
							return null;
						}
					}
				});
		
		// TODO: for optimization, one can cache the created proxy's
		
		return proxy;
	}
	
	@Deprecated
	@Override
	public <T> AsyncProxy<T> createAsyncAgentProxy(final URI receiverUrl,
			Class<T> agentInterface) {
		return createAsyncAgentProxy(null, receiverUrl, agentInterface);
	}
	
	@Override
	public <T> AsyncProxy<T> createAsyncAgentProxy(final AgentInterface sender,
			final URI receiverUrl, Class<T> agentInterface) {
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
		agent.signalAgent(new AgentSignal<Void>("create"));
		agent.signalAgent(new AgentSignal<Void>("init"));
		
		if (agentType.isAnnotationPresent(ThreadSafe.class)
				&& agentType.getAnnotation(ThreadSafe.class).value()) {
			AgentCache.put(agentId, agent);
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
				agent.signalAgent(new AgentSignal<Void>("destroy"));
				agent.signalAgent(new AgentSignal<Void>("delete"));
				AgentCache.delete(agentId);
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
	public JSONResponse receive(String receiverId, JSONRequest request,
			RequestParams requestParams) throws JSONRPCException {
		try {
			Agent receiver = getAgent(receiverId);
			if (receiver != null) {
				JSONResponse response = JSONRPC.invoke(receiver, request,
						requestParams, receiver);
				return response;
			}
		} catch (Exception e) {
			throw new JSONRPCException("Couldn't instantiate agent for id '"
					+ receiverId + "'", e);
		}
		throw new JSONRPCException("Agent with id '" + receiverId
				+ "' not found");
	}
	
	@Deprecated
	@Override
	public JSONResponse send(URI receiverUrl, JSONRequest request)
			throws ProtocolException, JSONRPCException {
		return send(null, receiverUrl, request);
	}
	
	@Override
	public JSONResponse send(AgentInterface sender, URI receiverUrl,
			JSONRequest request) throws ProtocolException, JSONRPCException {
		String receiverId = getAgentId(receiverUrl.toASCIIString());
		String protocol = receiverUrl.getScheme();
		String senderUrl = null;
		if (sender != null) {
			senderUrl = getSenderUrl(sender.getId(),
					receiverUrl.toASCIIString());
		}
		
		if ("local".equals(protocol) || (doesShortcut && receiverId != null)) {
			// local shortcut
			RequestParams requestParams = new RequestParams();
			requestParams.put(Sender.class, senderUrl);
			return receive(receiverId, request, requestParams);
		} else {
			TransportService service = null;
			service = getTransportService(protocol);
			
			if (service != null) {
				JSONResponse response = service.send(senderUrl,
						receiverUrl.toASCIIString(), request);
				return response;
			} else {
				throw new ProtocolException(
						"No transport service configured for protocol '"
								+ protocol + "'.");
			}
		}
	}
	
	@Deprecated
	@Override
	public void sendAsync(final URI receiverUrl, final JSONRequest request,
			final AsyncCallback<JSONResponse> callback)
			throws ProtocolException, JSONRPCException {
		sendAsync(null, receiverUrl, request, callback);
	}
	
	@Override
	public void sendAsync(final AgentInterface sender, final URI receiverUrl,
			final JSONRequest request,
			final AsyncCallback<JSONResponse> callback)
			throws JSONRPCException, ProtocolException {
		final String receiverId = getAgentId(receiverUrl.toASCIIString());
		if (doesShortcut && receiverId != null) {
			// local shortcut
			new Thread(new Runnable() {
				@Override
				public void run() {
					JSONResponse response;
					try {
						String senderUrl = null;
						if (sender != null) {
							senderUrl = getSenderUrl(sender.getId(),
									receiverUrl.toASCIIString());
						}
						RequestParams requestParams = new RequestParams();
						requestParams.put(Sender.class, senderUrl);
						response = receive(receiverId, request, requestParams);
						callback.onSuccess(response);
					} catch (Exception e) {
						callback.onFailure(e);
					}
				}
			}).start();
		} else {
			TransportService service = null;
			String protocol = null;
			String senderUrl = null;
			if (sender != null) {
				senderUrl = getSenderUrl(sender.getId(),
						receiverUrl.toASCIIString());
			}
			protocol = receiverUrl.getScheme();
			service = getTransportService(protocol);
			if (service != null) {
				service.sendAsync(senderUrl, receiverUrl.toASCIIString(),
						request, callback);
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
	public String getSenderUrl(String agentId, String receiverUrl) {
		if (receiverUrl.startsWith("local:")) {
			return "local://" + agentId;
		}
		for (TransportService service : transportServices.values()) {
			List<String> protocols = service.getProtocols();
			for (String protocol : protocols) {
				if (receiverUrl.startsWith(protocol + ":")) {
					String senderUrl = service.getAgentUrl(agentId);
					if (senderUrl != null) {
						return senderUrl;
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
	public boolean isDoesShortcut() {
		return doesShortcut;
	}
	
	@Override
	public void setDoesShortcut(boolean doesShortcut) {
		this.doesShortcut = doesShortcut;
	}
	
	@Override
	public void setStateFactory(Config config) {
		if (this.stateFactory != null) {
			LOG.warning("Not loading statefactory from config, there is already a statefactory available.");
			return;
		}
		
		// get the class name from the config file
		// first read from the environment specific configuration,
		// if not found read from the global configuration
		String className = config.get("state", "class");
		String configName = "state";
		if (className == null) {
			className = config.get("context", "class");
			if (className == null) {
				throw new IllegalArgumentException(
						"Config parameter 'state.class' missing in Eve configuration.");
			} else {
				LOG.warning("Use of config parameter 'context' is deprecated, please use 'state' instead.");
				configName = "context";
			}
		}
		
		// TODO: deprecated since "2013-02-20"
		if ("FileContextFactory".equals(className)) {
			LOG.warning("Use of Classname FileContextFactory is deprecated, please use 'FileStateFactory' instead.");
			className = "FileStateFactory";
		}
		if ("MemoryContextFactory".equals(className)) {
			LOG.warning("Use of Classname MemoryContextFactory is deprecated, please use 'MemoryStateFactory' instead.");
			className = "MemoryStateFactory";
		}
		if ("DatastoreContextFactory".equals(className)) {
			LOG.warning("Use of Classname DatastoreContextFactory is deprecated, please use 'DatastoreStateFactory' instead.");
			className = "DatastoreStateFactory";
		}
		
		// Recognize known classes by their short name,
		// and replace the short name for the full class path
		for (String name : STATE_FACTORIES.keySet()) {
			if (className.equalsIgnoreCase(name)) {
				className = STATE_FACTORIES.get(name);
				break;
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
			StateFactory sf = (StateFactory) stateClass.getConstructor(
					Map.class).newInstance(params);
			
			setStateFactory(sf);
			LOG.info("Initialized state factory: " + sf.toString());
		} catch (Exception e) {
			LOG.log(Level.WARNING, "", e);
		}
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
		FACTORY.signalAgents(new AgentSignal<StateFactory>("setStateFactory",
				stateFactory));
		
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
		
		// TODO: remove warning some day (added 2013-01-22)
		if (className.equalsIgnoreCase("RunnableScheduler")) {
			LOG.warning("Deprecated class RunnableScheduler configured. Use RunnableSchedulerFactory instead to configure a scheduler factory.");
			className = "RunnableSchedulerFactory";
		}
		if (className.equalsIgnoreCase("AppEngineScheduler")) {
			LOG.warning("Deprecated class AppEngineScheduler configured. Use GaeSchedulerFactory instead to configure a scheduler factory.");
			className = "GaeSchedulerFactory";
		}
		if (className.equalsIgnoreCase("AppEngineSchedulerFactory")) {
			LOG.warning("Deprecated class AppEngineSchedulerFactory configured. Use GaeSchedulerFactory instead to configure a scheduler factory.");
			className = "GaeSchedulerFactory";
		}
		
		// Recognize known classes by their short name,
		// and replace the short name for the full class path
		for (String name : SCHEDULERS.keySet()) {
			if (className.equalsIgnoreCase(name)) {
				className = SCHEDULERS.get(name);
				break;
			}
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
						
						// TODO: remove deprecation warning some day (added
						// 2013-01-24)
						if (className.equalsIgnoreCase("XmppTransportService")) {
							LOG.warning("Deprecated class XmppTransportService, use XmppService instead.");
							className = "XmppService";
						}
						if (className.equalsIgnoreCase("HttpTransportService")) {
							LOG.warning("Deprecated class HttpTransportService, use HttpService instead.");
							className = "HttpService";
						}
						
						for (String name : TRANSPORT_SERVICES.keySet()) {
							if (className.equalsIgnoreCase(name)) {
								className = TRANSPORT_SERVICES.get(name);
								break;
							}
						}
						
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
					LOG.warning("Cannot load service at index " + index + ": "
							+ e.getMessage());
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
			if (FACTORY != null) {
				FACTORY.signalAgents(new AgentSignal<TransportService>(
						"addTransportService", transportService));
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
		FACTORY.signalAgents(new AgentSignal<TransportService>(
				"removeTransportService", transportService));
		
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
		if (schedulerFactory != null) {
			LOG.warning("Replacing earlier schedulerFactory.");
		}
		this.schedulerFactory = schedulerFactory;
		FACTORY.signalAgents(new AgentSignal<SchedulerFactory>(
				"setSchedulerFactory", schedulerFactory));
	}
	
	@Override
	public Scheduler getScheduler(Agent agent) {
		if (schedulerFactory == null) {
			LOG.warning("SchedulerFactory is null, while agent "
					+ agent.getId() + " calls for getScheduler");
			return null;
		}
		return schedulerFactory.getScheduler(agent);
	}
	
}
