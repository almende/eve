package com.almende.eve.agent;

import java.io.IOException;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.net.ProtocolException;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.joda.time.DateTime;

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
import com.almende.eve.state.MemoryStateFactory;
import com.almende.eve.state.State;
import com.almende.eve.state.StateFactory;
import com.almende.eve.transport.AsyncCallback;
import com.almende.eve.transport.TransportService;
import com.almende.eve.transport.http.HttpService;
import com.almende.util.ClassUtil;
import com.almende.util.TypeUtil;

/**
 * The AgentFactory is a factory to instantiate and invoke Eve Agents within the
 * configured state. The AgentFactory can invoke local as well as remote agents.
 * 
 * An AgentFactory must be instantiated with a valid Eve configuration file.
 * This configuration is needed to load the configured agent classes and
 * instantiate a state for each agent.
 * 
 * Example usage: // generic constructor Config config = new Config("eve.yaml");
 * AgentFactory factory = new AgentFactory(config);
 * 
 * // construct in servlet InputStream is =
 * getServletContext().getResourceAsStream("/WEB-INF/eve.yaml"); Config config =
 * new Config(is); AgentFactory factory = new AgentFactory(config);
 * 
 * // create or get a shared instance of the AgentFactory AgentFactory factory =
 * AgentFactory.createInstance(namespace, config); AgentFactory factory =
 * AgentFactory.getInstance(namespace);
 * 
 * // invoke a local agent by its id response = factory.invoke(agentId,
 * request);
 * 
 * // invoke a local or remote agent by its url response =
 * factory.send(senderId, receiverUrl, request);
 * 
 * // create a new agent Agent agent = factory.createAgent(agentType, agentId);
 * String desc = agent.getDescription(); // use the agent agent.destroy(); //
 * neatly shutdown the agents state
 * 
 * // instantiate an existing agent Agent agent = factory.getAgent(agentId);
 * String desc = agent.getDescription(); // use the agent agent.destroy(); //
 * neatly shutdown the agents state
 * 
 * @author jos
 */
public class AgentFactory {
	
	// Note: the CopyOnWriteArrayList is inefficient but thread safe.
	private List<TransportService>					transportServices	= new CopyOnWriteArrayList<TransportService>();
	private StateFactory							stateFactory		= null;
	private SchedulerFactory						schedulerFactory	= null;
	private Config									config				= null;
	private EventLogger								eventLogger			= new EventLogger(
																				this);
	
	private static String							ENVIRONMENT_PATH[]	= new String[] {
			"com.google.appengine.runtime.environment",
			"com.almende.eve.runtime.environment"						};
	private static String							environment			= null;
	private static boolean							doesShortcut		= true;
	
	private static final Map<String, AgentFactory>	FACTORIES			= new ConcurrentHashMap<String, AgentFactory>();
	private static final Map<String, String>		STATE_FACTORIES		= new HashMap<String, String>();
	private static final Map<String, String>		SCHEDULERS			= new HashMap<String, String>();
	private static final Map<String, String>		TRANSPORT_SERVICES	= new HashMap<String, String>();
	private static final RequestParams				EVEREQUESTPARAMS	= new RequestParams();
	private static final Logger						LOG					= Logger.getLogger(AgentFactory.class
																				.getSimpleName());
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
	
	private AgentFactory() {
	}
	
	/**
	 * Get a shared AgentFactory instance with the default namespace "default"
	 * 
	 * @return factory Returns the factory instance, or null when not existing
	 */
	public static AgentFactory getInstance() {
		return getInstance(null);
	}
	
	/**
	 * Get a shared AgentFactory instance with a specific namespace
	 * 
	 * @param namespace
	 *            If null, "default" namespace will be loaded.
	 * @return factory Returns the factory instance, or null when not existing
	 */
	public static AgentFactory getInstance(String namespace) {
		if (namespace == null) {
			namespace = "default";
		}
		
		return FACTORIES.get(namespace);
	}
	
	/**
	 * Create a shared AgentFactory instance with the default namespace
	 * "default"
	 * 
	 * @return factory
	 */
	public static synchronized AgentFactory createInstance() {
		return createInstance(null, null);
	}
	
	/**
	 * Create a shared AgentFactory instance with the default namespace
	 * "default"
	 * 
	 * @param config
	 * @return factory
	 */
	public static synchronized AgentFactory createInstance(Config config) {
		return createInstance(null, config);
	}
	
