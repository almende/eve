package com.almende.eve.scheduler;

import java.util.Set;

import com.almende.eve.rpc.annotation.Access;
import com.almende.eve.rpc.annotation.AccessType;
import com.almende.eve.rpc.jsonrpc.JSONRequest;

public interface Scheduler {
	/**
	 * Schedule a task
	 * @param request   A JSONRequest with method and params
	 * @param delay     The delay in milliseconds
	 * @return taskId
	 */
	String createTask(JSONRequest request, long delay);
	
	/**
	 * Schedule a task, potentially running at an interval
	 * 
	 * @param request    A JSONRequest with method and params
	 * @param delay      The delay in milliseconds
	 * @param interval   Should the task be repeated at an interval?
	 * @param sequential Should (long running) tasks run sequential, or may they run in parallel?
	 * @return taskId
	 */
	String createTask(JSONRequest request, long delay, boolean interval, boolean sequential);

	/**
	 * Cancel a scheduled task by its id
	 * @param taskId
	 */
	void cancelTask(String id);
	

	/**
	 * Retrieve a list with all scheduled tasks
	 * @return taskIds
	 */
	@Access(AccessType.PUBLIC)
	Set<String> getTasks();
	
}
