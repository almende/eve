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
import com.almende.eve.clock.Clock;
import com.almende.eve.clock.RunnableClock;
import com.almende.eve.rpc.RequestParams;
import com.almende.eve.rpc.jsonrpc.JSONRequest;
import com.almende.eve.rpc.jsonrpc.jackson.JOM;

public class ClockSchedulerFactory implements SchedulerFactory {
	Map<String, Scheduler> schedulers = new HashMap<String, Scheduler>();
	AgentFactory agentFactory = null;

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
	public Scheduler getScheduler(String agentId) {
		synchronized (schedulers) {
			if (schedulers.containsKey(agentId))
				return schedulers.get(agentId);
			Scheduler scheduler;
			try {
				scheduler = new ClockScheduler(agentId, agentFactory);
				schedulers.put(agentId, scheduler);
				return scheduler;
			} catch (Exception e) {
				e.printStackTrace();
			}
			return null;
		}
	}
	
	@Override
	public void destroyScheduler(String agentId){
		synchronized(schedulers){
			schedulers.remove(agentId);
		}
	}
}

class ClockScheduler implements Scheduler, Runnable {

	final Agent myAgent;
	final Clock myClock;

	public ClockScheduler(String agentId, AgentFactory factory)
			throws Exception {
		myAgent = factory.getAgent(agentId);
		if (myAgent == null) {
			Logger.getLogger("ClockScheduler").severe(
					"Error: Agent not found:" + agentId);
			throw new Exception("Trying to getScheduler() on a not existing agent:"+agentId);
		}
		myClock = new RunnableClock();
		TaskEntry task = getFirstTask(false);
		if (task != null)
			myClock.requestTrigger(agentId, task.due, this);
	}

	@SuppressWarnings("unchecked")
	public TaskEntry getFirstTask(boolean remove) {
		TreeSet<TaskEntry> timeline = (TreeSet<TaskEntry>) myAgent.getState()
				.get("_taskList");
		if (timeline != null && !timeline.isEmpty()) {
			TaskEntry task = timeline.first();
			if (remove) {
				TreeSet<TaskEntry> newTimeline = (TreeSet<TaskEntry>) timeline
						.clone();
				newTimeline.remove(task);
				if (!myAgent.getState().putIfUnchanged("_taskList",
						newTimeline, timeline)) {
					return getFirstTask(remove); // recursive retry......
				}
			}
			return task;
		}
		return null;
	}

	@SuppressWarnings("unchecked")
	public void putTask(TaskEntry task) {
		TreeSet<TaskEntry> oldTimeline = (TreeSet<TaskEntry>) myAgent
				.getState().get("_taskList");
		TreeSet<TaskEntry> timeline;
		if (oldTimeline == null) {
			timeline = new TreeSet<TaskEntry>();
		} else {
			timeline = (TreeSet<TaskEntry>) oldTimeline.clone();
		}
		timeline.add(task);
		if (!myAgent.getState().putIfUnchanged("_taskList", timeline,
				oldTimeline)) {
			putTask(task); // recursive retry....
			return;
		}
	}

	public void runTask(final TaskEntry task) {
		myClock.runInPool(new Runnable() {
			@Override
			public void run() {
				try {
					RequestParams params = new RequestParams();
					String senderUrl = "local://" + myAgent.getId();
					params.put(Sender.class,senderUrl);

					myAgent.getAgentFactory().receive(myAgent.getId(),
							task.request, params);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}

		});
	}

	@Override
	public String createTask(JSONRequest request, long delay) {
		TaskEntry task = new TaskEntry(DateTime.now().plus(delay), request);
		if (delay <= 0) {
			runTask(task);
		} else {
			putTask(task);
			TaskEntry first = getFirstTask(false);
			if (first != null)
				myClock.requestTrigger(myAgent.getId(), first.due, this);
		}
		return task.taskId;
	}

	@SuppressWarnings("unchecked")
	@Override
	public void cancelTask(String id) {
		TreeSet<TaskEntry> oldTimeline = (TreeSet<TaskEntry>) myAgent
				.getState().get("_taskList");
		TreeSet<TaskEntry> timeline;
		if (oldTimeline == null) {
			timeline = new TreeSet<TaskEntry>();
		} else {
			timeline = (TreeSet<TaskEntry>) oldTimeline.clone();
		}
		TaskEntry[] arr = timeline.toArray(new TaskEntry[0]);
		timeline.clear();
		for (TaskEntry entry : arr) {
			if (!entry.taskId.equals(id)) {
				timeline.add(entry);
			}
		}
		if (!myAgent.getState().putIfUnchanged("_taskList", timeline,
				oldTimeline)) {
			cancelTask(id); // recursive retry....
			return;
		}
	}

	@SuppressWarnings("unchecked")
	@Override
	public Set<String> getTasks() {
		Set<String> result = new HashSet<String>();
		TreeSet<TaskEntry> timeline = (TreeSet<TaskEntry>) myAgent
				.getState().get("_taskList");
		if (timeline == null) return result;
		for (TaskEntry entry : timeline){
			result.add(entry.taskId);
		}
		return result;
	}

	@Override
	public void run() {
		TaskEntry task = getFirstTask(true);
		if (task != null) {
			if (task.due.isBeforeNow()) {
				runTask(task);
				run();// recursive call next task
				return;
			} else {
				putTask(task);
			}
			myClock.requestTrigger(myAgent.getId(), task.due, this);
		}
	}

	@Override
	public String toString() {
		@SuppressWarnings("unchecked")
		TreeSet<TaskEntry> timeline = (TreeSet<TaskEntry>) myAgent
				.getState().get("_taskList");
		return timeline.toString();
	}
}

class TaskEntry implements Comparable<TaskEntry>, Serializable {
	private static final long serialVersionUID = -2402975617148459433L;
	// TODO, make JSONRequest.equals() state something about real equal tasks,
	// use it as deduplication!
	String taskId;
	JSONRequest request;
	DateTime due;

	public TaskEntry(DateTime due, JSONRequest request) {
		taskId = UUID.randomUUID().toString();
		this.request = request;
		this.due = due;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o)
			return true;
		if (!(o instanceof TaskEntry))
			return false;
		TaskEntry other = (TaskEntry) o;
		return taskId.equals(other.taskId);
	}

	@Override
	public int hashCode() {
		return taskId.hashCode();
	}

	@Override
	public int compareTo(TaskEntry o) {
		if (due.equals(o.due))
			return 0;
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
	public String toString(){
		try {
			return JOM.getInstance().writeValueAsString(this);
		} catch (Exception e) {
			e.printStackTrace();
			return "{\"taskId\":"+taskId+",\"due\":"+due+"}";
		}
	}
}
