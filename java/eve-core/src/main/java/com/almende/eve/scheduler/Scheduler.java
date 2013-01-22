package com.almende.eve.scheduler;

import java.util.Set;

import com.almende.eve.rpc.jsonrpc.JSONRequest;

public interface Scheduler {
	/**
	 * Schedule a task
	 * @param request   A JSONRequest with method and params
	 * @param delay     The delay in milliseconds
	 * @return taskId
	 */
	public String createTask(JSONRequest request, long delay) ;

	/**
	 * Cancel a scheduled task by its id
	 * @param taskId
	 */
	public void cancelTask(String id);
	

	/**
	 * Retrieve a list with all scheduled tasks
	 * @return taskIds
	 */
	public Set<String> getTasks();
}
