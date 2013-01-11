package com.almende.eve.context;

import java.util.Map;

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
 *     Context context = new Context("agentId");<br>
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
	public Context (String agentId) {
		this.agentId = agentId;
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
		// TODO: dangerous to use a generic context parameter to store the agent class, can be accidentally overwritten 
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

	// init and destroy methods
	public abstract void init();     // executed once after the agent is instantiated
	public abstract void destroy();  // executed once before the agent is destroyed
	
	protected String agentId = null;
	//protected AgentFactory agentFactory = null;
}
