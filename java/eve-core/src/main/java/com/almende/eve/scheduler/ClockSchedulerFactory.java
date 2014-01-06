package com.almende.eve.scheduler;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.almende.eve.agent.AgentHost;
import com.almende.eve.agent.AgentInterface;

public class ClockSchedulerFactory implements SchedulerFactory {
	private static final Logger		LOG			= Logger.getLogger(ClockSchedulerFactory.class
														.getCanonicalName());
	private Map<String, Scheduler>	schedulers	= new HashMap<String, Scheduler>();
	private AgentHost				host		= null;
	
	/**
	 * This constructor is called when constructed by the AgentHost
	 * 
	 * @param AgentHostDefImpl
	 * @param params
	 */
	public ClockSchedulerFactory(AgentHost host, Map<String, Object> params) {
		this(host, "");
	}
	
	public ClockSchedulerFactory(AgentHost host, String id) {
		this.host = host;
	}
	
	@Override
	public Scheduler getScheduler(AgentInterface agent) {
		ClockScheduler scheduler = null;
		synchronized (schedulers) {
			if (schedulers.containsKey(agent.getId())) {
				scheduler = (ClockScheduler) schedulers.get(agent.getId());
			} else {
				try {
					scheduler = new ClockScheduler(agent, host);
					schedulers.put(agent.getId(), scheduler);
				} catch (Exception e) {
					LOG.log(Level.SEVERE, "Couldn't init new scheduler", e);
				}
			}
		}
		if (scheduler != null) {
			scheduler.run();
			return scheduler;
		}
		return null;
	}
	
	@Override
	public void destroyScheduler(String agentId) {
		synchronized (schedulers) {
			schedulers.remove(agentId);
		}
	}
}
