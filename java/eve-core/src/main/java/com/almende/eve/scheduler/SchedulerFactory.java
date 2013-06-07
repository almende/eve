package com.almende.eve.scheduler;

import com.almende.eve.agent.Agent;

public interface SchedulerFactory {
	Scheduler getScheduler(Agent agent);
	void destroyScheduler(String agentId);
}
