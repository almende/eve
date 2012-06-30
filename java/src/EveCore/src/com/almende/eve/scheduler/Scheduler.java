package com.almende.eve.scheduler;

import java.util.Set;

import com.almende.eve.context.Context;
import com.almende.eve.json.JSONRequest;

public interface Scheduler {
	/**
	 * Set the agent context for the scheduler. 
	 * This is needed for example to know for which agent the scheduler
	 * needs to schedule a task
	 * @param context
	 */
	public void setContext(Context context);
	
	/**
	 * Schedule a task
	 * @param request   A JSONRequest with method and params
	 * @param delay     The delay in milliseconds
	 * @return taskId
	 */
	public String createTask(JSONRequest request, long delay);
	
	/**
	 * Schedule a repeating task
	 * @param request   A JSONRequest with method and params
	 * @param interval  The interval in milliseconds
	 * @return taskId
	 */
	public String createRepeatingTask(JSONRequest request, long interval);
	
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
