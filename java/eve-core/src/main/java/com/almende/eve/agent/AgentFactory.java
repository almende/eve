package com.almende.eve.agent;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.net.ProtocolException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.logging.Logger;

import com.almende.eve.agent.annotation.ThreadSafe;
import com.almende.eve.agent.log.EventLogger;
import com.almende.eve.config.Config;
import com.almende.eve.context.Context;
import com.almende.eve.context.ContextFactory;
import com.almende.eve.rpc.jsonrpc.JSONRPC;
import com.almende.eve.rpc.jsonrpc.JSONRPCException;
import com.almende.eve.rpc.jsonrpc.JSONRequest;
import com.almende.eve.rpc.jsonrpc.JSONResponse;
import com.almende.eve.scheduler.Scheduler;
import com.almende.eve.scheduler.SchedulerFactory;
import com.almende.eve.transport.AsyncCallback;
import com.almende.eve.transport.TransportService;
import com.almende.eve.transport.http.HttpTransportService;
import com.almende.util.ClassUtil;

/**
 * The AgentFactory is a factory to instantiate and invoke Eve Agents within the 
 * configured context. The AgentFactory can invoke local as well as remote 
 * agents.
 * 
 * An AgentFactory must be instantiated with a valid Eve configuration file.
 * This configuration is needed to load the configured agent classes and 
 * instantiate a context for each agent.
 * 
 * Example usage:
 *     // generic constructor
 *     Config config = new Config("eve.yaml");
 *     AgentFactory factory = new AgentFactory(config);
 *     
 *     // construct in servlet
 *     InputStream is = getServletContext().getResourceAsStream("/WEB-INF/eve.yaml");
 *     Config config = new Config(is);
 *     AgentFactory factory = new AgentFactory(config);
 *     
 *     // create or get a shared instance of the AgentFactory
 *     AgentFactory factory = AgentFactory.createInstance(namespace, config);
 *     AgentFactory factory = AgentFactory.getInstance(namespace);
 *     
 *     // invoke a local agent by its id
 *     response = factory.invoke(agentId, request); 
 *
 *     // invoke a local or remote agent by its url
 *     response = factory.send(senderId, receiverUrl, request);
 *     
 *     // create a new agent
 *     Agent agent = factory.createAgent(agentClass, agentId);
 *     String desc = agent.getDescription(); // use the agent
 *     agent.destroy(); // neatly shutdown the agents context
 *     
 *     // instantiate an existing agent
 *     Agent agent = factory.getAgent(agentId);
 *     String desc = agent.getDescription(); // use the agent
 *     agent.destroy(); // neatly shutdown the agents context
 * 
 * @author jos
 */
public class AgentFactory {
	public AgentFactory () {
		addTransportService(new HttpTransportService(this));
		agents = new AgentCache();
	}
	
	/**
	 * Construct an AgentFactory and initialize the configuration
	 * @param config
	 * @throws Exception
	 */
	public AgentFactory(Config config) throws Exception {
		this.config = config;

		// important to initialize in the correct order: cache first, 
		// then the context and transport services, and lastly scheduler.
		agents = new AgentCache(config);
		initContext(config);
		initTransportServices(config);
		initScheduler(config);

		addTransportService(new HttpTransportService(this));
	}
	
	/**
	 * Get a shared AgentFactory instance with the default namespace "default"
	 * @return factory     Returns the factory instance, or null when not 
	 *                     existing 
	 */
	public static AgentFactory getInstance() {
		return getInstance(null);
	}

	/**
	 * Get a shared AgentFactory instance with a specific namespace
	 * @param namespace    If null, "default" namespace will be loaded.
	 * @return factory     Returns the factory instance, or null when not 
	 *                     existing 
	 */
	public static AgentFactory getInstance(String namespace) {
		if (namespace == null) {
			namespace = "default";
		}
		return factories.get(namespace);
	}
	
	/**
	 * Create a shared AgentFactory instance with the default namespace "default"
	 * @param config
	 * @return factory
	 */
	public static synchronized AgentFactory createInstance(Config config) 
			throws Exception{
		return createInstance(null, config);
	}

