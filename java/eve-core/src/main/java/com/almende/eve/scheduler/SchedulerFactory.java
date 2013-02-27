package com.almende.eve.scheduler;

public interface SchedulerFactory {
	public abstract Scheduler getScheduler(String agentId);
	void destroyScheduler(String agentId);
}
