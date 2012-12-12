package com.almende.eve.scheduler;

import com.almende.eve.agent.AgentFactory;
import com.almende.eve.json.JSONRequest;

public abstract class Scheduler {
	public Scheduler (AgentFactory agentFactory, String agentId) {
		this.agentFactory = agentFactory;
		this.agentId = agentId;
	}

	/**
	 * Schedule a task
	 * @param request   A JSONRequest with method and params
	 * @param delay     The delay in milliseconds
	 * @return taskId
	 */
	public abstract String createTask(JSONRequest request, long delay);

	/**
	 * Cancel a scheduled task by its id
	 * @param taskId
	 */
	public abstract void cancelTask(String id);
	
	protected AgentFactory agentFactory = null;
	protected String agentId = null;
}
