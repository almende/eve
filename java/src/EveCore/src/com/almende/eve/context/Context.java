package com.almende.eve.context;

import java.util.Map;

import com.almende.eve.scheduler.Scheduler;
import com.almende.eve.agent.AgentFactory;
import com.almende.eve.config.Config;
import com.almende.eve.json.JSONRequest;
import com.almende.eve.json.JSONResponse;

/**
 * @class Context
 * 
 * An interface for a context for Eve agents.
 * The context provides general information for the agent (about itself,
 * the environment, and the system configuration), and the agent can store its 
 * state in the context. 
 * The context extends a standard Java Map.
 * 
 * Usage:<br>
 *     AgentFactory factory = AgentFactory(config);<br>
 *     Context context = new Context(factory, "agentClass", "agentId");<br>
 *     context.put("key", "value");<br>
 *     System.out.println(context.get("key")); // "value"<br>
 * 
 * @author jos
 */
public abstract class Context implements Map<String, Object> {
	/**
	 * The implemented classes must have a public constructor
	 */
	public Context () {}

	/**
	 * The implemented classes must have this public constructor with
	 * parameters agentFactory, agentClass, and agentId
	 */
	public Context (AgentFactory agentFactory, String agentClass, 
			String agentId) {
		this.agentFactory = agentFactory;
		this.agentId = agentId;
		this.agentClass = agentClass;
	}
	
	/**
	 * Get the current environment: "Production" or "Development"
	 * @return environment
	 */
	public abstract String getEnvironment();
	
	/**
	 * Get the Servlet url. Returns null if not configured
	 * @return servletUrl
	 */
	public synchronized String getServletUrl() {
		return (agentFactory != null) ? agentFactory.getServletUrl() : null;
	}
	
	/**
	 * Get the loaded configuration file. Can be used to read additional
	 * configuration parameters.
	 * @return config
	 */
	public synchronized Config getConfig() {
		// TODO: config should be read only
		return (agentFactory != null) ? agentFactory.getConfig() : null;
	}
	
	/**
	 * Get the agents id
	 * @return agentId
	 */	
	public synchronized String getAgentId() {
		return agentId;
	}
	
	/**
	 * Get the agents class. This is the classes simplename, not the full
	 * class path
	 * @return agentClass
	 */
	public synchronized String getAgentClass() {
		return agentClass;
	}
	
	/**
	 * Returns the agents url. Only applicable when a servletUrl is configured
	 * in the configuration file.
	 * @return
	 */
	public synchronized String getAgentUrl() {
		if (agentUrl == null) {
			String servletUrl = getServletUrl();
			if (servletUrl != null) {
				String url = servletUrl;
				if (!url.endsWith("/")) {
					url += "/";
				}
				if (agentClass != null) {
					url += agentClass + "/";
					if (agentId != null) {
						url += agentId + "/";
					}
				}
				agentUrl = url;
			}			
		}
		return agentUrl;
	}

	/**
	 * Get the scheduler, which can be used to schedule tasks.
	 * @return scheduler
	 */
	public abstract Scheduler getScheduler();
	
	// access to the AgentFactory, invoke or instantiate other agents 
	// (internal or external) via the context
	
	/**
	 * invoke an agent (internal or external) via the agent factory
	 * @throws Exception 
	 */
	public JSONResponse invoke(String url, JSONRequest request) throws Exception  {
		return agentFactory.invoke(url, request);
	}
	
	/**
	 * Get the agent factory. Can be used to instantiate new agents. 
	 * @return
	 */
	public AgentFactory getAgentFactory() {
		return agentFactory;
	}
	
	// init and destroy methods
	public abstract void init();     // executed once after the agent is instantiated
	public abstract void destroy();  // executed once before the agent is destroyed
	
	protected String agentUrl = null;
	protected String agentClass = null;
	protected String agentId = null;
	protected AgentFactory agentFactory = null;
}
