/*
 * Copyright: Almende B.V. (2014), Rotterdam, The Netherlands
 * License: The Apache Software License, Version 2.0
 */
package com.almende.eve.scheduler;

import com.almende.eve.agent.AgentInterface;

/**
 * A factory for creating Scheduler objects.
 */
public interface SchedulerFactory {
	
	/**
	 * Gets the scheduler.
	 * 
	 * @param agent
	 *            the agent
	 * @return the scheduler
	 */
	Scheduler getScheduler(AgentInterface agent);
	
	/**
	 * Destroy scheduler.
	 * 
	 * @param agentId
	 *            the agent id
	 */
	void destroyScheduler(String agentId);
}
