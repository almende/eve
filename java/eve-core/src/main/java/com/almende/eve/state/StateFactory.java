package com.almende.eve.state;

import java.io.IOException;
import java.util.Iterator;

public interface StateFactory {
	/**
	 * Get state with given id. Returns null if not found
	 * @param agentId
	 * @return state
	 */
	State get(String agentId);
	
	/**
	 * Create a state with given id. Will throw an exception when already
	 * existing.
	 * @param agentId
	 * @return state
	 */
	State create(String agentId) throws IOException;
	
	/**
	 * Delete a state. If the state does not exist, nothing will happen.
	 * @param agentId
	 */
	 void delete(String agentId);
	
	/**
	 * Test if a state with given id exists.
	 * @param agentId
	 * @return exists
	 */
	 boolean exists(String agentId);
	
	/**
	 * Get an interator on all agents
	 * @return Iterator<Agent>
	 */
	 Iterator<String> getAllAgentIds();
}
