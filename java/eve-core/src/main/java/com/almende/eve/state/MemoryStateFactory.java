/*
 * Copyright: Almende B.V. (2014), Rotterdam, The Netherlands
 * License: The Apache Software License, Version 2.0
 */
package com.almende.eve.state;

import java.io.IOException;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * A factory for creating MemoryState objects.
 */
public class MemoryStateFactory implements StateFactory {
	/** Singleton containing all states, stored per id. */
	private final Map<String, State>	states	= new ConcurrentHashMap<String, State>();
	
	/**
	 * This constructor is called when constructed by the AgentHost.
	 * 
	 * @param params
	 *            the params
	 */
	public MemoryStateFactory(final Map<String, Object> params) {
	}
	
	/**
	 * Instantiates a new memory state factory.
	 */
	public MemoryStateFactory() {
	}
	
	/**
	 * Get state with given id. Will return null if not found
	 * 
	 * @param agentId
	 *            the agent id
	 * @return state
	 */
	@Override
	public State get(final String agentId) {
		return states.get(agentId);
	}
	
	/**
	 * Create a state with given id. Will throw an exception when already.
	 * existing.
	 * 
	 * @param agentId
	 *            the agent id
	 * @return state
	 * @throws IOException
	 *             Signals that an I/O exception has occurred.
	 */
	@Override
	public synchronized State create(final String agentId) throws IOException {
		if (states.containsKey(agentId)) {
			throw new IllegalStateException("Cannot create state, "
					+ "state with id '" + agentId + "' already exists.");
		}
		
		final State state = new MemoryState(agentId);
		states.put(agentId, state);
		
		return state;
	}
	
	/**
	 * Delete a state. If the state does not exist, nothing will happen.
	 * 
	 * @param agentId
	 *            the agent id
	 */
	@Override
	public void delete(final String agentId) {
		states.remove(agentId);
	}
	
	/**
	 * Test if a state with given id exists.
	 * 
	 * @param agentId
	 *            the agent id
	 * @return exists
	 */
	@Override
	public boolean exists(final String agentId) {
		return states.containsKey(agentId);
	}
	
	/*
	 * (non-Javadoc)
	 * 
	 * @see com.almende.eve.state.StateFactory#getAllAgentIds()
	 */
	@Override
	public Iterator<String> getAllAgentIds() {
		return states.keySet().iterator();
	}
	
}