	/**
	 * Create a shared AgentFactory instance with a specific namespace
	 * 
	 * @param namespace
	 * @return factory
	 */
	public static synchronized AgentFactory createInstance(String namespace) {
		return createInstance(namespace, null);
	}
	
	public static synchronized void registerInstance(AgentFactory factory) {
		registerInstance(null, factory);
	}
	
	public static synchronized void registerInstance(String namespace,
			AgentFactory factory) {
		if (namespace == null) {
			namespace = "default";
		}
		FACTORIES.put(namespace, factory);
	}
	
	/**
	 * Create a shared AgentFactory instance with a specific namespace
	 * 
	 * @param namespace
	 *            If null, "default" namespace will be loaded.
	 * @param config
	 *            If null, a non-configured AgentFactory will be created.
	 * @return factory
	 */
	public static synchronized AgentFactory createInstance(String namespace,
			Config config) {
		if (namespace == null) {
			namespace = "default";
		}
		
		if (FACTORIES.containsKey(namespace)) {
			throw new IllegalStateException(
					"Shared AgentFactory with namespace '"
							+ namespace
							+ "' already exists. "
							+ "A shared AgentFactory can only be created once. "
							+ "Use getInstance instead to get the existing shared instance.");
		}
		
		AgentFactory factory = new AgentFactory();
		factory.setConfig(config);
		if (config != null) {
			AgentCache.configCache(config);
			
			// initialize all factories for state, transport, and scheduler
			// important to initialize in the correct order: cache first,
			// then the state and transport services, and lastly scheduler.
			factory.setStateFactory(config);
			factory.addTransportServices(config);
			// ensure there is always an HttpService for outgoing calls
			factory.addTransportService(new HttpService());
			factory.setSchedulerFactory(config);
			factory.addAgents(config);
		} else {
			// ensure there is at least a memory state service
			factory.setStateFactory(new MemoryStateFactory());
			// ensure there is always an HttpService for outgoing calls
			factory.addTransportService(new HttpService());
		}
		FACTORIES.put(namespace, factory);
		factory.boot();
		return factory;
	}
	
	/**
	 * Should be called every time a new AgentFactory is started or if new
	 * agents become available (through setStateFactory())
	 * 
	 */
	public void boot() {
		if (stateFactory != null) {
			Iterator<String> iter = stateFactory.getAllAgentIds();
			if (iter != null) {
				while (iter.hasNext()) {
					try {
						Agent agent = getAgent(iter.next());
						if (agent != null) {
							agent.boot();
						}
					} catch (Exception e) {
					}
				}
			}
		}
	}
	
	/**
	 * Get an agent by its id. Returns null if the agent does not exist
	 * 
	 * Before deleting the agent, the method agent.destroy() must be executed to
	 * neatly shutdown the instantiated state.
	 * 
	 * @param agentId
	 * @return agent
	 * @throws ClassNotFoundException
	 * @throws NoSuchMethodException
	 * @throws InvocationTargetException
	 * @throws IllegalAccessException
	 * @throws InstantiationException
	 * @throws SecurityException
	 */
	public Agent getAgent(String agentId) throws JSONRPCException,
			ClassNotFoundException, InstantiationException,
			IllegalAccessException, InvocationTargetException,
			NoSuchMethodException {
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
		agent.init();
		
		if (agentType.isAnnotationPresent(ThreadSafe.class)
				&& agentType.getAnnotation(ThreadSafe.class).value()) {
			AgentCache.put(agentId, agent);
		}
		
		return agent;
	}
	
	/**
	 * Create an agent proxy from an java interface
	 * 
	 * @deprecated
	 *             "Please use authenticated version: createAgentProxy(sender,receiverUrl,agentInterface);"
	 * @param receiverUrl
	 *            Url of the receiving agent
	 * @param agentInterface
	 *            A java Interface, extending AgentInterface
	 * @return
	 */
	@Deprecated
	public <T> T createAgentProxy(final URI receiverUrl, Class<T> agentInterface) {
		return createAgentProxy(null, receiverUrl, agentInterface);
	}
	
