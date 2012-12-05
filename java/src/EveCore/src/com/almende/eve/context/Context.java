package com.almende.eve.context;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.almende.eve.scheduler.Scheduler;
import com.almende.eve.service.Service;
import com.almende.eve.agent.AgentFactory;
import com.almende.eve.config.Config;

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
	 * parameters agentFactory, and agentId
	 */
	public Context (AgentFactory agentFactory, String agentId) {
		this.agentFactory = agentFactory;
		this.agentId = agentId;
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
	 * Get the configured agents class.
	 * @return agentClass
	 */
	public synchronized void setAgentClass(Class<?> agentClass) {
		put("class", agentClass.getName());
	}
	
	/**
	 * Get the configured agents class.
	 * @return agentClass
	 * @throws ClassNotFoundException 
	 */
	public synchronized Class<?> getAgentClass() throws ClassNotFoundException {
		String agentClass = (String) get("class");
		if (agentClass != null) {
			return Class.forName(agentClass);
		}
		else {
			return null;
		}
	}

	/**
	 * Returns the agents urls. An agent can have an url for every configured
	 * communication service
	 * @return
	 */
	public synchronized List<String> getAgentUrls() {
		List<String> urls = new ArrayList<String>();
		if (agentFactory != null) {
			for (Service service : agentFactory.getServices()) {
				String url = service.getAgentUrl(agentId);
				if (url != null) {
					urls.add(url);
				}
			}
		}
		return urls;
	}

	/**
	 * Get the scheduler, which can be used to schedule tasks.
	 * @return scheduler
	 */
	public abstract Scheduler getScheduler();

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
	
	protected String agentId = null;
	protected AgentFactory agentFactory = null;
}
