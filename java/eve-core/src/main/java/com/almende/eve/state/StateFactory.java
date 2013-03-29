package com.almende.eve.state;

import java.util.Iterator;

public abstract interface StateFactory {
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
	 * Get an interator on all agents
	 * @return Iterator<Agent>
	 */
	public abstract Iterator<String> getAllAgentIds();
}
