package com.almende.eve.scheduler;

import java.io.IOException;
import java.io.Serializable;
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
import java.util.logging.Level;
import java.util.logging.Logger;

import org.joda.time.DateTime;
import org.joda.time.Interval;

import com.almende.eve.agent.Agent;
import com.almende.eve.agent.AgentHost;
import com.almende.eve.rpc.RequestParams;
import com.almende.eve.rpc.annotation.Sender;
import com.almende.eve.rpc.jsonrpc.JSONRPCException;
import com.almende.eve.rpc.jsonrpc.JSONRequest;
import com.almende.eve.rpc.jsonrpc.JSONResponse;
import com.almende.eve.rpc.jsonrpc.jackson.JOM;
import com.almende.eve.state.State;

/**
 * Documentation on Scheduling:
 * http://docs.oracle.com/javase/1.5.0/docs/api/java
 * /util/concurrent/ScheduledExecutorService.html
 * http://www.javapractices.com/topic/TopicAction.do?Id=54
 */
public class RunnableSchedulerFactory implements SchedulerFactory {
	private State									state		= null;
	private String									stateId		= null;
	private AgentHost								host		= null;
	private long									count		= 0;
	private ScheduledExecutorService				scheduler	= Executors
																		.newScheduledThreadPool(10);
	
	// {agentId: {taskId: task}}
	private final Map<String, Map<String, Task>>	allTasks	= new ConcurrentHashMap<String, Map<String, Task>>();
	
	private static final Logger						LOG			= Logger.getLogger(RunnableSchedulerFactory.class
																		.getSimpleName());
	
	/**
	 * This constructor is called when constructed by the AgentHost
	 * 
	 * @param AgentHost
	 * @param params
	 */
	public RunnableSchedulerFactory(AgentHost agentHost,
			Map<String, Object> params) {
		this(agentHost, (params != null) ? (String) params.get("id") : null);
	}
	
	public RunnableSchedulerFactory(AgentHost agentHost, String id) {
		this.host = agentHost;
		this.stateId = id;
		
		init();
	}
	
	/**
	 * Perform initialization tasks.
	 */
	private void init() {
		initState();
		initTasks();
	}
	
	/**
	 * Initialize a state for the service, to persist the parameters of all open
	 * connections.
	 * 
	 * @param id
	 */
	private void initState() {
		// set a state for the service, where the service can
		// persist its state.
		if (stateId == null || stateId.equals("")) {
			stateId = "_runnableScheduler";
			LOG.info("No id specified for RunnableSchedulerFactory. "
					+ "Using '" + stateId + "' as id.");
		}
		try {
			// TODO: dangerous to use a generic state (can possibly conflict
			// with the id a regular agent)
			state = host.getStateFactory().get(stateId);
			if (state == null) {
				state = host.getStateFactory().create(stateId);
				state.setAgentType(RunnableScheduler.class);
			}
		} catch (Exception e) {
			LOG.log(Level.WARNING, "Can't init State", e);
		}
	}
	
	/**
	 * Get a scheduler for a specific agent
	 * 
	 * @param agentId
	 */
	@Override
	public Scheduler getScheduler(Agent agent) {
		return new RunnableScheduler(agent.getId());
	}
	
	@Override
	public void destroyScheduler(String agentId) {
		allTasks.remove(agentId);
	}
	
	/**
	 * Create a new unique taskId
	 * 
	 * @return taskId
	 */
	private synchronized String createTaskId() {
		count++;
		long id = count;
		return Long.toString(id);
	}
	
	// TODO: make the class Task serializable (and auto-restart when
	// initializing again?)
	class Task implements Serializable {
		private static final long	serialVersionUID	= -2250937108323878021L;
		private String				agentId				= null;
		private String				taskId				= null;
		private JSONRequest			request				= null;
		private DateTime			timestamp			= null;
		private ScheduledFuture<?>	future				= null;
		private long				interval			= 0;
		private boolean				sequential			= false;
		
		/**
		 * Schedule a task
		 * 
		 * @param agentId
		 *            Id of the agent to be requested
		 * @param request
		 *            A JSONRequest with method and params
		 * @param delay
		 *            The delay in milliseconds
		 */
		Task(final String agentId, final JSONRequest request, long delay,
				boolean interval, boolean sequential) {
			// TODO: throw exceptions when agentId, request are null or
			// delay < 0
			this.agentId = agentId;
			this.request = request;
			if (interval) {
				this.interval = delay;
				this.sequential = sequential;
			}
			
			if (interval) {
				start(-1);
			} else {
				start(delay);
			}
		}
		
		/**
		 * Schedule a task
		 * 
		 * @param params
		 *            A Map with parameters: agentId, request (stringified
		 *            JSONRequest), and timestamp (ISOdate)
		 * @throws IOException
		 * @throws JSONRPCException
		 * @throws JsonMappingException
		 * @throws JsonParseException
		 */
		Task(Map<String, String> params) throws JSONRPCException, IOException {
			// TODO: throw exceptions when agentId, request are null or
			// delay < 0
			
			agentId = params.get("agentId");
			request = new JSONRequest(params.get("request"));
			timestamp = new DateTime(params.get("timestamp"));
			interval = Long.valueOf(params.get("interval"));
			sequential = Boolean.valueOf(params.get("sequential"));
			
			long delay = 0;
			if (timestamp.isAfterNow()) {
				delay = new Interval(DateTime.now(), timestamp)
						.toDurationMillis();
			}
			
			if (interval > 0) {
				start(-1);
			} else {
				start(delay);
			}
		}
		
