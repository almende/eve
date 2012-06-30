package com.almende.eve.scheduler;

import java.io.IOException;
import java.util.Map;
import java.util.HashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

import com.almende.eve.context.Context;
import com.almende.eve.json.JSONRPC;
import com.almende.eve.json.JSONRequest;

public class RunnableScheduler implements Scheduler {
	// http://docs.oracle.com/javase/1.5.0/docs/api/java/util/concurrent/ScheduledExecutorService.html
	// http://www.javapractices.com/topic/TopicAction.do?Id=54

	private Logger logger = Logger.getLogger(this.getClass().getSimpleName());

	private static long count = 0;
	private static final ScheduledExecutorService scheduler = 
		Executors.newScheduledThreadPool(1);

	private static final Map<String, Map<String, ScheduledFuture<?>>> allTasks = 
		new HashMap<String, Map<String, ScheduledFuture<?>>>(); // {agentUrl: {taskId: task}}
	private Map<String, ScheduledFuture<?>> tasks = null;      // {taskId: task}
	
	private Context context = null;
	
	public RunnableScheduler () {}
	
	/**
	 * Set the agent context for the scheduler. 
	 * This is needed for example to know for which agent the scheduler
	 * needs to schedule a task
	 * @param context
	 */
	@Override
	public void setContext(Context context) {
		this.context = context;
		
		// initialize a map to store the tasks for this agent
		String url = getUrl();
		if (url != null) {
			if (allTasks.containsKey(url)) {
				tasks = allTasks.get(url);
			}
			else {
				tasks = new HashMap<String, ScheduledFuture<?>>();
				allTasks.put(url, tasks);
			}
		}
	}
	
	/**
	 * Schedule a task
	 * @param request   A JSONRequest with method and params
	 * @param delay     The delay in milliseconds
	 * @return taskId
	 */
	@Override
	public synchronized String createTask(JSONRequest request, long delay) {
		String url = getUrl();
		if (url == null) {
			return null;
		}
		Task task = new Task(url, request);
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
		
		tasks.remove(id);
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
	 * Get the agents url from the currently set context
	 * @return
	 */
	private String getUrl() {
		if (context == null) {
			logger.warning("No context initialized, cannot retrieve url of agent");
			return null;
		}
		String url = context.getAgentUrl();
		if (url == null) {
			logger.warning("No agent url initialized in context");
		}
		return url;
	}

	/**
	 * Create a new unique taskId
	 * @return
	 */
	private synchronized String createId() {
		count++;
		long id = count;
		return Long.toString(id);
	}
	
	private synchronized void putFuture(String id, ScheduledFuture<?> future) {
		if (tasks != null && id != null) {
			tasks.put(id, future);
		}
	}

	private synchronized void removeFuture(String id) {
		if (tasks != null && id != null) {
			tasks.remove(id);
		}
	}
	
	private synchronized boolean isDone(String id) {
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
		private String url = null;
		private JSONRequest request = null;
		
		public Task (String url, JSONRequest request) {
			this.id = createId();
			this.url = url;
			this.request = request;
		}
		
		public String getId() {
			return id;
		}
		
		@Override
		public void run() {
			try {
				JSONRPC.send(url, request);
			} catch (IOException e) {
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
