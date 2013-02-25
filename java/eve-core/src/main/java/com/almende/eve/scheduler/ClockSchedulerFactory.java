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

public class ClockSchedulerFactory implements SchedulerFactory {
	Map<String,Scheduler> schedulers = new HashMap<String,Scheduler>();
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
		synchronized(schedulers){
			if (schedulers.containsKey(agentId)) return schedulers.get(agentId);
			Scheduler scheduler;
			try {
				scheduler = new ClockScheduler(agentId,agentFactory);
				schedulers.put(agentId, scheduler);
				return scheduler;
			} catch (Exception e) {
				e.printStackTrace();
			}
			return null;
		}
	}
}
class ClockScheduler implements Scheduler,Runnable {

	final Agent myAgent;
	final Clock myClock;
	
	public ClockScheduler(String agentId, AgentFactory factory) throws Exception {
		myAgent = factory.getAgent(agentId);
		if (myAgent == null) {
			Logger.getLogger("ClockScheduler").severe("Error: Agent not found:"+agentId);
		}
		myClock = new RunnableClock();
		TaskEntry task = getFirstTask(false);
		if (task != null) myClock.requestTrigger(agentId, task.due, this);
	}

	@SuppressWarnings("unchecked")
	public TaskEntry getFirstTask(boolean remove){
		TreeSet<TaskEntry> timeline = (TreeSet<TaskEntry>) myAgent.getState().get("_taskList");
		if (timeline != null && !timeline.isEmpty()){
			TaskEntry task = timeline.first();
			if (remove){
				TreeSet<TaskEntry> newTimeline = (TreeSet<TaskEntry>) timeline.clone();
				newTimeline.remove(task);
				if (!myAgent.getState().putIfUnchanged("_taskList", newTimeline, timeline)){
					return getFirstTask(true); //recursive retry......
				}
			}
			return task;
		}
		return null;
	}
	@SuppressWarnings("unchecked")
	public void putTask(TaskEntry task){
		TreeSet<TaskEntry> oldTimeline = (TreeSet<TaskEntry>) myAgent.getState().get("_taskList");
		TreeSet<TaskEntry> timeline;
		if (oldTimeline == null){
			timeline = new TreeSet<TaskEntry>();
			myAgent.getState().put("_taskList", timeline);
			oldTimeline = (TreeSet<TaskEntry>) timeline.clone();
		} else {
			timeline = (TreeSet<TaskEntry>) oldTimeline.clone();	
		}
		timeline.add(task);
		if (!myAgent.getState().putIfUnchanged("_taskList", timeline, oldTimeline)){
			putTask(task); //recursive retry....
		}
	}

	public void runTask(final TaskEntry task){
		myClock.requestTrigger(myAgent.getId(), DateTime.now(), new Runnable(){
			@Override
			public void run() {
				try {
					RequestParams params = new RequestParams();
					params.put(Sender.class, null); // TODO: provide itself

					myAgent.getAgentFactory().invoke(myAgent.getId(), task.request, params);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
			
		});
	}
	
	@Override
	public String createTask(JSONRequest request, long delay) {
		TaskEntry task =new TaskEntry(DateTime.now(),request); 
		if (delay <= 0) {
			runTask(task);
		} else {
			putTask(task);
			TaskEntry first = getFirstTask(false);
			if (first != null) myClock.requestTrigger(myAgent.getId(), first.due, this);
		}
		return task.taskId;
	}

	@Override
	public void cancelTask(String id) {
		//TODO
		
	}

	@Override
	public Set<String> getTasks() {
		// TODO: 
		/*
		 * Return taskslist
		 */
		return new HashSet<String>();
	}
	
	@Override
	public void run(){
		TaskEntry task = getFirstTask(true);
		if (task != null){
			if (task.due.isBeforeNow()){
				runTask(task);
				run();
				return;
			} else {
				putTask(task);
			}
			TaskEntry first = getFirstTask(false);
			if (first != null) myClock.requestTrigger(myAgent.getId(), first.due, this);
		}
	}
}
class TaskEntry implements Comparable<TaskEntry>,Serializable{
	private static final long serialVersionUID = -2402975617148459433L;
	//TODO, make JSONRequest.equals() state something about real equal tasks, use it as deduplication!
	String taskId;
	JSONRequest request;
	DateTime due;
	
	public TaskEntry(DateTime due,JSONRequest request){
		taskId = UUID.randomUUID().toString();
		this.request=request;
		this.due=due;
	}
	
	@Override
	public boolean equals(Object o){
		if ( this == o ) return true;
		if ( !(o instanceof TaskEntry) ) return false;
		TaskEntry other = (TaskEntry) o;
		return taskId.equals(other.taskId);
	}
	@Override
	public int hashCode(){
		return taskId.hashCode();
	}
	
	@Override
	public int compareTo(TaskEntry o) {
		if (due.equals(o.due)) return 0;
		return due.compareTo(o.due);		}
}
