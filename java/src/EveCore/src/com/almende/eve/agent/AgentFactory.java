package com.almende.eve.agent;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

import com.almende.eve.agent.log.LogAgent;
import com.almende.eve.config.Config;
import com.almende.eve.context.Context;
import com.almende.eve.json.JSONRPC;
import com.almende.eve.json.JSONRequest;
import com.almende.eve.json.JSONResponse;
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
 *     // invoke agents
 *     response = factory.invoke(url, request); // invoke a local or remote agent
 *     response = factory.invoke(agentClass, agentId, request); // invoke a local agent
 *     
 *     // instantiate an agent
 *     Agent agent = factory.getAgent(agentClass, agentId); // load local agent
 *     String desc = agent.getDescription(); // use the agent
 *     agent.destroy(); // neatly shutdown context
 * 
 * @author jos
 */
public class AgentFactory {
	protected AgentFactory () {}

	/**
	 * Construct an AgentFactory and initialize the configuration
	 * @param config
	 * @throws Exception
	 */
	public AgentFactory(Config config) throws Exception {
		setConfig(config);
	}
	
	/**
	 * Initialize an agent by its url.
	 * The agent must be located in the configured servlet, i.e. it may not 
	 * be a remote agent.
	 * 
	 * Before deleting the agent, the method agent.destroy() must be executed
	 * to neatly shutdown the instantiated context.
	 * @param agentUrl
	 * @return agent
	 * @throws Exception
	 */
	public Agent getAgent(String agentUrl) throws Exception {
		AgentAddress addr= splitAgentUrl(agentUrl);
		return getAgent(addr.agentClass, addr.agentId);
	}
	
	/**
	 * Get an agent by its class and id. 
	 * If the agent class is marked thread-safe, the agent will be instantiated
	 * only once, and will be kept in memory.
	 * 
	 * Before deleting the agent, the method agent.destroy() must be executed
	 * to neatly shutdown the instantiated context.
	 * 
	 * @param agentClass  simple class name
	 * @param agentId
	 * @return
	 * @throws Exception
	 */
	public Agent getAgent(String agentClass, String agentId) throws Exception {
		AgentClass ac = getAgentClass(agentClass);
		if (ac.threadSafe) {
			// use existing instance (load if not yet instantiated)
			String key = agentClass + "/" + agentId;
			if (agents.containsKey(key)) {
				// agent is already instantiated, return this instance
				return agents.get(key);
			}
			else {
				// agent is not yet instantiated, create a new instance
				Agent agent = createAgent(agentClass, agentId);

				// store the agent in map with the currently running agents
				agents.put(key, agent);
				
				// return new instance
				return agent;
			}
		}
		else {
			// create and return a new instance
			return createAgent(agentClass, agentId);
		}
	}

	/**
	 * Instantiate an agent by its class and id.
	 * 
	 * Before deleting the agent, the method agent.destroy() must be executed
	 * to neatly shutdown the instantiated context.
	 * 
	 * @param agentClass
	 * @param agentId
	 * @return
	 * @throws Exception
	 */
	private Agent createAgent(String agentClass, String agentId) throws Exception {
		// get the agent class
		AgentClass ac = getAgentClass(agentClass);
		Class<?> clazz = (ac != null) ? ac.classInstance : null;
		if (clazz == null) {
			throw new IllegalArgumentException(
					"Unknown agent class '" + agentClass + "'");
		}
		
		// instantiate the agent
		Agent agent = (Agent) clazz.getConstructor().newInstance();
		
		// instantiate the context
		Context context = (Context) contextClass
				.getConstructor(AgentFactory.class, String.class, String.class)
				.newInstance(this, agentClass, agentId);
		context.init();
		agent.setContext(context);
		
		// execute the init method of the agent
		agent.init();
		
		return agent;
	}
	
	/**
	 * Get the class of an agent by its simple class name.
	 * If the class name is not found in the initialized classes, null is 
	 * returned.
	 * @param simpleName   case insensitive simple class name
	 * @return agentClass
	 */
	public AgentClass getAgentClass(String simpleName) {
		return agentClasses.get(simpleName.toLowerCase());
	}
	
	/**
	 * Test whether an agents class is stateful
	 * @param simpleName
	 * @return
	 */
	public boolean isStateful(String simpleName) {
		AgentClass ac = getAgentClass(simpleName);
		return (ac != null) ? ac.stateful : false;
	}
	
