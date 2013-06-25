package com.almende.eve.scheduler;

import java.io.Serializable;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.joda.time.DateTime;

import com.almende.eve.agent.Agent;
import com.almende.eve.agent.AgentHost;
import com.almende.eve.rpc.RequestParams;
import com.almende.eve.rpc.annotation.Sender;
import com.almende.eve.rpc.jsonrpc.JSONRequest;
import com.almende.eve.rpc.jsonrpc.JSONResponse;
import com.almende.eve.rpc.jsonrpc.jackson.JOM;
import com.almende.eve.scheduler.clock.Clock;
import com.almende.eve.scheduler.clock.RunnableClock;

public class ClockSchedulerFactory implements SchedulerFactory {
	private static final Logger		LOG			= Logger.getLogger(ClockSchedulerFactory.class
														.getCanonicalName());
	private Map<String, Scheduler>	schedulers	= new HashMap<String, Scheduler>();
	private AgentHost				agentHost	= null;
	
	/**
	 * This constructor is called when constructed by the AgentHost
	 * 
	 * @param AgentHost
	 * @param params
	 */
	public ClockSchedulerFactory(AgentHost agentHost,
			Map<String, Object> params) {
		this(agentHost, "");
	}
	
	public ClockSchedulerFactory(AgentHost agentHost, String id) {
		this.agentHost = agentHost;
	}
	
	@Override
	public Scheduler getScheduler(Agent agent) {
		synchronized (schedulers) {
			if (schedulers.containsKey(agent.getId())) {
				return schedulers.get(agent.getId());
			}
			ClockScheduler scheduler;
			try {
				scheduler = new ClockScheduler(agent, agentHost);
				scheduler.run();
				schedulers.put(agent.getId(), scheduler);
				return scheduler;
			} catch (Exception e) {
				LOG.log(Level.SEVERE, "Couldn't init new scheduler", e);
			}
			return null;
		}
	}
	
	@Override
	public void destroyScheduler(String agentId) {
		synchronized (schedulers) {
			schedulers.remove(agentId);
		}
	}
}

class ClockScheduler implements Scheduler, Runnable {
	private static final Logger		LOG		= Logger.getLogger("ClockScheduler");
	private final Agent				myAgent;
	private final Clock				myClock;
	private final ClockScheduler	_this	= this;
	
	public ClockScheduler(Agent myAgent, AgentHost factory) {
		this.myAgent = myAgent;
		myClock = new RunnableClock();
	}
	
	@SuppressWarnings("unchecked")
	public TaskEntry getFirstTask() {
		TreeSet<TaskEntry> timeline = (TreeSet<TaskEntry>) myAgent.getState()
				.get("_taskList");
		if (timeline != null && !timeline.isEmpty()) {
			TaskEntry task = timeline.first();
			while (task != null && task.isActive()) {
				task = timeline.higher(task);
			}
			return task;
		}
		return null;
	}
	
	public void putTask(TaskEntry task) {
		putTask(task, false);
	}
	
	@SuppressWarnings("unchecked")
	public void putTask(TaskEntry task, boolean onlyIfExists) {
		Set<TaskEntry> oldTimeline = (TreeSet<TaskEntry>) myAgent.getState()
				.get("_taskList");
		Set<TaskEntry> timeline = null;
		boolean found = false;
		if (oldTimeline != null) {
			timeline = new TreeSet<TaskEntry>();
			TaskEntry[] arr = oldTimeline.toArray(new TaskEntry[0]);
			for (TaskEntry entry : arr) {
				if (!entry.getTaskId().equals(task.getTaskId())) {
					timeline.add(entry);
				} else {
					found = true;
					timeline.add(task);
				}
			}
		}
		if (!found && !onlyIfExists) {
			if (timeline == null) {
				timeline = new TreeSet<TaskEntry>();
			}
			timeline.add(task);
		}
		if (!myAgent.getState().putIfUnchanged("_taskList",
				(Serializable) timeline, (Serializable) oldTimeline)) {
			LOG.severe("need to retry putTask...");
			// recursive retry....
			putTask(task, onlyIfExists);
			return;
		}
	}
	
	@SuppressWarnings("unchecked")
	@Override
	public void cancelTask(String id) {
		TreeSet<TaskEntry> oldTimeline = (TreeSet<TaskEntry>) myAgent
				.getState().get("_taskList");
		TreeSet<TaskEntry> timeline = null;
		if (oldTimeline != null) {
			timeline = new TreeSet<TaskEntry>();
			TaskEntry[] arr = oldTimeline.toArray(new TaskEntry[0]);
			for (TaskEntry entry : arr) {
				if (!entry.getTaskId().equals(id)) {
					timeline.add(entry);
				}
			}
		}
		if (!myAgent.getState().putIfUnchanged("_taskList", timeline,
				oldTimeline)) {
			LOG.severe("need to retry cancelTask...");
			// recursive retry....
			cancelTask(id);
			return;
		}
	}
	
