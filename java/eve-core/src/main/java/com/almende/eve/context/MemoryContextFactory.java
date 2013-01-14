package com.almende.eve.context;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.almende.eve.agent.AgentFactory;

public class MemoryContextFactory extends ContextFactory {
	public MemoryContextFactory (AgentFactory agentFactory, Map<String, Object> params) {
		super(agentFactory, params);
	}

	public MemoryContextFactory (AgentFactory agentFactory) {
		super(agentFactory, null);
	}

	/**
	 * Get context with given id. Will return null if not found
	 * @param agentId
	 * @return context
	 */
	@Override
	public MemoryContext get(String agentId) {
		return contexts.get(agentId);
	}
	
	/**
	 * Create a context with given id. Will throw an exception when already.
	 * existing.
	 * @param agentId
	 * @return context
	 */
	@Override
	public synchronized MemoryContext create(String agentId) throws Exception {
		if (contexts.containsKey(agentId)) {
			throw new Exception("Cannot create context, " + 
					"context with id '" + agentId + "' already exists.");
		}
		
		MemoryContext context = new MemoryContext(agentId); 
		contexts.put(agentId, context);
		
		return context;
	}
	
	/**
	 * Delete a context. If the context does not exist, nothing will happen.
	 * @param agentId
	 */
	@Override
	public void delete(String agentId) throws Exception {
		contexts.remove(agentId);
	}

	/**
	 * Test if a context with given id exists.
	 * @param agentId
	 * @return exists
	 */
	@Override
	public boolean exists(String agentId) {
		return contexts.containsKey(agentId);
	}

	/**
	 * Get the current environment, "Production" or "Development". 
	 * In case of a memory context, this will always return "Production".
	 * @return environment
	 */
	@Override
	public String getEnvironment() {
		return "Production";
	}

	// Singleton containing all contexts, stored per id
	private Map<String, MemoryContext> contexts = 
		new ConcurrentHashMap<String, MemoryContext>();
}
