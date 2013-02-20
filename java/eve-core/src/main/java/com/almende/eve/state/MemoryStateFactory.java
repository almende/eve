package com.almende.eve.state;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.almende.eve.agent.AgentFactory;

public class MemoryStateFactory extends StateFactory {
	// Singleton containing all states, stored per id
	private Map<String, MemoryState> states = 
		new ConcurrentHashMap<String, MemoryState>();
	
	public MemoryStateFactory (AgentFactory agentFactory, Map<String, Object> params) {
		super(agentFactory, params);
	}

	public MemoryStateFactory (AgentFactory agentFactory) {
		super(agentFactory, null);
	}

	/**
	 * Get state with given id. Will return null if not found
	 * @param agentId
	 * @return state
	 */
	@Override
	public MemoryState get(String agentId) {
		return states.get(agentId);
	}
	
	/**
	 * Create a state with given id. Will throw an exception when already.
	 * existing.
	 * @param agentId
	 * @return state
	 */
	@Override
	public synchronized MemoryState create(String agentId) throws Exception {
		if (states.containsKey(agentId)) {
			throw new Exception("Cannot create state, " + 
					"state with id '" + agentId + "' already exists.");
		}
		
		MemoryState state = new MemoryState(agentId); 
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

	/**
	 * Get the current environment, "Production" or "Development". 
	 * In case of a memory state, this will always return "Production".
	 * @return environment
	 */
	@Override
	public String getEnvironment() {
		return "Production";
	}


}
