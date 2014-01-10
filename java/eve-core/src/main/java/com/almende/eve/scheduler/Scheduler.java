/*
 * Copyright: Almende B.V. (2014), Rotterdam, The Netherlands
 * License: The Apache Software License, Version 2.0
 */
package com.almende.eve.scheduler;

import java.util.Set;

import com.almende.eve.rpc.annotation.Access;
import com.almende.eve.rpc.annotation.AccessType;
import com.almende.eve.rpc.jsonrpc.JSONRequest;

/**
 * The Interface Scheduler.
 */
public interface Scheduler {
	
	/**
	 * Schedule a task.
	 *
	 * @param request A JSONRequest with method and params
	 * @param delay The delay in milliseconds
	 * @return taskId
	 */
	String createTask(JSONRequest request, long delay);
	
	/**
	 * Schedule a task, potentially running at an interval.
	 *
	 * @param request A JSONRequest with method and params
	 * @param delay The delay in milliseconds
	 * @param repeat the repeat
	 * @param sequential Should (long running) repetitive tasks run sequentially, or
	 * may they run in parallel? (are they threadsafe?)
	 * @return taskId
	 */
	String createTask(JSONRequest request, long delay, boolean repeat,
			boolean sequential);
	
	/**
	 * Cancel a scheduled task by its id.
	 *
	 * @param id the id
	 */
	void cancelTask(String id);
	
	/**
	 * Cancel all scheduled tasks.
	 *
	 */
	void cancelAllTasks();
	
	/**
	 * Retrieve a list with all scheduled tasks.
	 *
	 * @return taskIds
	 */
	@Access(AccessType.PUBLIC)
	Set<String> getTasks();
	
	/**
	 * Retrieve a list with all scheduled tasks.
	 *
	 * @return taskIds
	 */
	@Access(AccessType.PUBLIC)
	Set<String> getDetailedTasks();
	
}
