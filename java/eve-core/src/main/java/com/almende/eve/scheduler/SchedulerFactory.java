package com.almende.eve.scheduler;

import com.almende.eve.agent.AgentInterface;

public interface SchedulerFactory {
	Scheduler getScheduler(AgentInterface agent);
	void destroyScheduler(String agentId);
}