	/**
	 * Create a shared AgentFactory instance with a specific namespace
	 * @param namespace    If null, "default" namespace will be loaded.
	 * @param config
	 * @return factory
	 * @throws Exception 
	 */
	public static synchronized AgentFactory createInstance(String namespace, 
			Config config) throws Exception {
		if (namespace == null) {
			namespace = "default";
		}
		
		if (factories.containsKey(namespace)) {
			throw new Exception("Shared AgentFactory with namespace '" + 
					namespace + "' already exists. " +
					"A shared AgentFactory can only be created once. " +
					"Use getInstance instead to get the existing shared instance.");
		}
		
		AgentFactory factory = new AgentFactory(config);
		factories.put(namespace, factory);
		
		return factory;
	}

	/**
	 * Get an agent by its id. Returns null if the agent does not exist
	 * 
	 * Before deleting the agent, the method agent.destroy() must be executed
	 * to neatly shutdown the instantiated context.
	 * 
	 * @param agentId
	 * @return agent
	 * @throws Exception
	 */
	public Agent getAgent(String agentId) throws Exception {
		
		//Check if agent is instantiated already, returning if it is:
		Agent agent = agents.get(agentId);
		if (agent != null){
			//System.err.println("Agent "+agentId+" found in cache!");
			return agent;
		}
		//No agent found, normal initialization:
		
		// load the context
		Context context = null; 
		context = getContextFactory().get(agentId);
		if (context == null) {
			// agent does not exist
			return null;
		}
		context.init();
		
		// read the agents class name from context
		Class<?> agentClass = context.getAgentClass();
		if (agentClass == null) {
			throw new Exception("Cannot instantiate agent. " +
					"Class information missing in the agents context " +
					"(agentId='" + agentId + "')");
		}
		
		// instantiate the agent
		agent = (Agent) agentClass.getConstructor().newInstance();
		agent.setAgentFactory(this);
		agent.setContext(context);
		agent.init();
		
		if (agentClass.isAnnotationPresent(ThreadSafe.class) && 
				agentClass.getAnnotation(ThreadSafe.class).value()){
			//System.err.println("Agent "+agentId+" is threadSafe, keeping!");
			agents.put(agentId, agent);
		}
		
		return agent;
	}

	/**
	 * Create an agent proxy from an java interface
	 * @param senderId        Internal id of the sender agent.
	 *                        Not required for all transport services 
	 *                        (for example not for outgoing HTTP requests)
	 * @param receiverUrl     Url of the receiving agent
	 * @param agentInterface  A java Interface, extending AgentInterface
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public <T> T createAgentProxy(final String senderId, final String receiverUrl,
			Class<T> agentInterface) {
		if (!ClassUtil.hasInterface(agentInterface, AgentInterface.class)) {
			throw new IllegalArgumentException("agentInterface must extend " + 
					AgentInterface.class.getName());
		}
		
		// http://docs.oracle.com/javase/1.4.2/docs/guide/reflection/proxy.html
		T proxy = (T) Proxy.newProxyInstance(agentInterface.getClassLoader(),
				new Class[] { agentInterface },
				new InvocationHandler() {
					public Object invoke(Object proxy, Method method,
							Object[] args) throws Throwable {
						String id = getAgentId(receiverUrl);
						if (id != null) {
							// local agent
							Agent agent = getAgent(id);
							return method.invoke(agent, args);
						}
						else {
							// remote agent
							JSONRequest request = JSONRPC.createRequest(method, args);
							JSONResponse response = send(senderId, receiverUrl, request);
							JSONRPCException err = response.getError();
							if (err != null) {
								throw err;
							}
							else if (response.getResult() != null) {
								return response.getResult(Object.class);
							}
							else {
								return null;
							}
						}
					}
				});
		
		// TODO: for optimization, one can cache the created proxy's

		return proxy;
	}

	/**
	 * Create an agent.
	 * 
	 * Before deleting the agent, the method agent.destroy() must be executed
	 * to neatly shutdown the instantiated context.
	 * 
	 * @param agentClass  full class path
	 * @param agentId
	 * @return
	 * @throws Exception
	 */
	public Agent createAgent(String agentClass, String agentId) throws Exception {
		return (Agent) createAgent(Class.forName(agentClass), agentId);
	}
	
