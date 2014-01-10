package com.almende.eve.scheduler;

import java.io.IOException;
import java.io.Serializable;
import java.net.URI;
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

import com.almende.eve.agent.AgentHost;
import com.almende.eve.agent.AgentInterface;
import com.almende.eve.rpc.jsonrpc.JSONRPCException;
import com.almende.eve.rpc.jsonrpc.JSONRequest;
import com.almende.eve.rpc.jsonrpc.jackson.JOM;
import com.almende.eve.state.State;
import com.almende.util.TypeUtil;

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
	private final ScheduledExecutorService			scheduler	= Executors
																		.newScheduledThreadPool(8);
	
	// {agentId: {taskId: task}}
	private final Map<String, Map<String, Task>>	allTasks	= new ConcurrentHashMap<String, Map<String, Task>>();
	
	private static final Logger						LOG			= Logger.getLogger(RunnableSchedulerFactory.class
																		.getSimpleName());
	
	/**
	 * This constructor is called when constructed by the AgentHost
	 * 
	 * @param AgentHostDefImpl
	 * @param params
	 */
	public RunnableSchedulerFactory(final AgentHost host,
			final Map<String, Object> params) {
		this(host, (params != null) ? (String) params.get("id") : null);
	}
	
	public RunnableSchedulerFactory(final AgentHost host, final String id) {
		this.host = host;
		stateId = id;
		
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
		} catch (final Exception e) {
			LOG.log(Level.WARNING, "Can't init State", e);
		}
	}
	
	/**
	 * Get a scheduler for a specific agent
	 * 
	 * @param agentId
	 */
	@Override
	public Scheduler getScheduler(final AgentInterface agent) {
		return new RunnableScheduler(agent.getId());
	}
	
	@Override
	public void destroyScheduler(final String agentId) {
		allTasks.remove(agentId);
	}
	
	/**
	 * Create a new unique taskId
	 * 
	 * @return taskId
	 */
	private synchronized String createTaskId() {
		count++;
		final long id = count;
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
		Task(final String agentId, final JSONRequest request, final long delay,
				final boolean interval, final boolean sequential) {
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
		 */
		Task(final Map<String, String> params) throws JSONRPCException,
				IOException {
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
			// persist the task, must be before schedule, because otherwise it
			// will report as cancelled!
			store();
			// TODO: Double threading with send method!
			// TODO: fix sequential calls
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
						
						final String receiverUrl = "local:" + agentId;
						final AgentInterface sender = host.getAgent(agentId);
						if (sender == null) {
							throw new IllegalStateException(
									"Sending agent is missing:" + agentId);
						}
						sender.send(request, URI.create(receiverUrl), null);
						
						if (interval > 0 && sequential && !cancelled()) {
							start(interval);
						}
					} catch (final Exception e) {
						LOG.log(Level.WARNING, "", e);
					} finally {
						if (interval <= 0) {
							remove();
						}
					}
				}
			}, delay, TimeUnit.MILLISECONDS);
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
				final boolean mayInterruptIfRunning = false;
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
			final Map<String, Task> tasks = allTasks.get(agentId);
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
			final Map<String, Task> tasks = allTasks.get(agentId);
			if (tasks != null) {
				final Task storedTask = tasks.get(taskId);
				if (storedTask != null) {
					return false;
				}
			}
			return true;
		}
		
		public Map<String, String> getParams() {
			final Map<String, String> params = new HashMap<String, String>();
			params.put("agentId", agentId);
			params.put("request", request.toString());
			params.put("timestamp", timestamp.toString());
			params.put("interval", new Long(interval).toString());
			params.put("sequential", Boolean.valueOf(sequential).toString());
			return params;
		}
		
		@Override
		public String toString() {
			try {
				return JOM.getInstance().writeValueAsString(this);
			} catch (final Exception e) {
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
		private RunnableScheduler(final String agentId) {
			this.agentId = agentId;
		}
		
		/**
		 * Schedule a task
		 * 
		 * @param request
		 *            A JSONRequest with method and params
		 * @param delay
		 *            The delay in milliseconds
		 * @param repeat
		 *            Should the task be repeated at an interval?
		 * @param sequential
		 *            Should (long running) tasks run sequential, or may they
		 *            run in parallel?
		 * @return taskId
		 */
		@Override
		public String createTask(final JSONRequest request, final long delay,
				final boolean repeat, final boolean sequential) {
			final Task task = new Task(agentId, request, delay, repeat,
					sequential);
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
		@Override
		public String createTask(final JSONRequest request, final long delay) {
			return createTask(request, delay, false, false);
		}
		
		/**
		 * Cancel a scheduled task by its id
		 * 
		 * @param taskId
		 */
		@Override
		public void cancelTask(final String taskId) {
			final Map<String, Task> tasks = allTasks.get(agentId);
			if (tasks != null) {
				final Task task = tasks.get(taskId);
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
			final Map<String, Task> tasks = allTasks.get(agentId);
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
			final Map<String, Task> tasks = allTasks.get(agentId);
			final HashSet<String> result = new HashSet<String>();
			if (tasks != null) {
				for (final Task task : tasks.values()) {
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
			for (final String id : getTasks()) {
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
			final List<Map<String, String>> serializedTasks = state.get(
					"tasks", new TypeUtil<List<Map<String, String>>>() {
					});
			
			if (serializedTasks != null) {
				for (final Map<String, String> taskParams : serializedTasks) {
					taskCount++;
					try {
						// start the task
						new Task(taskParams);
						// TODO: optimize: when a new Task is created, it will
						// automatically
						// store and persist allTasks again. That is inefficient
					} catch (final Exception e) {
						LOG.log(Level.WARNING, "", e);
						failedTaskCount++;
					}
				}
			}
		} catch (final Exception e) {
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
		final ArrayList<Map<String, String>> serializedTasks = new ArrayList<Map<String, String>>();
		
		for (final Entry<String, Map<String, Task>> allEntry : allTasks
				.entrySet()) {
			final Map<String, Task> tasks = allEntry.getValue();
			for (final Entry<String, Task> entry : tasks.entrySet()) {
				final Task task = entry.getValue();
				serializedTasks.add(task.getParams());
			}
		}
		
		state.put("tasks", serializedTasks);
	}
	
}
