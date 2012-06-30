package com.almende.eve.scheduler.google;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Set;
import java.util.logging.Logger;

import com.almende.eve.context.Context;
import com.almende.eve.json.JSONRequest;
import com.almende.eve.scheduler.Scheduler;

import com.google.appengine.api.taskqueue.Queue;
import com.google.appengine.api.taskqueue.QueueFactory;
import com.google.appengine.api.taskqueue.TaskHandle;
import static com.google.appengine.api.taskqueue.TaskOptions.Builder.withUrl;

public class AppEngineScheduler implements Scheduler {
	private Logger logger = Logger.getLogger(this.getClass().getSimpleName());

	private Context context = null;
	
	public AppEngineScheduler () {}

	@Override
	public void setContext(Context context) {
		this.context = context;
	}
	
	/**
	 * Schedule a task
	 * @param request   A JSONRequest with method and params
	 * @param delay     The delay in milliseconds
	 * @return taskId
	 */
	@Override
	public String createTask(JSONRequest request, long delay) {
		try {
			String url = null;
			if (context != null) {
				url = context.getAgentUrl();
			}
			if (url == null) {
				return null;
			}
			
			URL uri = new URL(url);
			String path = uri.getPath();
			
			Queue queue = QueueFactory.getDefaultQueue();
			TaskHandle task = queue.add(withUrl(path)
					.payload(request.toString())
					.countdownMillis(delay));
			
			/* TODO: store the task
			if (context != null) {
				Set<String> tasks = (Set<String>) context.get("tasks");
				if (tasks == null) {
					tasks = new HashSet<String>();
				}
				tasks.add(task.getName());
				
				context.put("tasks", tasks);
			}
			else {
				logger.warning("No context initialized. Timers cannot be stored");
			}
			*/
			
			return task.getName();			
		} catch (MalformedURLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		// TODO: throw error?
		return null;
	}

	/**
	 * Schedule a repeating task
	 * @param request   A JSONRequest with method and params
	 * @param interval  The interval in milliseconds
	 * @return taskId
	 */
	@Override
	public String createRepeatingTask(JSONRequest request, long interval) {
		// TODOL implement setInterval
		
		System.out.println("setInterval not yet supported by AppEngineScheduler");
		
		return null;
	}

	/**
	 * Cancel a scheduled task by its id
	 * @param taskId
	 */
	@Override
	public void cancelTask(String id) {
		/* TODO: store the task
		if (context != null) {
			Set<String> tasks = (Set<String>) context.get("tasks");
			if (tasks != null && tasks.contains(id)) {
				Queue queue = QueueFactory.getDefaultQueue();
				queue.deleteTask(id);
				tasks.remove(id);
				context.put("tasks", tasks);
			}
		}
		else {
			logger.warning("No context initialized. Timers cannot be canceled");
		}
		*/
		Queue queue = QueueFactory.getDefaultQueue();
		queue.deleteTask(id);
	}

	/**
	 * Retrieve a list with all scheduled tasks
	 * @return taskIds
	 */
	@Override
	public Set<String> getTasks() {
		/* TODO: be able to retrieve all tasks
		Set<String> tasks = null;
		
		if (context != null) {
			tasks = (Set<String>) context.get("tasks");
		}
		
		if (tasks == null) {
			tasks = new HashSet<String>();
		}

		return tasks;
		*/
		logger.warning("getTasks not yet implemented for AppEngineScheduler");
		
		return null;
	}
}
