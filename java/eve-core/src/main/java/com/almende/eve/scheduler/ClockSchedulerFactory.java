package com.almende.eve.scheduler;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.almende.eve.agent.Agent;
import com.almende.eve.agent.AgentHost;

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
	public ClockSchedulerFactory(AgentHost agentHost, Map<String, Object> params) {
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

