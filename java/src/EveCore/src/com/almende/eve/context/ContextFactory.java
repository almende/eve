package com.almende.eve.context;

import java.util.Map;

import com.almende.eve.agent.AgentFactory;

public abstract class ContextFactory {
	protected ContextFactory () {}
	
	public ContextFactory (AgentFactory agentFactory, Map<String, Object> params) {
		this.agentFactory = agentFactory;
	}
	
	/**
	 * Get context with given id. Return null if not found
	 * @param agentId
	 * @return context
	 */
	public abstract Context get(String agentId);
	
	/**
	 * Create a context with given id. Will throw an exception when already
	 * existing.
	 * @param agentId
	 * @return context
	 */
	public abstract Context create(String agentId) throws Exception;
	
	/**
	 * Delete a context. If the context does not exist, nothing will happen.
	 * @param agentId
	 */
	public abstract void delete(String agentId) throws Exception;
	
	/**
	 * Test if a context with given id exists.
	 * @param agentId
	 * @return exists
	 */
	public abstract boolean exists(String agentId);

	/**
	 * Get the current environment: "Production" or "Development"
	 * @return environment
	 */
	public abstract String getEnvironment();

	protected AgentFactory agentFactory = null;
}