		/**
		 * Start task
		 * 
		 * @param delay
		 *            delay in milliseconds
		 */
		private void start(final long delay) {
			// create the task
			timestamp = DateTime.now().plus(delay);
			if (taskId == null) {
				taskId = createTaskId();
			}
			future = scheduler.schedule(new Runnable() {
				@Override
				public void run() {
					try {
						if (cancelled()) {
							return;
						}
						if (interval > 0 && !sequential) {
							start(interval);
						}
						
						RequestParams params = new RequestParams();
						String senderUrl = "local:" + agentId;
						params.put(Sender.class, senderUrl);
						// TODO: provide itself
						
						JSONResponse resp = host.receive(agentId, request,
								params);
						
						if (interval > 0 && sequential && !cancelled()) {
							start(interval);
						}
						if (resp.getError() != null) {
							throw resp.getError();
						}
					} catch (Exception e) {
						LOG.log(Level.WARNING, "", e);
					} finally {
						if (interval <= 0) {
							remove();
						}
					}
				}
			}, delay, TimeUnit.MILLISECONDS);
			
			// persist the task
			store();
		}
		
		public String getTaskId() {
			return taskId;
		}
		
		public String getAgentId() {
			return agentId;
		}
		
		public JSONRequest getRequest() {
			return request;
		}
		
		public DateTime getTimestamp() {
			return timestamp;
		}
		
		public ScheduledFuture<?> getFuture() {
			return future;
		}
		
		public long getInterval() {
			return interval;
		}
		
		public boolean isSequential() {
			return sequential;
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
		private void remove() {
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
		
		/**
		 * Check if this task is still on the global Task list. If missing (e.g.
		 * due to cancel) returns true;
		 */
		private boolean cancelled() {
			Map<String, Task> tasks = allTasks.get(agentId);
			if (tasks != null) {
				Task storedTask = tasks.get(taskId);
				if (storedTask != null) {
					return false;
				}
			}
			return true;
		}
		
		public Map<String, String> getParams() {
			Map<String, String> params = new HashMap<String, String>();
			params.put("agentId", agentId);
			params.put("request", request.toString());
			params.put("timestamp", timestamp.toString());
			params.put("interval", new Long(interval).toString());
			params.put("sequential", Boolean.valueOf(sequential).toString());
			return params;
		}
		
		public String toString() {
			try {
				return JOM.getInstance().writeValueAsString(this);
			} catch (Exception e) {
				LOG.log(Level.WARNING, "", e);
				return super.toString();
			}
		}
	}
	
	/**
	 * The RunnableSchedular class is the interface which the agents can
	 * interact with. It can only be instantiated by the factory using the
	 * method getSchedular(agentId).
	 */
	public final class RunnableScheduler extends AbstractScheduler {
		private RunnableScheduler(String agentId) {
			this.agentId = agentId;
		}
		
		/**
		 * Schedule a task
		 * 
		 * @param request
		 *            A JSONRequest with method and params
		 * @param delay
		 *            The delay in milliseconds
		 * @param interval
		 *            Should the task be repeated at an interval?
		 * @param sequential
		 *            Should (long running) tasks run sequential, or may they
		 *            run in parallel?
		 * @return taskId
		 */
		public String createTask(JSONRequest request, long delay,
				boolean interval, boolean sequential) {
			Task task = new Task(agentId, request, delay, interval, sequential);
			return task.getTaskId();
		}
		
		/**
		 * Schedule a task
		 * 
		 * @param request
		 *            A JSONRequest with method and params
		 * @param delay
		 *            The delay in milliseconds
		 * @return taskId
		 */
		public String createTask(JSONRequest request, long delay) {
			return createTask(request, delay, false, false);
		}
		
		/**
		 * Cancel a scheduled task by its id
		 * 
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
		 * 
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
		
		/**
		 * Retrieve a list with all scheduled tasks
		 * 
		 * @return taskIds
		 */
		@Override
		public synchronized Set<String> getDetailedTasks() {
			Map<String, Task> tasks = allTasks.get(agentId);
			HashSet<String> result = new HashSet<String>();
			if (tasks != null) {
				for (Task task : tasks.values()) {
					result.add(task.toString());
				}
			}
			return new HashSet<String>();
		}
		
		@Override
		public String toString() {
			return allTasks.toString();
		}
		
		private String	agentId	= null;
		
		@Override
		public void cancelAllTasks() {
			for (String id : getTasks()) {
				cancelTask(id);
			}
		}
	}
	
	/**
	 * load scheduled, persisted tasks
	 */
	private void initTasks() {
		int taskCount = 0;
		int failedTaskCount = 0;
		
		try {
			List<Map<String, String>> list = new ArrayList<Map<String,String>>();
			
			@SuppressWarnings("unchecked")
			List<Map<String, String>> serializedTasks = state.get("tasks",list.getClass());
			
			if (serializedTasks != null) {
				for (Map<String, String> taskParams : serializedTasks) {
					taskCount++;
					try {
						// start the task
						new Task(taskParams);
						// TODO: optimize: when a new Task is created, it will
						// automatically
						// store and persist allTasks again. That is inefficient
					} catch (Exception e) {
						LOG.log(Level.WARNING, "", e);
						failedTaskCount++;
					}
				}
			}
		} catch (Exception e) {
			LOG.log(Level.WARNING, "", e);
		}
		
		LOG.info("Initialized "
				+ taskCount
				+ " tasks"
				+ ((failedTaskCount > 0) ? (" " + failedTaskCount + " tasks failed to start.")
						: ""));
	}
	
	/**
	 * Persist all currently running tasks
	 */
	private void storeTasks() {
		ArrayList<Map<String, String>> serializedTasks = new ArrayList<Map<String, String>>();
		
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
