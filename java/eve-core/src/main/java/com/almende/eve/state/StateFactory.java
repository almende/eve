/*
 * Copyright: Almende B.V. (2014), Rotterdam, The Netherlands
 * License: The Apache Software License, Version 2.0
 */
package com.almende.eve.state;

import java.io.IOException;
import java.util.Iterator;

/**
 * A factory for creating State objects.
 */
public interface StateFactory {
	
	/**
	 * Get state with given id. Returns null if not found
	 *
	 * @param agentId the agent id
	 * @return state
	 */
	State get(String agentId);
	
	/**
	 * Create a state with given id. Will throw an exception when already
	 * existing.
	 *
	 * @param agentId the agent id
	 * @return state
	 * @throws IOException Signals that an I/O exception has occurred.
	 */
	State create(String agentId) throws IOException;
	
	/**
	 * Delete a state. If the state does not exist, nothing will happen.
	 *
	 * @param agentId the agent id
	 */
	void delete(String agentId);
	
	/**
	 * Test if a state with given id exists.
	 *
	 * @param agentId the agent id
	 * @return exists
	 */
	boolean exists(String agentId);
	
	/**
	 * Get an interator on all agents.
	 *
	 * @return Iterator<Agent>
	 */
	Iterator<String> getAllAgentIds();
}
