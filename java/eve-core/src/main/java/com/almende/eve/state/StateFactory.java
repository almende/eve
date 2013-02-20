package com.almende.eve.state;

import java.util.HashMap;
import java.util.Map;

import com.almende.eve.agent.AgentFactory;

public abstract class StateFactory {
	protected StateFactory () {}
	
	public StateFactory (AgentFactory agentFactory, Map<String, Object> params) {
		this.agentFactory = agentFactory;
	}
	
	/**
	 * Get state with given id. Returns null if not found
	 * @param agentId
	 * @return state
	 */
	public abstract State get(String agentId);
	
	/**
	 * Create a state with given id. Will throw an exception when already
	 * existing.
	 * @param agentId
	 * @return state
	 */
	public abstract State create(String agentId) throws Exception;
	
	/**
	 * Delete a state. If the state does not exist, nothing will happen.
	 * @param agentId
	 */
	public abstract void delete(String agentId) throws Exception;
	
	/**
	 * Test if a state with given id exists.
	 * @param agentId
	 * @return exists
	 */
	public abstract boolean exists(String agentId);

	/**
	 * Get the current environment: "Production" or "Development"
	 * @return environment
	 */
	public abstract String getEnvironment();

	@Override
	public String toString() {
		Map<String, Object> data = new HashMap<String, Object>();
		data.put("type", this.getClass().getName());
		return data.toString();
	}
	
	protected AgentFactory agentFactory = null;
}
