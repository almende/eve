/*
 * 
 */
package com.almende.eve.scheduler;

import java.io.Serializable;
import java.net.URI;
import java.util.HashSet;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.joda.time.DateTime;

import com.almende.eve.agent.AgentHost;
import com.almende.eve.agent.AgentInterface;
import com.almende.eve.rpc.jsonrpc.JSONRequest;
import com.almende.eve.rpc.jsonrpc.jackson.JOM;
import com.almende.eve.scheduler.clock.Clock;
import com.almende.eve.scheduler.clock.RunnableClock;
import com.almende.eve.state.TypedKey;
import com.almende.util.uuid.UUID;
import com.fasterxml.jackson.annotation.JsonIgnore;

/**
 * The Class ClockScheduler.
 */
public class ClockScheduler extends AbstractScheduler implements Runnable {
	private static final Logger									LOG			= Logger.getLogger("ClockScheduler");
	private final AgentInterface								myAgent;
	private final Clock											myClock;
	private final ClockScheduler								_this		= this;
	private static final TypedKey<TreeMap<String, TaskEntry>>	TYPEDKEY	= new TypedKey<TreeMap<String, TaskEntry>>(
																					"_taskList") {
																			};
	private static final int									MAXCOUNT	= 100;
	
	/**
	 * Instantiates a new clock scheduler.
	 * 
	 * @param myAgent
	 *            the my agent
	 * @param host
	 *            the host
	 */
	public ClockScheduler(final AgentInterface myAgent, final AgentHost host) {
		if (myAgent == null) {
			throw new IllegalArgumentException("MyAgent should not be null!");
		}
		this.myAgent = myAgent;
		myClock = new RunnableClock();
	}
	
	/**
	 * Gets the first task.
	 * 
	 * @return the first task
	 */
	public TaskEntry getFirstTask() {
		if (myAgent.getState() == null) {
			return null;
		}
		final TreeMap<String, TaskEntry> timeline = myAgent.getState().get(
				TYPEDKEY);
		if (timeline != null && !timeline.isEmpty()) {
			TaskEntry task = timeline.firstEntry().getValue();
			int count = 0;
			while (task != null && task.isActive() && count < MAXCOUNT) {
				count++;
				final Entry<String, TaskEntry> entry = timeline
						.higherEntry(task.getTaskId());
				task = null;
				if (entry != null) {
					task = entry.getValue();
				}
			}
			if (count >= MAXCOUNT) {
				LOG.warning("Oops: more than 100 tasks active at the same time:"
						+ myAgent.getId()
						+ " : "
						+ timeline.size()
						+ "/"
						+ count);
			} else {
				return task;
			}
		}
		return null;
	}
	
	/**
	 * Put task.
	 * 
	 * @param task
	 *            the task
	 */
	public void putTask(final TaskEntry task) {
		putTask(task, false);
	}
	
	/**
	 * Put task.
	 * 
	 * @param task
	 *            the task
	 * @param onlyIfExists
	 *            the only if exists
	 */
	public void putTask(final TaskEntry task, final boolean onlyIfExists) {
		if (task == null || myAgent.getState() == null) {
			LOG.warning("Trying to save task to non-existing state or task is null");
			return;
		}
		final TreeMap<String, TaskEntry> oldTimeline = myAgent.getState().get(
				TYPEDKEY);
		TreeMap<String, TaskEntry> timeline = null;
		
		if (oldTimeline != null) {
			timeline = new TreeMap<String, TaskEntry>(oldTimeline);
		} else {
			timeline = new TreeMap<String, TaskEntry>();
		}
		
		if (onlyIfExists) {
			if (timeline.containsKey(task.getTaskId())) {
				timeline.put(task.getTaskId(), task);
			}
		} else {
			timeline.put(task.getTaskId(), task);
		}
		
		if (!myAgent.getState().putIfUnchanged(TYPEDKEY.getKey(), timeline,
				oldTimeline)) {
			// recursive retry....
			putTask(task, onlyIfExists);
			return;
		}
	}
	
