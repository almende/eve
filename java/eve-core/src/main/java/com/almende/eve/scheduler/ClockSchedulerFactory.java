/*
 * Copyright: Almende B.V. (2014), Rotterdam, The Netherlands
 * License: The Apache Software License, Version 2.0
 */
package com.almende.eve.scheduler;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.almende.eve.agent.AgentHost;
import com.almende.eve.agent.AgentInterface;

/**
 * A factory for creating ClockScheduler objects.
 */
public class ClockSchedulerFactory implements SchedulerFactory {
	private static final Logger				LOG			= Logger.getLogger(ClockSchedulerFactory.class
																.getCanonicalName());
	private final Map<String, Scheduler>	schedulers	= new HashMap<String, Scheduler>();
	private AgentHost						host		= null;
	
	/**
	 * This constructor is called when constructed by the AgentHost.
	 * 
	 * @param host
	 *            the host
	 * @param params
	 *            the params
	 */
	public ClockSchedulerFactory(final AgentHost host,
			final Map<String, Object> params) {
		this(host, "");
	}
	
	/**
	 * Instantiates a new clock scheduler factory.
	 * 
	 * @param host
	 *            the host
	 * @param id
	 *            the id
	 */
	public ClockSchedulerFactory(final AgentHost host, final String id) {
		this.host = host;
	}
	
	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * com.almende.eve.scheduler.SchedulerFactory#getScheduler(com.almende.eve
	 * .agent.AgentInterface)
	 */
	@Override
	public Scheduler getScheduler(final AgentInterface agent) {
		ClockScheduler scheduler = null;
		synchronized (schedulers) {
			if (schedulers.containsKey(agent.getId())) {
				scheduler = (ClockScheduler) schedulers.get(agent.getId());
			} else {
				try {
					scheduler = new ClockScheduler(agent, host);
					schedulers.put(agent.getId(), scheduler);
				} catch (final Exception e) {
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
	
	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * com.almende.eve.scheduler.SchedulerFactory#destroyScheduler(java.lang
	 * .String)
	 */
	@Override
	public void destroyScheduler(final String agentId) {
		synchronized (schedulers) {
			schedulers.remove(agentId);
		}
	}
}