	/**
	 * Create an agent.
	 * 
	 * Before deleting the agent, the method agent.destroy() must be executed
	 * to neatly shutdown the instantiated context.
	 * 
	 * @param agentClass
	 * @param agentId
	 * @return
	 * @throws Exception
	 */
	public Agent createAgent(Class<?> agentClass, String agentId) throws Exception {
		if (!ClassUtil.hasSuperClass(agentClass, Agent.class)) {
			throw new Exception(
					"Class " + agentClass + " does not extend class " + Agent.class);
		}
		
		// create the context
		Context context = getContextFactory().create(agentId);
		context.setAgentClass(agentClass);
		context.destroy();

		// instantiate the agent
		Agent agent = (Agent) agentClass.getConstructor().newInstance();
		agent.setAgentFactory(this);
		agent.setContext(context);
		agent.create();
		agent.init();

		if (agentClass.isAnnotationPresent(ThreadSafe.class) && 
				agentClass.getAnnotation(ThreadSafe.class).value()){
			//System.err.println("Agent "+agentId+" is threadSafe, keeping!");
			agents.put(agentId, agent);
		}
		
		return agent;
	}
	
	/**
	 * Delete an agent
	 * @param agentId
	 * @throws Exception 
	 */
	public void deleteAgent(String agentId) throws Exception {
		// get the agent and execute the delete method
		Agent agent = getAgent(agentId);
		agent.destroy();
		agent.delete();
		agent = null;
		
		getContextFactory().delete(agentId);
	}
	
	/**
	 * Test if an agent exists
	 * @param agentId
	 * @return true if the agent exists
	 * @throws Exception 
	 */
	public boolean hasAgent(String agentId) throws Exception {
		return getContextFactory().exists(agentId);
	}

	/**
	 * Get the event logger. The event logger is used to temporary log 
	 * triggered events, and display them on the agents web interface.
	 * @return eventLogger
	 */
	public EventLogger getEventLogger() {
		return eventLogger;
	}
	
	/**
	 * Invoke a local agent
	 * @param agentId
	 * @param request
	 * @return
	 * @throws Exception
	 */
	// TOOD: cleanup this method?
	public JSONResponse invoke(String agentId, JSONRequest request) throws Exception {
		Agent agent = getAgent(agentId);
		if (agent != null) {
			JSONResponse response = JSONRPC.invoke(agent, request);
			agent.destroy();
			return response;
		}
		else {
			throw new Exception("Agent with id '" + agentId + "' not found");
		}
	}

	/**
	 * Invoke a local or remote agent. 
	 * In case of an local agent, the agent is invoked immediately.
	 * In case of an remote agent, an HTTP Request is sent to the concerning
	 * agent.
	 * @param senderId    Internal id of the sender agent
	 *                    Not required for all transport services 
	 *                    (for example not for outgoing HTTP requests)
	 * @param receiverUrl
	 * @param request
	 * @return
	 * @throws Exception
	 */
	public JSONResponse send(String senderId, String receiverUrl, JSONRequest request) 
			throws Exception {
		String agentId = getAgentId(receiverUrl);
		if (agentId != null) {
			// local agent, invoke locally
			return invoke(agentId, request);
		}
		else {
			TransportService service = null;
			String protocol = null;
			int separator = receiverUrl.indexOf(":");
			if (separator != -1) {
				protocol = receiverUrl.substring(0, separator);
				service = getTransportService(protocol);
			}
			if (service != null) {
				return service.send(senderId, receiverUrl, request);
			}
			else {
				throw new ProtocolException(
					"No transport service configured for protocol '" + protocol + "'.");
			}			
		}
	}
	
