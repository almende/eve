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
	public static String KEY_AGENT_TYPE = "_type"; // key name for agent type

	protected String agentId = null;

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
	 * Set the configured agents class.
	 * @return agentType
	 */
	public synchronized void setAgentType(Class<?> agentType) {
		// TODO: dangerous to use a generic context parameter to store the agent class, can be accidentally overwritten 
		put(KEY_AGENT_TYPE, agentType.getName());
	}
	
	/**
	 * Get the configured agents type (the full class path).
	 * @return type
	 * @throws ClassNotFoundException 
	 */
	public synchronized Class<?> getAgentType() throws ClassNotFoundException {
		String agentType = (String) get(KEY_AGENT_TYPE);
		if (agentType == null) {
			// try deprecated "class"
			agentType = (String) get("class");
			if (agentType != null) {
				put(KEY_AGENT_TYPE, agentType);
				remove("class");
			}
		}
		if (agentType != null) {
			return Class.forName(agentType);
		}
		else {
			return null;
		}
	}

	// init and destroy methods
	public abstract void init();     // executed once after the agent is instantiated
	public abstract void destroy();  // executed once before the agent is destroyed
}
