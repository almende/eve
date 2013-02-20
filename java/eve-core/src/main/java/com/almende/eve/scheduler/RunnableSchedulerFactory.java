package com.almende.eve.scheduler;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

import org.joda.time.DateTime;
import org.joda.time.Interval;

import com.almende.eve.agent.AgentFactory;
import com.almende.eve.agent.annotation.Sender;
import com.almende.eve.rpc.RequestParams;
import com.almende.eve.rpc.jsonrpc.JSONRPCException;
import com.almende.eve.rpc.jsonrpc.JSONRequest;
import com.almende.eve.state.State;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;

/**
 * Documentation on Scheduling:
 * 	   http://docs.oracle.com/javase/1.5.0/docs/api/java/util/concurrent/ScheduledExecutorService.html
 * 	   http://www.javapractices.com/topic/TopicAction.do?Id=54
 */
public class RunnableSchedulerFactory implements SchedulerFactory {
	
	private AgentFactory agentFactory;
	private State state = null;	

	private long count = 0;
	private ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(10);

	// {agentId: {taskId: task}}
	private final Map<String, Map<String, Task>> allTasks = 
			new ConcurrentHashMap<String, Map<String, Task>>(); 

	private Logger logger = Logger.getLogger(this.getClass().getSimpleName());
	
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
	 *                 {String} id   state id, to persist the running tasks
     */
	private void init(Map<String, Object> params) {
		String stateId = null;
		if (params != null) {
			stateId = (String) params.get("id");
		}
		init(stateId);
	}

	/**
	 * initialize the settings for the scheduler
	 * @param id       state id, to persist the running tasks
     */
	private void init(String id) {
		initState(id);
		loadTasks();
	}
	
	/**
	 * Initialize a state for the service, to persist the parameters of all
	 * open connections.
	 * @param id
	 */
	private void initState (String id) {
		// set a state for the service, where the service can 
		// persist its state.
		if (id == null) {
			id = ".runnablescheduler";
			logger.info("No id specified for RunnableSchedulerFactory. " +
					"Using '" + id + "' as id.");
		}
		try {
			// TODO: dangerous to use a generic state (can possibly conflict with the id a regular agent)
			state = agentFactory.getStateFactory().get(id);
			if (state == null) {
				state = agentFactory.getStateFactory().create(id);
			}
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
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
	
	// TODO: make the class Task serializable (and auto-restart when initializing again?)
	private class Task {
		/**
		 * Schedule a task
		 * @param agentId   Id of the agent to be requested
		 * @param request   A JSONRequest with method and params
		 * @param delay     The delay in milliseconds
		 */
		Task(final String agentId, final JSONRequest request, long delay) {
			// TODO: throw exceptions when agentId, request are null or delay < 0
			this.agentId = agentId;
			this.request = request;
			
			start(delay);
		}
		
		
		/**
		 * Schedule a task
		 * @param params    A Map with parameters:
		 *                  agentId, request (stringified JSONRequest), and
		 *                  timestamp (ISOdate)
		 * @throws IOException 
		 * @throws JSONRPCException 
		 * @throws JsonMappingException 
		 * @throws JsonParseException 
		 */
		Task(Map<String, String> params) throws JsonParseException, 
				JsonMappingException, JSONRPCException, IOException {
			// TODO: throw exceptions when agentId, request are null or delay < 0
			
			agentId = params.get("agentId");
			request = new JSONRequest(params.get("request"));
			timestamp = new DateTime(params.get("timestamp"));
			
			long delay = 0;
			if (timestamp.isAfterNow()) {
				delay = new Interval(DateTime.now(), timestamp).toDurationMillis();
			}
			
			start(delay);
		}
		
		/**
		 * Start task
		 * @param delay   delay in milliseconds
		 */
		private void start(long delay) {
			// create the task
			timestamp = DateTime.now().plus(delay);
			taskId = createTaskId();
		    future = scheduler.schedule(new Runnable() {
				@Override
				public void run() {
					try {
						RequestParams params = new RequestParams(); 
						params.put(Sender.class, null);  // TODO: provide itself

						agentFactory.invoke(agentId, request, params);
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
			storeTasks();
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
				storeTasks();
			}
		}
		
		public Map<String, String> getParams () {
			Map<String, String> params = new HashMap<String, String>();
			params.put("agentId", agentId);
			params.put("request", request.toString());
			params.put("timestamp", timestamp.toString());
			return params;
		}
		
		private String agentId = null;
		private String taskId = null;
		private JSONRequest request = null;
		private DateTime timestamp = null;
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

	/**
	 * load scheduled, persisted tasks
	 */
	// TODO: storing all running tasks in one state file is quite a bottleneck and not scalable!
	private void loadTasks() {
		int taskCount = 0;
		int failedTaskCount = 0;
		
		try {
			@SuppressWarnings("unchecked")
			List<Map<String, String>> serializedTasks = 
					(List<Map<String, String>>) state.get("tasks");
			
			if (serializedTasks != null) {
				for (Map<String, String> taskParams : serializedTasks) {
					taskCount++;
		        	try {
		        		// start the task
						new Task(taskParams);
						// TODO: optimize: when a new Task is created, it will automatically
						//       store and persist allTasks again. That is inefficient 
		        	} catch (Exception e) {
						e.printStackTrace();
						failedTaskCount++;
					}
				}
			}
		}
		catch (Exception e) {
			e.printStackTrace();
		}
		
		logger.info("Initialized " + taskCount + " tasks" + 
				((failedTaskCount > 0) ? (" " + failedTaskCount + " tasks failed to start.") : ""));
	}
	
	/**
	 * Persist all currently running tasks
	 */
	private void storeTasks() {
		List<Map<String, String>> serializedTasks = 
				new ArrayList<Map<String, String>>();
		
		for (Entry<String, Map<String, Task>> allEntry : allTasks.entrySet()) {
	        Map<String, Task> tasks = allEntry.getValue();
	        for (Entry<String, Task> entry : tasks.entrySet()) {
		        Task task = entry.getValue();
		        serializedTasks.add(task.getParams());
	        }
	    }
		
		state.put("tasks", serializedTasks);
	}


}
