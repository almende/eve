package com.almende.eve.agent;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import com.almende.eve.agent.log.LogAgent;
import com.almende.eve.config.Config;
import com.almende.eve.context.Context;
import com.almende.eve.context.ContextFactory;
import com.almende.eve.json.JSONRPC;
import com.almende.eve.json.JSONRequest;
import com.almende.eve.json.JSONResponse;

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
	public Agent getAgent(String agentClass, String agentId) throws Exception {
		// get the agent class
		Class<?> clazz = getAgentClass(agentClass);
		if (clazz == null) {
			throw new IllegalArgumentException(
					"Unknown agent class '" + agentClass + "'");
		}
		
		// instantiate the agent
		Agent agent = (Agent) clazz.getConstructor().newInstance();
		
		// instantiate the context
		Context context = contextFactory.getContext(agentClass, agentId);
		context.init();
		agent.setContext(context);
		
		// execute the init method of the agent
		agent.init();

		return agent;
	}
	
	/**
	 * Get the class of an agent by its classname.
	 * If the classname is not found in the initialized classes, null is 
	 * returned.
	 * @param className   case insensitive class name
	 * @return class
	 */
	public Class<?> getAgentClass(String className) {
		return agentClasses.get(className.toLowerCase());
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
		JSONResponse response = null;
		Agent agent = getAgent(agentClass, agentId);
		
		try {
			response = JSONRPC.invoke(agent, request);
		}
		finally {
			agent.destroy();
		}
		
		return response;
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
		String servletUrl = null;
		try {
			servletUrl = contextFactory.getServletUrl();
			if (!servletUrl.endsWith("/")) {
				servletUrl += "/";
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return servletUrl;
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

		Map<String, Class<?>> newAgentClasses = new HashMap<String, Class<?>>();
		
		List<String> classes = config.get("agent", "classes");
		if (classes == null) {
			throw new IllegalArgumentException(
				"Config parameter 'agent.classes' missing in Eve configuration.");
		}

		for (int i = 0; i < classes.size(); i++) {
			String className = classes.get(i);
			try {
				if (className != null && !className.isEmpty()) {
					Class<?> agentClass = Class.forName(className);

					if (!agentClass.getSuperclass().equals(Agent.class)) {
						throw new IllegalArgumentException("Class " + agentClass.getName() + 
								" must extend " + Agent.class.getName());
					}
					
					// test if the class contains valid JSON-RPC messages
					List<String> errors = JSONRPC.validate(agentClass);
					for (String e : errors) {
						logger.warning(e);
					}
					
					String simpleName = agentClass.getSimpleName().toLowerCase();
					newAgentClasses.put(simpleName, agentClass);
					
					logger.info("Agent class " + agentClass.getName() + " loaded");
				}
			} 
			catch (ClassNotFoundException e) {
				logger.warning("Agent class '" + className + "' not found");
			}
			catch (Exception e) {
				logger.warning(e.getMessage());
			}
		}
		
		String simpleName = LogAgent.class.getSimpleName().toLowerCase();
		newAgentClasses.put(simpleName, LogAgent.class);
		logger.info("Agent class " + LogAgent.class.getName() + " loaded");
		
		// copy to agentClasses once the map is loaded
		agentClasses = newAgentClasses;
	}
	
	/**
	 * Initialize the correct Context class for the SingleAgentServlet.
	 * The class is read from the servlet init parameters in web.xml.
	 * @throws Exception
	 */
	private void initContext() throws Exception {
		if (contextFactory != null) {
			return;
		}
		
		String className = config.get("context", "class");
		if (className == null) {
			throw new IllegalArgumentException(
				"Config parameter 'context.class' missing in Eve configuration.");
		}
		
		Class<?> contextClass = null;
		try {
			contextClass = Class.forName(className);
		} catch (ClassNotFoundException e) {
			throw new IllegalArgumentException("Cannot find class " + className + "");
		}
		
		if (!hasInterface(contextClass, ContextFactory.class)) {
			throw new IllegalArgumentException(
					"Context class " + contextClass.getName() + 
					" must implement interface " + ContextFactory.class.getName());
		}

		ContextFactory newContextFactory = 
			(ContextFactory) contextClass.getConstructor().newInstance();
		newContextFactory.setAgentFactory(this);
		newContextFactory.setConfig(config);
		
		// copy the context as soon as it is done
		contextFactory = newContextFactory;
		
		logger.info("Context class " + contextClass.getName() + " loaded");
	}

	/**
	 * Check if checkClass has implemented interfaceClass
	 * @param checkClass
	 * @param interfaceClass
	 */
	private boolean hasInterface(Class<?> checkClass, Class<?> interfaceClass) {
		Class<?>[] interfaces = checkClass.getInterfaces();
		
		for (Class<?> i : interfaces) {
			if (i.equals(interfaceClass)) {
				return true;
			}
		}
		
		return false;
	}

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
	
	private Logger logger = Logger.getLogger(this.getClass().getSimpleName());
	
	private Map<String, Class<?>> agentClasses = null;
	private ContextFactory contextFactory = null;
	private Config config = null;
}
