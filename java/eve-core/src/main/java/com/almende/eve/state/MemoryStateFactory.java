package com.almende.eve.state;

import java.io.IOException;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class MemoryStateFactory implements StateFactory {
	// Singleton containing all states, stored per id
	private Map<String, ExtendedState> states = 
		new ConcurrentHashMap<String, ExtendedState>();
	
	/**
	 * This constructor is called when constructed by the AgentHost
	 * @param params
	 */
	public MemoryStateFactory (Map<String, Object> params) {}

	public MemoryStateFactory () {}

	/**
	 * Get state with given id. Will return null if not found
	 * @param agentId
	 * @return state
	 */
	@Override
	public ExtendedState get(String agentId) {
		return states.get(agentId);
	}
	
	/**
	 * Create a state with given id. Will throw an exception when already.
	 * existing.
	 * @param agentId
	 * @return state
	 */
	public synchronized ExtendedState create(String agentId) throws IOException {
		if (states.containsKey(agentId)) {
			throw new IllegalStateException("Cannot create state, " + 
					"state with id '" + agentId + "' already exists.");
		}
		
		ExtendedState state = new MemoryState(agentId); 
		states.put(agentId, state);
		
		return state;
	}
	
	/**
	 * Delete a state. If the state does not exist, nothing will happen.
	 * @param agentId
	 */
	@Override
	public void delete(String agentId) {
		states.remove(agentId);
	}

	/**
	 * Test if a state with given id exists.
	 * @param agentId
	 * @return exists
	 */
	@Override
	public boolean exists(String agentId) {
		return states.containsKey(agentId);
	}

	@Override
	public Iterator<String> getAllAgentIds() {
		return states.keySet().iterator();
	}

}