	/**
	 * Asynchronously invoke a request on an agent.
	 * @param senderId    Internal id of the sender agent. 
	 *                    Not required for all transport services 
	 *                    (for example not for outgoing HTTP requests)
	 * @param receiverUrl
	 * @param request
	 * @param callback
	 * @throws Exception 
	 */
	public void sendAsync(final String senderId, final String receiverUrl, 
			final JSONRequest request, 
			final AsyncCallback<JSONResponse> callback) throws Exception {
		final String agentId = getAgentId(receiverUrl);
		if (agentId != null) {
			new Thread(new Runnable () {
				@Override
				public void run() {
					JSONResponse response;
					try {
						response = invoke(agentId, request);
						callback.onSuccess(response);
					} catch (Exception e) {
						callback.onFailure(e);
					}
				}
			}).start();
		}
		else {
			TransportService service = null;
			String protocol = null;
			int separator = receiverUrl.indexOf(":");
			if (separator != -1) {
				protocol = receiverUrl.substring(0, separator);
				service = getTransportService(protocol);
			}
			if (service != null) {
				service.sendAsync(senderId, receiverUrl, request, callback);
			}
			else {
				throw new ProtocolException(
					"No transport service configured for protocol '" + protocol + "'.");
			}
		}
	}

	/**
	 * Get the agentId from given agentUrl. The url can be any protocol.
	 * If the url matches any of the registered transport services, 
	 * an agentId is returned.
	 * This means that the url represents a local agent. It is possible
	 * that no agent with this id exists.
	 * @param agentUrl
	 * @return agentId
	 */
	private String getAgentId(String agentUrl) {
		for (TransportService service : transportServices) {
			String agentId = service.getAgentId(agentUrl);
			if (agentId != null) {
				return agentId;
			}
		}		
		return null;
	}
	
	/**
	 * Retrieve the current environment, using the configured Context.
	 * Available values: "Production", "Development"
	 * @return environment
	 */
	public String getEnvironment() {
		return (contextFactory != null) ? contextFactory.getEnvironment() : null;
	}

	/**
	 * Get the loaded config file
	 * @return config   A configuration file
	 */
	public Config getConfig() {
		return config;
	}
	
	/**
	 * Initialize the context factory. The class is read from the provided 
	 * configuration file.
	 * @param config
	 * @throws Exception
	 */
	private void initContext(Config config) {
		// get the class name from the config file
		// first read from the environment specific configuration,
		// if not found read from the global configuration
		String className = config.get("context", "class");
		if (className == null) {
			throw new IllegalArgumentException(
				"Config parameter 'context.class' missing in Eve configuration.");
		}
		
		// Recognize known classes by their short name,
		// and replace the short name for the full class path
		for (String name : CONTEXT_FACTORIES.keySet()) {
			if (className.toLowerCase().equals(name.toLowerCase())) {
				className = CONTEXT_FACTORIES.get(name);
				break;
			}
		}
		
		try {
			// get the class
			Class<?> contextClass = Class.forName(className);
			if (!ClassUtil.hasSuperClass(contextClass, ContextFactory.class)) {
				throw new IllegalArgumentException(
						"Context factory class " + contextClass.getName() + 
						" must extend " + Context.class.getName());
			}
	
			// instantiate the context factory
			Map<String, Object> params = config.get("context");
			ContextFactory contextFactory = (ContextFactory) contextClass
					.getConstructor(AgentFactory.class, Map.class )
					.newInstance(this, params);

			setContextFactory(contextFactory);
			logger.info("Initialized context factory: " + contextFactory.toString());
		}
		catch (Exception e) {
			e.printStackTrace();
		}		
	}
	
	/**
	 * Set a context factory. The context factory is used to get/create/delete
	 * an agents context.
	 * @param contextFactory
	 */
	public void setContextFactory(ContextFactory contextFactory) {
		this.contextFactory = contextFactory;
	}

	/**
	 * Get the configured context factory.
	 * @return contextFactory
	 */
	public ContextFactory getContextFactory() throws Exception {
		if (contextFactory == null) {
			throw new Exception("No context factory initialized.");
		}
		return contextFactory;
	}

