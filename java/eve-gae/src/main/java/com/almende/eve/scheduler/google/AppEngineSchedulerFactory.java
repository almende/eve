package com.almende.eve.scheduler.google;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

import com.almende.eve.agent.AgentFactory;
import com.almende.eve.context.Context;
import com.almende.eve.rpc.jsonrpc.JSONRequest;
import com.almende.eve.scheduler.Scheduler;
import com.almende.eve.scheduler.SchedulerFactory;
import com.almende.eve.transport.TransportService;

import com.google.appengine.api.taskqueue.Queue;
import com.google.appengine.api.taskqueue.QueueFactory;
import com.google.appengine.api.taskqueue.TaskHandle;
import static com.google.appengine.api.taskqueue.TaskOptions.Builder.withUrl;

public class AppEngineSchedulerFactory implements SchedulerFactory {
	public AppEngineSchedulerFactory (AgentFactory agentFactory, 
			Map<String, Object> params) {
		// TODO: constructor
		this.agentFactory = agentFactory;
		init(params);
	}
	
	public AppEngineSchedulerFactory (AgentFactory agentFactory, String id) {
		this.agentFactory = agentFactory;
		init(id);
	}

	public AppEngineScheduler getScheduler(String agentId) {
		return new AppEngineScheduler(agentId);
	}

	/**
	 * initialize the settings for the scheduler
	 * @param params   Available parameters:
	 *                 {String} id   context id, to persist the running tasks
     */
	private void init(Map<String, Object> params) {
		String contextId = null;
		if (params != null) {
			contextId = (String) params.get("id");
		}
		init(contextId);
	}
	
	/**
	 * initialize the settings for the scheduler
	 * @param id       context id, to persist the running tasks
     */
	private void init(String id) {
		initContext(id);
		initScheduledTasks();
	}
	
	/**
	 * Initialize a context for the service, to persist the parameters of all
	 * open connections.
	 * @param id
	 */
	private void initContext (String id) {
		// set a context for the service, where the service can 
		// persist its state.
		if (id == null) {
			id = ".runnablescheduler";
			logger.info("No id specified for RunnableSchedulerFactory. " +
					"Using " + id + " as id.");
		}
		try {
			// TODO: dangerous to use a generic context (can possibly conflict with the id a regular agent)
			context = agentFactory.getContextFactory().get(id);
			if (context == null) {
				context = agentFactory.getContextFactory().create(id);
			}
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	/**
	 * Initialize scheduled tasks
	 */
	private void initScheduledTasks() {
		// TODO: implement initialization of persisted scheduled tasks
		
	}
	
	public class AppEngineScheduler implements Scheduler {
		private AppEngineScheduler(String agentId) {
			this.agentId = agentId;
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
		
		@Override 
		public Set<String> getTasks() {
			// TODO: implement getTasks
			return new HashSet<String>();
		}
		
		private String agentId = null;
	}
	
	private AgentFactory agentFactory = null;
	private Context context = null;

	private Logger logger = Logger.getLogger(this.getClass().getSimpleName());
}