	/*
	 * (non-Javadoc)
	 * 
	 * @see com.almende.eve.scheduler.Scheduler#cancelTask(java.lang.String)
	 */
	@Override
	public void cancelTask(final String id) {
		if (myAgent.getState() == null) {
			return;
		}
		
		final TreeMap<String, TaskEntry> oldTimeline = myAgent.getState().get(
				TYPEDKEY);
		TreeMap<String, TaskEntry> timeline = null;
		
		if (oldTimeline != null) {
			timeline = new TreeMap<String, TaskEntry>(oldTimeline);
			timeline.remove(id);
		} else {
			timeline = new TreeMap<String, TaskEntry>();
		}
		
		if (timeline != null
				&& !myAgent.getState().putIfUnchanged(TYPEDKEY.getKey(),
						timeline, oldTimeline)) {
			// recursive retry....
			cancelTask(id);
			return;
		}
	}
	
	/**
	 * Run task.
	 * 
	 * @param task
	 *            the task
	 */
	public void runTask(final TaskEntry task) {
		if (task == null || task.isActive()) {
			return;
		}
		task.setActive(true);
		_this.putTask(task, true);
		
		try {
			// TODO: fix sequential calls, needs callback and guaranteed
			// replies, also in the case of void? (This holds for all methods?)
			final String receiverUrl = "local:" + myAgent.getId();
			// Next call is always short/asynchronous
			myAgent.send(task.getRequest(), URI.create(receiverUrl), null, null);
			
			if (task.getInterval() <= 0) {
				// Remove from list
				_this.cancelTask(task.getTaskId());
			} else {
				task.setDue(DateTime.now().plus(task.getInterval()));
				task.setActive(false);
				_this.putTask(task, true);
				_this.run();
			}
		} catch (final Exception e) {
			LOG.log(Level.SEVERE, myAgent.getId()
					+ ": Failed to run scheduled task:" + task.toString(), e);
		}
		
	}
	
	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * com.almende.eve.scheduler.Scheduler#createTask(com.almende.eve.rpc.jsonrpc
	 * .JSONRequest, long)
	 */
	@Override
	public String createTask(final JSONRequest request, final long delay) {
		return createTask(request, delay, false, false);
	}
	
	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * com.almende.eve.scheduler.Scheduler#createTask(com.almende.eve.rpc.jsonrpc
	 * .JSONRequest, long, boolean, boolean)
	 */
	@Override
	public String createTask(final JSONRequest request, final long delay,
			final boolean repeat, final boolean sequential) {
		final TaskEntry task = new TaskEntry(DateTime.now().plus(delay),
				request, (repeat ? delay : 0), sequential);
		putTask(task);
		if (repeat || delay <= 0) {
			runTask(task);
		} else {
			run();
		}
		return task.getTaskId();
	}
	
	/*
	 * (non-Javadoc)
	 * 
	 * @see com.almende.eve.scheduler.Scheduler#getTasks()
	 */
	@Override
	public Set<String> getTasks() {
		if (myAgent.getState() == null) {
			return null;
		}
		
		final Set<String> result = new HashSet<String>();
		final TreeMap<String, TaskEntry> timeline = myAgent.getState().get(
				TYPEDKEY);
		if (timeline == null || timeline.size() == 0) {
			return result;
		}
		return timeline.keySet();
	}
	
	/*
	 * (non-Javadoc)
	 * 
	 * @see com.almende.eve.scheduler.Scheduler#getDetailedTasks()
	 */
	@Override
	public Set<String> getDetailedTasks() {
		if (myAgent.getState() == null) {
			return null;
		}
		
		final Set<String> result = new HashSet<String>();
		final TreeMap<String, TaskEntry> timeline = myAgent.getState().get(
				TYPEDKEY);
		if (timeline == null || timeline.size() == 0) {
			return result;
		}
		for (final TaskEntry entry : timeline.values()) {
			result.add(entry.toString());
		}
		return result;
	}
	
	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Runnable#run()
	 */
	@Override
	public void run() {
		final TaskEntry task = getFirstTask();
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
	
	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		if (myAgent.getState() == null) {
			return null;
		}
		
		final TreeMap<String, TaskEntry> timeline = myAgent.getState().get(
				TYPEDKEY);
		return (timeline != null) ? timeline.toString() : "[]";
	}
}

/**
 * @author Almende
 *
 */