	/**
	 * Initialize the scheduler. The class is read from the provided 
	 * configuration file.
	 * @param config
	 * @throws Exception
	 */
	private void initScheduler(Config config) {
		// get the class name from the config file
		// first read from the environment specific configuration,
		// if not found read from the global configuration
		String className = config.get("environment", getEnvironment(), "scheduler", "class");
		if (className == null) {
			className = config.get("scheduler", "class");
		}
		if (className == null) {
			throw new IllegalArgumentException(
				"Config parameter 'scheduler.class' missing in Eve configuration.");
		}

		// TODO: remove warning some day (added 2013-01-22)
		if (className.toLowerCase().equals("RunnableScheduler".toLowerCase())) {
			logger.warning("Deprecated class RunnableScheduler configured. Use RunnableSchedulerFactory instead to configure a scheduler factory.");
			className = "RunnableSchedulerFactory";
		}
		if (className.toLowerCase().equals("AppEngineScheduler".toLowerCase())) {
			logger.warning("Deprecated class AppEngineScheduler configured. Use AppEngineSchedulerFactory instead to configure a scheduler factory.");
			className = "AppEngineSchedulerFactory";
		}
		
		// Recognize known classes by their short name,
		// and replace the short name for the full class path
		for (String name : SCHEDULERS.keySet()) {
			if (className.toLowerCase().equals(name.toLowerCase())) {
				className = SCHEDULERS.get(name);
				break;
			}
		}

		// read all scheduler params (will be fed to the scheduler factory
		// on construction)
		Map<String, Object> params = config.get("environment", getEnvironment(), "scheduler");
		if (params == null) {
			params = config.get("scheduler");
		}
		
		try {
			// get the class
			Class<?> schedulerClass = Class.forName(className);
			if (!ClassUtil.hasInterface(schedulerClass, SchedulerFactory.class)) {
				throw new IllegalArgumentException(
						"Scheduler class " + schedulerClass.getName() + 
						" must implement " + SchedulerFactory.class.getName());
			}
			
			// initialize the scheduler factory
			schedulerFactory = (SchedulerFactory) schedulerClass
						.getConstructor(AgentFactory.class, Map.class )
						.newInstance(this, params);
			
			logger.info("Initialized scheduler factory: " + 
					schedulerFactory.getClass().getName());
		}
		catch (Exception e) {
			e.printStackTrace();
		}		
	}

	/**
	 * Initialize transport services for incoming and outgoing messages
	 * (for example http and xmpp services).
	 * @param config
	 */
	private void initTransportServices(Config config) {
		if (config == null) {
			Exception e = new Exception("Configuration uninitialized");
			e.printStackTrace();
			return;
		}
		
		// create a list to hold both global and environment specific transport
		List<Map<String, Object>> allTransportParams = 
				new ArrayList<Map<String, Object>>();
		
		// read global service params
		List<Map<String, Object>> globalTransportParams = 
				config.get("transport_services");
		if (globalTransportParams == null) {
			// TODO: cleanup some day. deprecated since 2013-01-17
			globalTransportParams = config.get("services");
			if (globalTransportParams != null) {
				logger.warning("Property 'services' is deprecated. Use 'transport_services' instead.");
			}
		}
		if (globalTransportParams != null) {
			allTransportParams.addAll(globalTransportParams);
		}

		// read service params for the current environment
		List<Map<String, Object>> environmentTransportParams = 
				config.get("environment", getEnvironment(), "transport_services");
		if (environmentTransportParams == null) {
			// TODO: cleanup some day. deprecated since 2013-01-17
			environmentTransportParams = config.get("environment", getEnvironment(), "services");
			if (environmentTransportParams != null) {
				logger.warning("Property 'services' is deprecated. Use 'transport_services' instead.");
			}
		}
		if (environmentTransportParams != null) {
			allTransportParams.addAll(environmentTransportParams);
		}
		
		int index = 0;
		for (Map<String, Object> transportParams : allTransportParams) {
			String className = (String) transportParams.get("class");
			try {
				if (className != null) {
					// Recognize known classes by their short name,
					// and replace the short name for the full class path
					
					// TODO: remove warning some day (added 2013-01-17)
					if (className.toLowerCase().equals("XmppService".toLowerCase())) {
						logger.warning("Deprecated class XmppService, use XmppTransportService instead.");
						className = "XmppTransportService";
					}
					if (className.toLowerCase().equals("HttpService".toLowerCase())) {
						logger.warning("Deprecated class HttpService, use HttpTransportService instead.");
						className = "HttpTransportService";
					}

					for (String name : TRANSPORT_SERVICES.keySet()) {
						if (className.toLowerCase().equals(name.toLowerCase())) {
							className = TRANSPORT_SERVICES.get(name);
							break;
						}
					}
					
					// initialize the transport service
					Class<?> transportClass = Class.forName(className);
					TransportService transport = (TransportService) transportClass
							.getConstructor(AgentFactory.class)
							.newInstance(this);
					transport.init(transportParams);

					// register the service with the agent factory
					addTransportService(transport);
				}
				else {
					logger.warning("Cannot load transport service at index " + index + 
							": no class defined.");
				}
			}
			catch (Exception e) {
				logger.warning("Cannot load service at index " + index + 
						": " + e.getMessage());
			}
			index++;
		}
	}

