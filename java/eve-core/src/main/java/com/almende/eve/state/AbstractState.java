package com.almende.eve.state;

import java.lang.reflect.Type;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.almende.util.TypeUtil;
import com.fasterxml.jackson.databind.JavaType;

/**
 * @class State
 * 
 *        An interface for a state for Eve agents.
 *        The state provides general information for the agent (about itself,
 *        the environment, and the system configuration), and the agent can
 *        store
 *        persistent data in the state.
 *        The state extends a standard Java Map.
 * 
 *        Usage:<br>
 *        AgentHost factory = AgentHost.getInstance(config);<br>
 *        State state = new State("agentId");<br>
 *        state.put("key", "value");<br>
 *        System.out.println(state.get("key")); // "value"<br>
 * 
 * @author jos
 */
public abstract class AbstractState implements State {
	private static final Logger	LOG		= Logger.getLogger(AbstractState.class
												.getCanonicalName());
	private String				agentId	= null;
	
	/**
	 * The implemented classes must have a public constructor
	 */
	public AbstractState() {
	}
	
	/**
	 * The implemented classes must have this public constructor with
	 * parameters AgentHost, and agentId
	 */
	public AbstractState(String agentId) {
		this.agentId = agentId;
	}
	
	/**
	 * Get the agents id
	 * 
	 * @return agentId
	 */
	public synchronized String getAgentId() {
		return agentId;
	}
	
	/**
	 * Set the configured agents class.
	 * 
	 * @return agentType
	 */
	public synchronized void setAgentType(Class<?> agentType) {
		// TODO: dangerous to use a generic state parameter to store the agent
		// class, can be accidentally overwritten
		put(KEY_AGENT_TYPE, agentType.getName());
	}
	
	/**
	 * Get the configured agents type (the full class path).
	 * 
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
		} else {
			return null;
		}
	}
	
	// init and destroy methods
	
	// executed once after the agent is instantiated
	public abstract void init();
	
	// executed once before the agent is destroyed
	public abstract void destroy();
	
	@Override
	public <T> T get(String key, Class<T> type) {
		return TypeUtil.inject(type, get(key));
	}
	
	@Override
	public <T> T get(String key, Type type) {
		return TypeUtil.inject(type, get(key));
	}
	
	@Override
	public <T> T get(String key, JavaType type) {
		return TypeUtil.inject(type, get(key));
	}
	
	@Override
	public <T> T get(String key, TypeUtil<T> type) {
		return type.inject(get(key));
	}
	
	public <T> T get(T ret, String key) {
		try {
			ret = TypeUtil.inject(ret, get(key));
		} catch (Exception e) {
			LOG.log(Level.WARNING, "", e);
		}
		return ret;
	}
}