	public void runTask(final TaskEntry task) {
		if (task.getInterval() <= 0) {
			// Remove from list
			cancelTask(task.getTaskId());
		} else {
			if (!task.isSequential()) {
				task.setDue(DateTime.now().plus(task.getInterval()));
			} else {
				task.setActive(true);
			}
			_this.putTask(task, true);
			_this.run();
		}
		myClock.runInPool(new Runnable() {
			@Override
			public void run() {
				try {
					
					RequestParams params = new RequestParams();
					String senderUrl = "local://" + myAgent.getId();
					params.put(Sender.class, senderUrl);
					
					JSONResponse resp = myAgent.getAgentHost().receive(
							myAgent.getId(), task.getRequest(), params);
					
					if (task.getInterval() > 0 && task.isSequential()) {
						task.setDue(DateTime.now().plus(task.getInterval()));
						task.setActive(false);
						_this.putTask(task, true);
						_this.run();
					}
					if (resp.getError() != null) {
						throw resp.getError();
					}
				} catch (Exception e) {
					LOG.log(Level.SEVERE, "Failed to run scheduled task", e);
				}
			}
			
		});
	}
	
	@Override
	public String createTask(JSONRequest request, long delay) {
		return createTask(request, delay, false, false);
	}
	
	@Override
	public String createTask(JSONRequest request, long delay, boolean interval,
			boolean sequential) {
		TaskEntry task = new TaskEntry(DateTime.now().plus(delay), request,
				(interval ? delay : 0), sequential);
		if (delay <= 0) {
			runTask(task);
		} else {
			putTask(task);
			run();
		}
		return task.getTaskId();
	}
	
	@SuppressWarnings("unchecked")
	@Override
	public Set<String> getTasks() {
		Set<String> result = new HashSet<String>();
		TreeSet<TaskEntry> timeline = (TreeSet<TaskEntry>) myAgent.getState()
				.get("_taskList");
		if (timeline == null) {
			return result;
		}
		for (TaskEntry entry : timeline) {
			result.add(entry.getTaskId());
		}
		return result;
	}
	
	@Override
	public void run() {
		TaskEntry task = getFirstTask();
		if (task != null) {
			if (task.getDue().isBeforeNow()) {
				runTask(task);
				// recursive call next task
				run();
				return;
			}
			myClock.requestTrigger(myAgent.getId(), task.getDue(), this);
		}
	}
	
	@Override
	public String toString() {
		@SuppressWarnings("unchecked")
		TreeSet<TaskEntry> timeline = (TreeSet<TaskEntry>) myAgent.getState()
				.get("_taskList");
		return (timeline != null) ? timeline.toString() : "[]";
	}
}

class TaskEntry implements Comparable<TaskEntry>, Serializable {
	private static final Logger	LOG					= Logger.getLogger(TaskEntry.class
															.getCanonicalName());
	private static final long	serialVersionUID	= -2402975617148459433L;
	// TODO, make JSONRequest.equals() state something about real equal tasks,
	// use it as deduplication!
	private String				taskId;
	private JSONRequest			request;
	private DateTime			due;
	private long				interval			= 0;
	private boolean				sequential			= true;
	private boolean				active				= false;
	
	public TaskEntry(DateTime due, JSONRequest request, long interval,
			boolean sequential) {
		taskId = UUID.randomUUID().toString();
		this.request = request;
		this.due = due;
		this.interval = interval;
		this.sequential = sequential;
	}
	
	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (!(o instanceof TaskEntry)) {
			return false;
		}
		TaskEntry other = (TaskEntry) o;
		return taskId.equals(other.taskId);
	}
	
	@Override
	public int hashCode() {
		return taskId.hashCode();
	}
	
	@Override
	public int compareTo(TaskEntry o) {
		if (equals(o)) {
			return 0;
		}
		if (due.equals(o.due)) {
			return 0;
		}
		return due.compareTo(o.due);
	}
	
	public String getTaskId() {
		return taskId;
	}
	
	public JSONRequest getRequest() {
		return request;
	}
	
	public String getDueAsString() {
		return due.toString();
	}
	
	public DateTime getDue() {
		return due;
	}
	
	public long getInterval() {
		return interval;
	}
	
	public void setTaskId(String taskId) {
		this.taskId = taskId;
	}
	
	public void setRequest(JSONRequest request) {
		this.request = request;
	}
	
	public void setDue(DateTime due) {
		this.due = due;
	}
	
	public void setInterval(long interval) {
		this.interval = interval;
	}
	
	public void setSequential(boolean sequential) {
		this.sequential = sequential;
	}
	
	public void setActive(boolean active) {
		this.active = active;
	}
	
	public boolean isSequential() {
		return sequential;
	}
	
	public boolean isActive() {
		return active;
	}
	
	@Override
	public String toString() {
		try {
			return JOM.getInstance().writeValueAsString(this);
		} catch (Exception e) {
			LOG.log(Level.WARNING, "Couldn't use Jackson to print task.", e);
			return "{\"taskId\":" + taskId + ",\"due\":" + due + "}";
		}
	}
}