class TaskEntry implements Comparable<TaskEntry>, Serializable {
	private static final Logger	LOG					= Logger.getLogger(TaskEntry.class
															.getCanonicalName());
	private static final long	serialVersionUID	= -2402975617148459433L;
	// TODO, make JSONRequest.equals() state something about real equal tasks,
	// use it as deduplication!
	private String				taskId				= null;
	private JSONRequest			request;
	private DateTime			due;
	private long				interval			= 0;
	private boolean				sequential			= true;
	private boolean				active				= false;
	
	/**
	 * Instantiates a new task entry.
	 */
	public TaskEntry() {
	};
	
	/**
	 * Instantiates a new task entry.
	 * 
	 * @param due
	 *            the due
	 * @param request
	 *            the request
	 * @param interval
	 *            the interval
	 * @param sequential
	 *            the sequential
	 */
	public TaskEntry(final DateTime due, final JSONRequest request,
			final long interval, final boolean sequential) {
		taskId = new UUID().toString();
		this.request = request;
		this.due = due;
		this.interval = interval;
		this.sequential = sequential;
	}
	
	/* (non-Javadoc)
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(final Object o) {
		if (this == o) {
			return true;
		}
		if (!(o instanceof TaskEntry)) {
			return false;
		}
		final TaskEntry other = (TaskEntry) o;
		return taskId.equals(other.taskId);
	}
	
	/* (non-Javadoc)
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
		return taskId.hashCode();
	}
	
	/* (non-Javadoc)
	 * @see java.lang.Comparable#compareTo(java.lang.Object)
	 */
	@Override
	public int compareTo(final TaskEntry o) {
		if (equals(o)) {
			return 0;
		}
		if (due.equals(o.due)) {
			return taskId.compareTo(o.taskId);
		}
		return due.compareTo(o.due);
	}
	
	/**
	 * Gets the task id.
	 * 
	 * @return the task id
	 */
	public String getTaskId() {
		return taskId;
	}
	
	/**
	 * Gets the request.
	 * 
	 * @return the request
	 */
	public JSONRequest getRequest() {
		return request;
	}
	
	/**
	 * Gets the due as string.
	 * 
	 * @return the due as string
	 */
	public String getDueAsString() {
		return due.toString();
	}
	
	/**
	 * Gets the due.
	 * 
	 * @return the due
	 */
	@JsonIgnore
	public DateTime getDue() {
		return due;
	}
	
	/**
	 * Gets the interval.
	 * 
	 * @return the interval
	 */
	public long getInterval() {
		return interval;
	}
	
	/**
	 * Sets the task id.
	 * 
	 * @param taskId
	 *            the new task id
	 */
	public void setTaskId(final String taskId) {
		this.taskId = taskId;
	}
	
	/**
	 * Sets the request.
	 * 
	 * @param request
	 *            the new request
	 */
	public void setRequest(final JSONRequest request) {
		this.request = request;
	}
	
	/**
	 * Sets the due as string.
	 * 
	 * @param due
	 *            the new due as string
	 */
	public void setDueAsString(final String due) {
		this.due = new DateTime(due);
	}
	
	/**
	 * Sets the due.
	 * 
	 * @param due
	 *            the new due
	 */
	public void setDue(final DateTime due) {
		this.due = due;
	}
	
	/**
	 * Sets the interval.
	 * 
	 * @param interval
	 *            the new interval
	 */
	public void setInterval(final long interval) {
		this.interval = interval;
	}
	
	/**
	 * Sets the sequential.
	 * 
	 * @param sequential
	 *            the new sequential
	 */
	public void setSequential(final boolean sequential) {
		this.sequential = sequential;
	}
	
	/**
	 * Sets the active.
	 * 
	 * @param active
	 *            the new active
	 */
	public void setActive(final boolean active) {
		this.active = active;
	}
	
	/**
	 * Checks if is sequential.
	 * 
	 * @return true, if is sequential
	 */
	public boolean isSequential() {
		return sequential;
	}
	
	/**
	 * Checks if is active.
	 * 
	 * @return true, if is active
	 */
	public boolean isActive() {
		return active;
	}
	
	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		try {
			return JOM.getInstance().writeValueAsString(this);
		} catch (final Exception e) {
			LOG.log(Level.WARNING, "Couldn't use Jackson to print task.", e);
			return "{\"taskId\":" + taskId + ",\"due\":" + due + "}";
		}
	}
}
