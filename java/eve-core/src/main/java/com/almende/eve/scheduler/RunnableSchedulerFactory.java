package com.almende.eve.scheduler;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

import com.almende.eve.agent.AgentFactory;
import com.almende.eve.context.Context;
import com.almende.eve.rpc.jsonrpc.JSONRequest;

/**
 * Documentation on Scheduling:
 * 	   http://docs.oracle.com/javase/1.5.0/docs/api/java/util/concurrent/ScheduledExecutorService.html
 * 	   http://www.javapractices.com/topic/TopicAction.do?Id=54
 */
public class RunnableSchedulerFactory implements SchedulerFactory {
	public RunnableSchedulerFactory (AgentFactory agentFactory, Map<String, Object> params) {
		this.agentFactory = agentFactory;
		init(params);
	}
	
	public RunnableSchedulerFactory (AgentFactory agentFactory, String id) {
		this.agentFactory = agentFactory;
		init(id);
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
	
	/**
	 * Get a scheduler for a specific agent
	 * @param agentId
	 */
	@Override
	public Scheduler getScheduler(String agentId) {
		return new RunnableScheduler(agentId);
	}
	
	/**
	 * Create a new unique taskId
	 * @return taskId
	 */
	private synchronized String createTaskId() {
		count++;
		long id = count;
		return Long.toString(id);
	}
	/* TODO
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
	*/
	
	private class Task {
		/**
		 * Schedule a task
		 * @param agentId   Id of the agent to be requested
		 * @param request   A JSONRequest with method and params
		 * @param delay     The delay in milliseconds
		 */
		Task(final String agentId, final JSONRequest request, long delay) {
			this.agentId = agentId;
			
			// create the task
			taskId = createTaskId();
		    future = scheduler.schedule(new Runnable() {
				@Override
				public void run() {
					try {
						agentFactory.invoke(agentId, request);
					} catch (Exception e) {
						e.printStackTrace();
					} finally {
						remove();
					}
				}
			}, delay, TimeUnit.MILLISECONDS);
		    
		    // persist the task
		    store();
		}

		public String getTaskId() {
			return taskId;
		}
		
		public void cancel() {
			if (future != null) {
				boolean mayInterruptIfRunning = false;
				future.cancel(mayInterruptIfRunning);
			}
			remove();
		}
		
		/**
		 * Store this task in the global task list
		 */
		private void store() {
			Map<String, Task> tasks = allTasks.get(agentId);
			if (tasks == null) {
				synchronized (allTasks) {
					tasks = allTasks.get(agentId);
					if (tasks == null) {
						tasks = new ConcurrentHashMap<String, Task>();
						allTasks.put(agentId, tasks);
					}
				}
			}
			tasks.put(taskId, this);
			
			// TODO: serialize and persist allTasks
		}
		
		/**
		 * Remove this task from the global task list
		 */
		private void remove () {
			Map<String, Task> tasks = allTasks.get(agentId);
			if (tasks != null) {
				tasks.remove(taskId);
				
				if (tasks.size() == 0) {
					synchronized (tasks) {
						synchronized (allTasks) {
							if (tasks.size() == 0) {
								allTasks.remove(agentId);
							}
						}
					}
				}
				
				// TODO: serialize and persist allTasks
			}
		}
		
		private String agentId = null;		
		private String taskId = null;
		private ScheduledFuture<?> future = null;
	}
	
	/**
	 * The RunnableSchedular class is the interface which the agents can 
	 * interact with. It can only be instantiated by the factory using the
	 * method getSchedular(agentId).
	 */
	public class RunnableScheduler implements Scheduler {
		private RunnableScheduler (String agentId) {
			this.agentId = agentId;
		}
		
		/**
		 * Schedule a task
		 * @param request   A JSONRequest with method and params
		 * @param delay     The delay in milliseconds
		 * @return taskId
		 */
		public String createTask(JSONRequest request, long delay) {
			Task task = new Task(agentId, request, delay);
			return task.getTaskId();
		}

		/**
		 * Cancel a scheduled task by its id
		 * @param taskId
		 */
		public void cancelTask(String taskId) {
			Map<String, Task> tasks = allTasks.get(agentId);
			if (tasks != null) {
				Task task = tasks.get(taskId);
				if (task != null) {
					task.cancel();
				}
			}
		}

		/**
		 * Retrieve a list with all scheduled tasks
		 * @return taskIds
		 */
		@Override
		public synchronized Set<String> getTasks() {
			Map<String, Task> tasks = allTasks.get(agentId);
			if (tasks != null) {
				return tasks.keySet();
			}
			return new HashSet<String>();
		}
		
		private String agentId = null;
	}

	private final AgentFactory agentFactory;
	private Context context = null;	

	private long count = 0;
	private final ScheduledExecutorService scheduler = 
		Executors.newScheduledThreadPool(1);

	// {agentId: {taskId: task}}
	private final Map<String, Map<String, Task>> allTasks = 
			new ConcurrentHashMap<String, Map<String, Task>>(); 

	private Logger logger = Logger.getLogger(this.getClass().getSimpleName());
}