	/**
	 * Add a new transport service
	 * @param transportService
	 */
	public void addTransportService(TransportService transportService) {
		transportServices.add(transportService);
		logger.info("Registered transport service: " + transportService.toString());
	}

	/**
	 * Remove a registered a transport service
	 * @param transportService
	 */
	public void removeTransportService(TransportService transportService) {
		transportServices.remove(transportService);
		logger.info("Unregistered transport service " + transportService.toString());
	}

	/**
	 * Get all registered transport services
	 * @return transportService
	 */
	public List<TransportService> getTransportServices() {
		return transportServices;
	}
	
	/**
	 * Get all registered transport services which can handle given protocol
	 * @param protocol   A protocol, for example "http" or "xmpp"
	 * @return transportService
	 */
	public List<TransportService> getTransportServices(String protocol) {
		List<TransportService> filteredServices = new ArrayList<TransportService> ();
		
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
	 * @param protocol   A protocol, for example "http" or "xmpp"
	 * @return service
	 */
	public TransportService getTransportService(String protocol) {
		List<TransportService> services = getTransportServices(protocol);
		if (services.size() > 0) {
			return services.get(0);
		}
		return null;
	}

	/**
	 * create a scheduler for an agent
	 * @param agentId
	 * @return scheduler
	 */
	public Scheduler getScheduler(String agentId) {
		return schedulerFactory.getScheduler(agentId);
	}
	
	// Note: the CopyOnWriteArrayList is inefficient but thread safe. 
	private List<TransportService> transportServices = new CopyOnWriteArrayList<TransportService>();
	private ContextFactory contextFactory = null;
	private SchedulerFactory schedulerFactory = null;
	private Config config = null;

	private static Map<String, AgentFactory> factories = 
			new ConcurrentHashMap<String, AgentFactory>();  // namespace:factory

	private EventLogger eventLogger = new EventLogger(this);
	
	private final static Map<String, String> CONTEXT_FACTORIES = new HashMap<String, String>();
	static {
        CONTEXT_FACTORIES.put("FileContextFactory", "com.almende.eve.context.FileContextFactory");
        CONTEXT_FACTORIES.put("MemoryContextFactory", "com.almende.eve.context.MemoryContextFactory");
        CONTEXT_FACTORIES.put("DatastoreContextFactory", "com.almende.eve.context.google.DatastoreContextFactory");
    }

	private final static Map<String, String> SCHEDULERS = new HashMap<String, String>();
	static {
		SCHEDULERS.put("RunnableSchedulerFactory",  "com.almende.eve.scheduler.RunnableSchedulerFactory");
		SCHEDULERS.put("AppEngineSchedulerFactory", "com.almende.eve.scheduler.google.AppEngineSchedulerFactory");
    }
	
	private final static Map<String, String> TRANSPORT_SERVICES = new HashMap<String, String>();
	static {
		TRANSPORT_SERVICES.put("XmppTransportService", "com.almende.eve.transport.xmpp.XmppTransportService");
		TRANSPORT_SERVICES.put("HttpTransportService", "com.almende.eve.transport.http.HttpServiceService");
    }

	private static AgentCache agents;
	
	private Logger logger = Logger.getLogger(this.getClass().getSimpleName());
}