	/**
	 * Append an agent class to the list with loaded agents
	 * @param agentClasses
	 * @param agentClass
	 */
	public void putAgentClass(Map<String, AgentClass> agentClasses, 
			AgentClass agentClass) {
		agentClasses.put(agentClass.simpleName, agentClass);
		logger.info("Agent class " + agentClass.className + " loaded " +
				"(stateful=" + agentClass.stateful + 
				", thread_safe=" + agentClass.threadSafe + ")");
	}
	
	/**
	 * Invoke a local agent
	 * @param agentClass
	 * @param agentId
	 * @param request
	 * @return
	 * @throws Exception
	 */
	public JSONResponse invoke(String agentClass, String agentId, 
			JSONRequest request) throws Exception {
		AgentClass ac = getAgentClass(agentClass);
		if (ac.threadSafe) {
			// keep the agent instance alive
			Agent agent = getAgent(agentClass, agentId);
			return JSONRPC.invoke(agent, request);
		}
		else {
			// destroy the agent after the method is executed. 
			// This forces the agent to persist and synchronize its context.
			JSONResponse response = null;
			Agent agent = createAgent(agentClass, agentId);
			try {
				response = JSONRPC.invoke(agent, request);
			}
			finally {
				agent.destroy();
			}
			return response;
		}
	}
	
	/**
	 * Invoke a local agent.
	 * @param agent
	 * @param request
	 * @return
	 * @throws Exception
	 */
	public JSONResponse invoke(Agent agent, JSONRequest request) throws Exception {
		return JSONRPC.invoke(agent, request);
	}

	/**
	 * Invoke a local or remote agent. 
	 * In case of an local agent, the agent is invoked immediately.
	 * In case of an remote agent, an HTTP Request is sent to the concerning
	 * agent.
	 * @param agentUrl
	 * @param request
	 * @return
	 * @throws Exception
	 */
	public JSONResponse invoke(String agentUrl, JSONRequest request) 
			throws Exception {
		if (isLocalUrl(agentUrl)) {
			// invoke locally
			AgentAddress addr = splitAgentUrl(agentUrl);
			return invoke(addr.agentClass, addr.agentId, request);
		}
		else {
			// send request to remote agent.
			return JSONRPC.send(agentUrl, request);
		}
	}
	
	/**
	 * Invoke a local agent.
	 * @param agentClass
	 * @param agentId
	 * @param request
	 * @return
	 * @throws Exception
	 */
	public String invoke(String agentClass, String agentId, String request) 
			throws Exception {
		JSONRequest jsonRequest = new JSONRequest(request);
		JSONResponse jsonResponse = invoke(agentClass, agentId, jsonRequest);
		return jsonResponse.toString();
	}

	/**
	 * Invoke a local or remote agent. 
	 * In case of an local agent, the agent is invoked immediately.
	 * In case of an remote agent, an HTTP Request is sent to the concerning
	 * agent.
     * @param agentUrl
	 * @param request
	 * @return
	 * @throws Exception
	 */
	public String invoke(String agentUrl, String request) throws Exception {
		JSONRequest jsonRequest = new JSONRequest(request);
		JSONResponse jsonResponse = invoke(agentUrl, jsonRequest);
		return jsonResponse.toString();
	}

	/**
	 * Get the configured servlet url, or null when not configured
	 * @return servletUrl
	 */
	public String getServletUrl() {
		if (servletUrl == null) {
			// read the servlet url from the config
			Config config = getConfig();
			if (config != null) {
				servletUrl = config.get("environment", getEnvironment(), "servlet_url");
				if (servletUrl != null) {
					 if (!servletUrl.endsWith("/")) {
						 servletUrl += "/";					 
					 }
				}
				else {
					String path = "environment." + getEnvironment() + ".servlet_url";
					Exception e = new Exception("Config parameter '" + path + "' is missing");
					e.printStackTrace();
				}
			}
			else {
				Exception e = new Exception("Configuration uninitialized");
				e.printStackTrace();
			}
		}
		return servletUrl;	
	}
	