	/**
	 * Create an agent proxy from an java interface
	 * 
	 * @param sender
	 *            Sender Agent, used to authentication purposes.
	 * @param receiverUrl
	 *            Url of the receiving agent
	 * @param agentInterface
	 *            A java Interface, extending AgentInterface
	 * @return
	 */
	@SuppressWarnings("unchecked")
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
							Object[] args) throws Throwable {
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
	
	/**
	 * Create an asynchronous agent proxy from an java interface, each call will
	 * return a future for handling the results.
	 * 
	 * @deprecated
	 *             "Please use authenticated version: createAgentProxy(sender,receiverUrl,agentInterface);"
	 * @param receiverUrl
	 *            Url of the receiving agent
	 * @param agentInterface
	 *            A java Interface, extending AgentInterface
	 * @return
	 */
	@Deprecated
	public <T> AsyncProxy<T> createAsyncAgentProxy(final URI receiverUrl,
			Class<T> agentInterface) {
		return createAsyncAgentProxy(null, receiverUrl, agentInterface);
	}
	
	/**
	 * Create an asynchronous agent proxy from an java interface, each call will
	 * return a future for handling the results.
	 * 
	 * @param senderId
	 *            Internal id of the sender agent. Not required for all
	 *            transport services (for example not for outgoing HTTP
	 *            requests)
	 * @param receiverUrl
	 *            Url of the receiving agent
	 * @param agentInterface
	 *            A java Interface, extending AgentInterface
	 * @return
	 */
	public <T> AsyncProxy<T> createAsyncAgentProxy(final AgentInterface sender,
			final URI receiverUrl, Class<T> agentInterface) {
		return new AsyncProxy<T>(createAgentProxy(sender, receiverUrl,
				agentInterface));
	}
	
	/**
	 * Create an agent.
	 * 
	 * Before deleting the agent, the method agent.destroy() must be executed to
	 * neatly shutdown the instantiated state.
	 * 
	 * @param agentType
	 *            full class path
	 * @param agentId
	 * @return
	 * @throws ClassNotFoundException
	 * @throws NoSuchMethodException
	 * @throws InvocationTargetException
	 * @throws IllegalAccessException
	 * @throws InstantiationException
	 * @throws JSONRPCException
	 * @throws IOException
	 */
	@SuppressWarnings("unchecked")
	public <T extends Agent> T createAgent(String agentType, String agentId)
			throws JSONRPCException, InstantiationException,
			IllegalAccessException, InvocationTargetException,
			NoSuchMethodException, ClassNotFoundException, IOException {
		return (T) createAgent((Class<T>) Class.forName(agentType), agentId);
	}
	
	/**
	 * Create an agent.
	 * 
	 * Before deleting the agent, the method agent.destroy() must be executed to
	 * neatly shutdown the instantiated state.
	 * 
	 * @param agentType
	 * @param agentId
	 * @return
	 * @throws JSONRPCException
	 * @throws NoSuchMethodException
	 * @throws InvocationTargetException
	 * @throws IllegalAccessException
	 * @throws InstantiationException
	 * @throws IOException
	 */
	@SuppressWarnings("unchecked")
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
		agent.create();
		agent.init();
		
		if (agentType.isAnnotationPresent(ThreadSafe.class)
				&& agentType.getAnnotation(ThreadSafe.class).value()) {
			AgentCache.put(agentId, agent);
		}
		
		return agent;
	}
	
	/**
	 * Create a new agent, using the base AspectAgent class. This agent has a
	 * namespace "sub", to which the given class's methods are added.
	 * 
	 * @param aspect
	 * @param agentId
	 * @return
	 * @throws NoSuchMethodException
	 * @throws InvocationTargetException
	 * @throws IllegalAccessException
	 * @throws InstantiationException
	 * @throws JSONRPCException
	 * @throws IOException
	 */
	public <T> AspectAgent<T> createAspectAgent(Class<? extends T> aspect,
			String agentId) throws JSONRPCException, InstantiationException,
			IllegalAccessException, InvocationTargetException,
			NoSuchMethodException, IOException {
		@SuppressWarnings("unchecked")
		AspectAgent<T> result = createAgent(AspectAgent.class, agentId);
		result.init(aspect);
		return result;
	}
	
	/**
	 * Delete an agent
	 * 
	 * @param agentId
	 * @throws NoSuchMethodException
	 * @throws InvocationTargetException
	 * @throws IllegalAccessException
	 * @throws InstantiationException
	 * @throws ClassNotFoundException
	 * @throws JSONRPCException
	 */
	public void deleteAgent(String agentId) throws JSONRPCException,
			ClassNotFoundException, InstantiationException,
			IllegalAccessException, InvocationTargetException,
			NoSuchMethodException {
		if (agentId == null) {
			return;
		}
		Agent agent = getAgent(agentId);
		if (agent == null) {
			return;
		}
		
		if (getScheduler(agent) != null) {
			schedulerFactory.destroyScheduler(agentId);
		}
		try {
			// get the agent and execute the delete method
			agent.destroy();
			agent.delete();
			AgentCache.delete(agentId);
			agent = null;
		} catch (Exception e) {
			LOG.log(Level.WARNING, "Error deleting agent:" + agentId, e);
		}
		
		// delete the state, even if the agent.destroy or agent.delete
		// failed.
		getStateFactory().delete(agentId);
	}
	
	/**
	 * Test if an agent exists
	 * 
	 * @param agentId
	 * @return true if the agent exists
	 * @throws JSONRPCException
	 */
	public boolean hasAgent(String agentId) throws JSONRPCException {
		return getStateFactory().exists(agentId);
	}
	
	/**
	 * Get the event logger. The event logger is used to temporary log triggered
	 * events, and display them on the agents web interface.
	 * 
	 * @return eventLogger
	 */
	public EventLogger getEventLogger() {
		return eventLogger;
	}
	
	/**
	 * Invoke a local agent
	 * 
	 * @param receiverId
	 *            Id of the receiver agent
	 * @param request
	 * @param requestParams
	 * @return
	 * @throws JSONRPCException
	 */
	public JSONResponse receive(String receiverId, JSONRequest request,
			RequestParams requestParams) throws JSONRPCException {
		try {
			Agent receiver = getAgent(receiverId);
			if (receiver != null) {
				JSONResponse response = JSONRPC.invoke(receiver, request,
						requestParams, receiver);
				receiver.destroy();
				return response;
			}
		} catch (Exception e) {
			throw new JSONRPCException("Couldn't instantiate agent for id '"
					+ receiverId + "'", e);
		}
		throw new JSONRPCException("Agent with id '" + receiverId
				+ "' not found");
	}
	
	/**
	 * Invoke a local or remote agent. In case of an local agent, the agent is
	 * invoked immediately. In case of an remote agent, an HTTP Request is sent
	 * to the concerning agent.
	 * 
	 * @deprecated
	 *             "Please use authenticated version: send(sender,receiverUrl,request);"
	 * 
	 * @param receiverUrl
	 * @param request
	 * @return
	 * @throws JSONRPCException
	 * @throws ProtocolException
	 */
	@Deprecated
	public JSONResponse send(URI receiverUrl, JSONRequest request)
			throws ProtocolException, JSONRPCException {
		return send(null, receiverUrl, request);
	}
	
	/**
	 * Invoke a local or remote agent. In case of an local agent, the agent is
	 * invoked immediately. In case of an remote agent, an HTTP Request is sent
	 * to the concerning agent.
	 * 
	 * @param sender
	 *            Sending agent. Not required for all
	 *            transport services (for example not for outgoing HTTP
	 *            requests), in which cases a "null" value may be passed.
	 * @param receiverUrl
	 * @param request
	 * @return
	 * @throws JSONRPCException
	 * @throws ProtocolException
	 */
	public JSONResponse send(AgentInterface sender, URI receiverUrl,
			JSONRequest request) throws ProtocolException, JSONRPCException {
		String receiverId = getAgentId(receiverUrl.toASCIIString());
		String senderUrl = null;
		if (sender != null) {
			senderUrl = getSenderUrl(sender.getId(),
					receiverUrl.toASCIIString());
		}
		if (doesShortcut && receiverId != null) {
			// local shortcut
			RequestParams requestParams = new RequestParams();
			requestParams.put(Sender.class, senderUrl);
			return receive(receiverId, request, requestParams);
		} else {
			TransportService service = null;
			String protocol = receiverUrl.getScheme();
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
	
	/**
	 * Asynchronously invoke a request on an agent.
	 * 
	 * @deprecated
	 *             "Please use authenticated version: sendAsync(sender,receiverUrl,request,callback);"
	 * 
	 * @param receiverUrl
	 * @param request
	 * @param callback
	 * @throws JSONRPCException
	 * @throws ProtocolException
	 */
	@Deprecated
	public void sendAsync(final URI receiverUrl, final JSONRequest request,
			final AsyncCallback<JSONResponse> callback)
			throws ProtocolException, JSONRPCException {
		sendAsync(null, receiverUrl, request, callback);
	}
	
	/**
	 * Asynchronously invoke a request on an agent.
	 * 
	 * @param sender
	 *            Internal id of the sender agent. Not required for all
	 *            transport services (for example not for outgoing HTTP
	 *            requests)
	 * @param receiverUrl
	 * @param request
	 * @param callback
	 * @throws JSONRPCException
	 * @throws ProtocolException
	 */
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
	
	/**
	 * Get the agentId from given agentUrl. The url can be any protocol. If the
	 * url matches any of the registered transport services, an agentId is
	 * returned. This means that the url represents a local agent. It is
	 * possible that no agent with this id exists.
	 * 
	 * @param agentUrl
	 * @return agentId
	 */
	private String getAgentId(String agentUrl) {
		if (agentUrl.startsWith("local:")) {
			return agentUrl.replaceFirst("local:/?/?", "");
		}
		for (TransportService service : transportServices) {
			String agentId = service.getAgentId(agentUrl);
			if (agentId != null) {
				return agentId;
			}
		}
		return null;
	}
	
	/**
	 * Determines best senderUrl for this agent, match receiverUrl transport
	 * method if possible. (fallback from HTTPS to HTTP included)
	 * 
	 * @param agentUrl
	 * @return agentId
	 */
	private String getSenderUrl(String agentId, String receiverUrl) {
		if (receiverUrl.startsWith("local:")) {
			return "local://" + agentId;
		}
		for (TransportService service : transportServices) {
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
	
	/**
	 * Retrieve the current environment, using the configured State. Can return
	 * values like "Production", "Development". If no environment variable is
	 * found, "Production" is returned.
	 * 
	 * @return environment
	 */
	public static String getEnvironment() {
		if (environment == null) {
			for (String path : ENVIRONMENT_PATH) {
				environment = System.getProperty(path);
				if (environment != null) {
					LOG.info("Current environment: '" + environment
							+ "' (read from path '" + path + "')");
					break;
				}
			}
			
			if (environment == null) {
				// no environment variable found. Fall back to "Production"
				environment = "Production";
				
				String msg = "No environment variable found. "
						+ "Environment set to '" + environment
						+ "'. Checked paths: ";
				for (String path : ENVIRONMENT_PATH) {
					msg += path + ", ";
				}
				LOG.warning(msg);
			}
		}
		
		return environment;
	}
	
	/**
	 * Programmatically set the environment
	 * 
	 * @param env
	 *            The environment, for example "Production" or "Development"
	 * @return
	 */
	public static final void setEnvironment(String env) {
		environment = env;
	}
	
	/**
	 * Get the loaded config file
	 * 
	 * @return config A configuration file
	 */
	public final void setConfig(Config config) {
		this.config = config;
	}
	
	/**
	 * Get the loaded config file
	 * 
	 * @return config A configuration file
	 */
	public Config getConfig() {
		return config;
	}
	
	public static boolean isDoesShortcut() {
		return doesShortcut;
	}
	
	public static void setDoesShortcut(boolean doesShortcut) {
		AgentFactory.doesShortcut = doesShortcut;
	}
	
	/**
	 * Load a state factory from config
	 * 
	 * @param config
	 */
	public final void setStateFactory(Config config) {
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
			StateFactory stateFactory = (StateFactory) stateClass
					.getConstructor(Map.class).newInstance(params);
			
			setStateFactory(stateFactory);
			LOG.info("Initialized state factory: " + stateFactory.toString());
		} catch (Exception e) {
			LOG.log(Level.WARNING, "", e);
		}
	}
	
	/**
	 * Create agents from a config (only when they do not yet exist). Agents
	 * will be read from the configuration path bootstrap.agents, which must
	 * contain a map where the keys are agentId's and the values are the agent
	 * types (full java class path).
	 * 
	 * @param config
	 */
	public final void addAgents(Config config) {
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
						agent.destroy();
						LOG.info("Bootstrap created agent id=" + agentId
								+ ", type=" + agentType);
					}
				} catch (Exception e) {
					LOG.log(Level.WARNING, "", e);
				}
			}
		}
	}
	
	/**
	 * Set a state factory. The state factory is used to get/create/delete an
	 * agents state.
	 * 
	 * @param stateFactory
	 */
	public final void setStateFactory(StateFactory stateFactory) {
		this.stateFactory = stateFactory;
	}
	
	/**
	 * Get the configured state factory.
	 * 
	 * @return stateFactory
	 */
	public StateFactory getStateFactory() throws JSONRPCException {
		if (stateFactory == null) {
			throw new JSONRPCException("No state factory initialized.");
		}
		return stateFactory;
	}
	
	/**
	 * Load a scheduler factory from a config file
	 * 
	 * @param config
	 */
	public final void setSchedulerFactory(Config config) {
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
			SchedulerFactory schedulerFactory = (SchedulerFactory) schedulerClass
					.getConstructor(AgentFactory.class, Map.class).newInstance(
							this, params);
			
			setSchedulerFactory(schedulerFactory);
			
			LOG.info("Initialized scheduler factory: "
					+ schedulerFactory.getClass().getName());
		} catch (Exception e) {
			LOG.log(Level.WARNING, "", e);
		}
	}
	
	/**
	 * Load transport services for incoming and outgoing messages from a config
	 * (for example http and xmpp services).
	 * 
	 * @param config
	 */
	public final void addTransportServices(Config config) {
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
								.getConstructor(AgentFactory.class, Map.class)
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
	
	/**
	 * Add a new transport service
	 * 
	 * @param transportService
	 */
	public final void addTransportService(TransportService transportService) {
		transportServices.add(transportService);
		LOG.info("Registered transport service: " + transportService.toString());
	}
	
	/**
	 * Remove a registered a transport service
	 * 
	 * @param transportService
	 */
	public void removeTransportService(TransportService transportService) {
		transportServices.remove(transportService);
		LOG.info("Unregistered transport service "
				+ transportService.toString());
	}
	
	/**
	 * Get all registered transport services
	 * 
	 * @return transportService
	 */
	public List<TransportService> getTransportServices() {
		return transportServices;
	}
	
	/**
	 * Get all registered transport services which can handle given protocol
	 * 
	 * @param protocol
	 *            A protocol, for example "http" or "xmpp"
	 * @return transportService
	 */
	public List<TransportService> getTransportServices(String protocol) {
		List<TransportService> filteredServices = new ArrayList<TransportService>();
		
		for (TransportService service : transportServices) {
			List<String> protocols = service.getProtocols();
			if (protocols.contains(protocol)) {
				filteredServices.add(service);
			}
		}
		
		return filteredServices;
	}
	
	/**
	 * Get the first registered transport service which supports given protocol.
	 * Returns null when none of the registered transport services can handle
	 * the protocol.
	 * 
	 * @param protocol
	 *            A protocol, for example "http" or "xmpp"
	 * @return service
	 */
	public TransportService getTransportService(String protocol) {
		List<TransportService> services = getTransportServices(protocol);
		if (services.size() > 0) {
			return services.get(0);
		}
		return null;
	}
	
	public List<Object> getMethods(Agent agent) {
		Boolean asString = false;
		return JSONRPC.describe(agent, EVEREQUESTPARAMS, asString);
	}
	
	/**
	 * Set a scheduler factory. The scheduler factory is used to
	 * get/create/delete an agents scheduler.
	 * 
	 * @param schedulerFactory
	 */
	public synchronized void setSchedulerFactory(
			SchedulerFactory schedulerFactory) {
		this.schedulerFactory = schedulerFactory;
		this.notifyAll();
	}
	
	/**
	 * create a scheduler for an agent
	 * 
	 * @param agentId
	 * @return scheduler
	 */
	public synchronized Scheduler getScheduler(Agent agent) {
		DateTime start = DateTime.now();
		while (schedulerFactory == null && start.plus(30000).isBeforeNow()) {
			try {
				this.wait();
			} catch (InterruptedException e) {
				LOG.log(Level.WARNING, "", e);
			}
		}
		if (schedulerFactory == null) {
			LOG.severe("SchedulerFactory is null, while agent " + agent.getId()
					+ " calls for getScheduler");
			return null;
		}
		return schedulerFactory.getScheduler(agent);
	}
	
}
