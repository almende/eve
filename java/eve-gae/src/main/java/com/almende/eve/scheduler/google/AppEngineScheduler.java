package com.almende.eve.scheduler.google;

import java.net.MalformedURLException;
import java.net.URL;

import com.almende.eve.agent.AgentFactory;
import com.almende.eve.rpc.jsonrpc.JSONRequest;
import com.almende.eve.scheduler.Scheduler;
import com.almende.eve.transport.TransportService;

import com.google.appengine.api.taskqueue.Queue;
import com.google.appengine.api.taskqueue.QueueFactory;
import com.google.appengine.api.taskqueue.TaskHandle;
import static com.google.appengine.api.taskqueue.TaskOptions.Builder.withUrl;

public class AppEngineScheduler extends Scheduler {
	public AppEngineScheduler (AgentFactory agentFactory, String agentId) {
		super(agentFactory, agentId);
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
			// TODO: getting an arbitrary http service which knows this agent
			//       is not safe
			//       -> Scheduler should be configured with the servlet_url 
			//          that it should use specified?
			TransportService service = null;
			for (TransportService s : agentFactory.getTransportServices("http")) {
				if (s.getAgentUrl(agentId) != null) {
					service = s;
					break;
				}
			}
			String url = null;
			if (service != null) {
				url = service.getAgentUrl(agentId);
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
}
