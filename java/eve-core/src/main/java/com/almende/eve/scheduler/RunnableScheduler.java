package com.almende.eve.scheduler;

import java.util.Map;
import java.util.HashMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import com.almende.eve.agent.AgentFactory;
import com.almende.eve.rpc.jsonrpc.JSONRequest;

public class RunnableScheduler extends Scheduler {
	// http://docs.oracle.com/javase/1.5.0/docs/api/java/util/concurrent/ScheduledExecutorService.html
	// http://www.javapractices.com/topic/TopicAction.do?Id=54
	public RunnableScheduler (AgentFactory agentFactory, String agentId) {
		super(agentFactory, agentId);
		
		// initialize a map to store the tasks for this agent
		if (agentId != null) {
			if (allTasks.containsKey(agentId)) {
				tasks = allTasks.get(agentId);
			}
			else {
				tasks = new HashMap<String, ScheduledFuture<?>>();
				allTasks.put(agentId, tasks);
			}
		}
	}

	private static long count = 0;
	private static final ScheduledExecutorService scheduler = 
		Executors.newScheduledThreadPool(1);

	private static final Map<String, Map<String, ScheduledFuture<?>>> allTasks = 
		new ConcurrentHashMap<String, Map<String, ScheduledFuture<?>>>(); // {agentId: {taskId: task}}
	private Map<String, ScheduledFuture<?>> tasks = null;      // {taskId: task}
	
	/**
	 * Schedule a task
	 * @param request   A JSONRequest with method and params
	 * @param delay     The delay in milliseconds
	 * @return taskId
	 */
	@Override
	public synchronized String createTask(JSONRequest request, long delay) {
		Task task = new Task(request);
	    ScheduledFuture<?> future = scheduler.schedule(
	    		task, delay, TimeUnit.MILLISECONDS );
	    
	    putFuture(task.getId(), future);
	    
		return task.getId();	
	}

	/**
	 * Schedule a repeating task
	 * @param request   A JSONRequest with method and params
	 * @param interval  The interval in milliseconds
	 * @return taskId
	 */
	/* TODO: cleanup deprecated repeating task
	@Override
	public synchronized String createRepeatingTask(JSONRequest request, long interval) {
		String url = getUrl();
		if (url == null) {
			return null;
		}
		Task task = new Task(url, request);
	    ScheduledFuture<?> future = scheduler.scheduleWithFixedDelay(
	    		task, interval, interval, TimeUnit.MILLISECONDS );
	    putFuture(task.getId(), future);
	    
		return task.getId();
	}
	*/
	
	/**
	 * Cancel a scheduled task by its id
	 * @param taskId
	 */
	@Override
	public synchronized void cancelTask(String id) {
		ScheduledFuture<?> scheduledFuture = tasks.get(id);

		if (scheduledFuture != null) {
			boolean mayInterruptIfRunning = false;
			scheduledFuture.cancel(mayInterruptIfRunning);
		}
	    
		removeFuture(id);
	}

	/**
	 * Retrieve a list with all scheduled tasks
	 * @return taskIds
	 */
	/* TODO: cleanup getTasks()
	@Override
	public synchronized Set<String> getTasks() {
		if (tasks != null) {
			return tasks.keySet();
		}
		return null;
	}
	*/
	
	/**
	 * Create a new unique taskId
	 * @return
	 */
	private synchronized String createTaskId() {
		count++;
		long id = count;
		return Long.toString(id);
	}
	
	private void putFuture(String id, ScheduledFuture<?> future) {
		if (tasks != null && id != null) {
			tasks.put(id, future);
		}
	}

	private void removeFuture(String id) {
		if (tasks != null && id != null) {
			tasks.remove(id);
		}
	}
	
	private boolean isDone(String id) {
		if (tasks != null && id != null) {
			ScheduledFuture<?> scheduledFuture = tasks.get(id);
			
			if (scheduledFuture != null) {
				return scheduledFuture.isDone();
			}
		}
		
		return true;
	}
	
	private class Task implements Runnable {
		private String id = null;
		private JSONRequest request = null;
		
		public Task (JSONRequest request) {
			this.id = createTaskId();
			this.request = request;
		}
		
		public String getId() {
			return id;
		}
		
		@Override
		public void run() {
			try {
				// TODO: test if the RunnableSchedular still works
				agentFactory.invoke(agentId, request);
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
			// FIXME: isDone does not work, as the task is not finished right now but currently being executed...
			if (isDone(id)) {
				removeFuture(id);
			}
		}	
	}
}