	/**
	 * Retrieve the current environment, using the configured Context.
	 * Available values: "Production", "Development"
	 * @return environent
	 */
	public String getEnvironment() {
		if (environment == null) {
			try {
				Context context = 
						(Context) contextClass.getConstructor().newInstance();
				if (context != null) {
					environment = context.getEnvironment();
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		
		return environment;
	}

	/**
	 * Get the servlet path from the configured servlet url.
	 * @return servletPath
	 * @throws MalformedURLException
	 */
	private String getServletPath() throws MalformedURLException {
		String servletUrl = getServletUrl();
		String servletPath = new URL(servletUrl).getPath();
		return servletPath;	
	}
	
	/**
	 * Test whether given url corresponds with the configured servlet url.
	 * If so, the url is local, and the method will return true
	 * @param url
	 * @return isLocal
	 * @throws MalformedURLException
	 */
	public boolean isLocalUrl(String url) throws MalformedURLException {
		String servletUrl = getServletUrl();
		String servletPath = new URL(servletUrl).getPath();
		return url.startsWith(servletUrl) || url.startsWith(servletPath);
	}
	
	/**
	 * Split the agentClass, agentId, and resource from an agents url
	 * An agent url looks like:
	 *   http://server/servlet/agentClass/agentId/
	 *   http://server/servlet/agentClass/agentId/agentResource
	 * @param url
	 * @return agentAddress
	 * @throws ServletException
	 * @throws MalformedURLException
	 */
	public AgentAddress splitAgentUrl (String url) 
			throws IllegalArgumentException, MalformedURLException {
		String path = null;
		String servletUrl = getServletUrl();
		if (url.startsWith(servletUrl)) {
			path = url.substring(servletUrl.length()); 
		}
		else {
			String servletPath = getServletPath();
			if (url.startsWith(servletPath)) {
				path = url.substring(servletPath.length()); 
			}
			else {
				throw new IllegalArgumentException(
						"I don't get it. The request url '"  +
						url + "' does not match the configured servlet url '" + 
						servletUrl + "' or servlet path '" + servletPath + "'");
			}
		}

		String agentClass = null;
		String agentId = null;
		String agentResource = null;
		int slash1 = path.indexOf('/');
		if (slash1 != -1) {
			agentClass = path.substring(0, slash1);
			int slash2 = path.indexOf('/', slash1 + 1);
			if (slash2 != -1) {
				agentId = path.substring(slash1 + 1, slash2);
				agentResource = path.substring(slash2 + 1);
			}
			else {
				agentId = path.substring(slash1 + 1);
				agentResource = "";
			}
		}
		else {
			agentClass = "";
		}
		
		return new AgentAddress(url, agentClass, agentId, agentResource);
	}

	/**
	 * Set configuration file
	 * @param config   A loaded configuration file
	 * @throws Exception 
	 */
	public void setConfig(Config config) throws Exception {
		if (config == null) {
			throw new IllegalArgumentException("Config not initialized");
		}
		this.config = config;

		initContext();
		initAgents();
	}

	/**
	 * Get the loaded config file
	 * @return config   A configuration file
	 */
	public Config getConfig() {
		return config;
	}
	
	/**
	 * Initialize the correct Agent class for the SingleAgentServlet.
	 * The class is read from the servlet init parameters in web.xml.
	 * @throws Exception 
	 */
	private void initAgents() throws Exception {
		if (agentClasses != null) {
			return;
		}

		Map<String, AgentClass> newAgentClasses = 
				new ConcurrentHashMap<String, AgentClass> ();
		
		List<Map<String, Object>> agents = config.get("agents");
		if (agents != null) {
			// load the agents one by one
			for (int i = 0; i < agents.size(); i++) {
				Map<String, Object> properties = agents.get(i);
				try {
					AgentClass agentClass = new AgentClass(properties);
					putAgentClass(newAgentClasses, agentClass);
				}
				catch (Exception e) {
					logger.warning(e.getMessage());
				}
			}
		}
		else {
			// test for deprecated array agent.classes
			List<String> classes = config.get("agent", "classes");
			if (classes != null) {
				logger.warning("Property agent.classes[] is deprecated. Use agents[].class instead");
				
				for (int i = 0; i < classes.size(); i++) {
					try {
						String className = classes.get(i);
						AgentClass agentClass = new AgentClass(className);
						putAgentClass(newAgentClasses, agentClass);
					}
					catch (Exception e) {
						logger.warning(e.getMessage());
					}
				}
			}
			else {
				throw new IllegalArgumentException(
					"Config parameter 'agents[]' missing in Eve configuration.");
			}
		}
		
		// always load the log agent
		putAgentClass(newAgentClasses, new AgentClass(LogAgent.class));
		
		// put the newly loaded map with agent classes into the map of the AgentFactory
		agentClasses = newAgentClasses;
	}
	
	/**
	 * Initialize the correct Context class for the SingleAgentServlet.
	 * The class is read from the servlet init parameters in web.xml.
	 * @throws Exception
	 */
	private void initContext() throws Exception {
		if (contextClass != null) {
			return;
		}
		
		// get the class name from the config file
		String className = config.get("context", "class");
		if (className == null) {
			throw new IllegalArgumentException(
				"Config parameter 'context.class' missing in Eve configuration.");
		}
		
		// test for deprecated ContextFactory (deprecated since version 0.11, 2012-11-20)
		if (className.endsWith("ContextFactory")) {
			logger.warning("ContextFactory classes are deprecated. Please specify the Context itself. " + 
					"(Hint: change the configuration parameter context.class to " + 
					className.substring(0, className.length() - "Factory".length())+ ")");
		}
		
		// load the class
		Class<?> newContextClass = null;
		try {
			newContextClass = Class.forName(className);
		} catch (ClassNotFoundException e) {
			throw new IllegalArgumentException("Cannot find class " + className + "");
		}

		if (!ClassUtil.hasSuperClass(newContextClass, Context.class)) {
			throw new IllegalArgumentException(
					"Context class " + newContextClass.getName() + 
					" must implement interface " + Context.class.getName());
		}

		// copy the context if no errors
		this.contextClass = newContextClass;		
		logger.info("Context class " + contextClass.getName() + " loaded");
	}

	/**
	 * Helper class to hold the parts of an agent url: url, class, id, resource. 
	 */
	public class AgentAddress {
		public AgentAddress(String agentUrl, String agentClass, 
				String agentId, String agentResource) {
			this.agentUrl = agentUrl;
			this.agentClass = agentClass;
			this.agentId = agentId;
			this.agentResource = agentResource;
		}
		
		public String agentUrl = null;
		public String agentClass = null;
		public String agentId = null;
		public String agentResource = null;
	}
	
	/**
	 * Helper class to cast an agent object from a config file
	 */
	public class AgentClass {
		public AgentClass(Map<String, Object> properties) throws ClassNotFoundException {
			// read the properties
			if (properties.containsKey("thread_safe")) {
				threadSafe = (Boolean) properties.get("thread_safe");
			}
			if (properties.containsKey("stateful")) {
				stateful = (Boolean) properties.get("stateful");
			}
			
			if (stateful) {
				if (!threadSafe) {
					throw new IllegalArgumentException(
						"Stateful agent only supported when marked thread_safe");
				}
				// TODO: throw an Exception when on Google App Engine and stateful==true 
			}
			
			String className = (String) properties.get("class");
			setClass(className);
		}
		
		public AgentClass(String className) throws ClassNotFoundException {
			setClass(className);
		}
		
		public AgentClass(Class<?> classInstance) 
				throws NullPointerException, IllegalArgumentException {
			setClass(classInstance);
		}
		
		private void setClass(String name) 
				throws ClassNotFoundException, IllegalArgumentException {
			if (name != null && !name.isEmpty()) {
				// load class instance
				Class<?> classInstance = Class.forName(name);
				setClass(classInstance);
			}
			else {
				throw new IllegalArgumentException("Agent property 'class' undefined");
			}
		}
		
		private void setClass(Class<?> classInstance) 
				throws IllegalArgumentException, NullPointerException {
			if (classInstance == null) {
				throw new NullPointerException();				
			}
			
			if (!classInstance.getSuperclass().equals(Agent.class)) {
				throw new IllegalArgumentException("Class " + classInstance.getName() + 
						" must extend " + Agent.class.getName());
			}
			this.classInstance = classInstance;
			
			// test if the class contains valid JSON-RPC messages
			List<String> errors = JSONRPC.validate(classInstance);
			for (String e : errors) {
				logger.warning(e);
			}
			
			// extract class name and simple name
			className = classInstance.getName();
			simpleName = classInstance.getSimpleName().toLowerCase();			
		}
		
		public Class<?> classInstance = null;
		public String className = null;
		public String simpleName = null;
		public boolean threadSafe = false;
		public boolean stateful = false;
	}
	
	private Logger logger = Logger.getLogger(this.getClass().getSimpleName());

	private Map<String, AgentClass> agentClasses = null;
	private Class<?> contextClass = null;
	private Config config = null;
	private String servletUrl = null;
	private String environment = null;
	
	// map with all instantiated agents (only applicable for stateful agents)
	private Map<String, Agent> agents = new ConcurrentHashMap<String, Agent>();
}
