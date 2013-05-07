package com.almende.eve.scheduler;

import java.io.Serializable;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.UUID;
import java.util.logging.Logger;

import org.joda.time.DateTime;

import com.almende.eve.agent.Agent;
import com.almende.eve.agent.AgentFactory;
import com.almende.eve.agent.annotation.Sender;
import com.almende.eve.rpc.RequestParams;
import com.almende.eve.rpc.jsonrpc.JSONRequest;
import com.almende.eve.rpc.jsonrpc.jackson.JOM;
import com.almende.eve.scheduler.clock.Clock;
import com.almende.eve.scheduler.clock.RunnableClock;

public class ClockSchedulerFactory implements SchedulerFactory {
	Map<String, Scheduler>	schedulers		= new HashMap<String, Scheduler>();
	AgentFactory			agentFactory	= null;
	
	/**
	 * This constructor is called when constructed by the AgentFactory
	 * 
	 * @param agentFactory
	 * @param params
	 */
	public ClockSchedulerFactory(AgentFactory agentFactory,
			Map<String, Object> params) {
		this(agentFactory, "");
	}
	
	public ClockSchedulerFactory(AgentFactory agentFactory, String id) {
		this.agentFactory = agentFactory;
	}
	
	@Override
	public Scheduler getScheduler(Agent agent) {
		synchronized (schedulers) {
			if (schedulers.containsKey(agent.getId())) return schedulers.get(agent.getId());
			Scheduler scheduler;
			try {
				scheduler = new ClockScheduler(agent, agentFactory);
				schedulers.put(agent.getId(), scheduler);
				return scheduler;
			} catch (Exception e) {
				e.printStackTrace();
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
	static final Logger	logger	= Logger.getLogger("ClockScheduler");
	final Agent			myAgent;
	final Clock			myClock;
	
	public ClockScheduler(Agent myAgent, AgentFactory factory)
			throws Exception {
		this.myAgent=myAgent;
		myClock = new RunnableClock();
		run();
	}
	
	@SuppressWarnings("unchecked")
	public TaskEntry getFirstTask() {
		TreeSet<TaskEntry> timeline = (TreeSet<TaskEntry>) myAgent.getState()
				.get("_taskList");
		if (timeline != null && !timeline.isEmpty()) {
			TaskEntry task = timeline.first();
			while (task != null && task.active) {
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
		Set<TaskEntry> oldTimeline = (TreeSet<TaskEntry>) myAgent
				.getState().get("_taskList");
		Set<TaskEntry> timeline = null;
		boolean found = false;
		if (oldTimeline != null) {
			timeline = new TreeSet<TaskEntry>();
			TaskEntry[] arr = oldTimeline.toArray(new TaskEntry[0]);
			for (TaskEntry entry : arr) {
				if (!entry.taskId.equals(task.taskId)) {
					timeline.add(entry);
				} else {
					found = true;
					timeline.add(task);
				}
			}
		}
		if (!found && !onlyIfExists) {
			if (timeline == null) timeline = new TreeSet<TaskEntry>();
			timeline.add(task);
		}
		if (!myAgent.getState().putIfUnchanged("_taskList", (Serializable)timeline,
				(Serializable) oldTimeline)) {
			logger.severe("need to retry putTask...");
			putTask(task, onlyIfExists); // recursive retry....
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
				if (!entry.taskId.equals(id)) {
					timeline.add(entry);
				}
			}
		}
		if (!myAgent.getState().putIfUnchanged("_taskList", timeline,
				oldTimeline)) {
			logger.severe("need to retry cancelTask...");
			cancelTask(id); // recursive retry....
			return;
		}
	}
	
	public void runTask(final TaskEntry task) {
		// logger.warning("Running "+task.taskId+" at "+DateTime.now().toString()+" Due: "+task.due.toString());
		if (task.interval <= 0) {
			cancelTask(task.taskId); // Remove from list
		} else {
			if (!task.sequential) {
				task.due = DateTime.now().plus(task.interval);
			} else {
				task.active = true;
			}
			putTask(task, true);
		}
		myClock.runInPool(new Runnable() {
			@Override
			public void run() {
				try {
					
					RequestParams params = new RequestParams();
					String senderUrl = "local://" + myAgent.getId();
					params.put(Sender.class, senderUrl);
					
					myAgent.getAgentFactory().receive(myAgent.getId(),
							task.request, params);
					
					if (task.interval > 0 && task.sequential) {
						task.due = DateTime.now().plus(task.interval);
						task.active = false;
						putTask(task, true);
					}
				} catch (Exception e) {
					e.printStackTrace();
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
		return task.taskId;
	}
	
	@SuppressWarnings("unchecked")
	@Override
	public Set<String> getTasks() {
		Set<String> result = new HashSet<String>();
		TreeSet<TaskEntry> timeline = (TreeSet<TaskEntry>) myAgent.getState()
				.get("_taskList");
		if (timeline == null) return result;
		for (TaskEntry entry : timeline) {
			result.add(entry.taskId);
		}
		return result;
	}
	
	@Override
	public void run() {
		TaskEntry task = getFirstTask();
		if (task != null) {
			if (task.due.isBeforeNow()) {
				runTask(task);
				run();// recursive call next task
				return;
			}
			myClock.requestTrigger(myAgent.getId(), task.due, this);
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
	private static final long	serialVersionUID	= -2402975617148459433L;
	// TODO, make JSONRequest.equals() state something about real equal tasks,
	// use it as deduplication!
	String						taskId;
	JSONRequest					request;
	DateTime					due;
	long						interval			= 0;
	boolean						sequential			= true;
	boolean						active				= false;
	
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
		if (this == o) return true;
		if (!(o instanceof TaskEntry)) return false;
		TaskEntry other = (TaskEntry) o;
		return taskId.equals(other.taskId);
	}
	
	@Override
	public int hashCode() {
		return taskId.hashCode();
	}
	
	@Override
	public int compareTo(TaskEntry o) {
		if (equals(o)) return 0;
		if (due.equals(o.due)) return 0;
		return due.compareTo(o.due);
	}
	
	public String getTaskId() {
		return taskId;
	}
	
	public JSONRequest getRequest() {
		return request;
	}
	
	public String getDue() {
		return due.toString();
	}
	
	@Override
	public String toString() {
		try {
			return JOM.getInstance().writeValueAsString(this);
		} catch (Exception e) {
			e.printStackTrace();
			return "{\"taskId\":" + taskId + ",\"due\":" + due + "}";
		}
	}
}
