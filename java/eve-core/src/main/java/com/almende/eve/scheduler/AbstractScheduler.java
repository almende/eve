package com.almende.eve.scheduler;

public abstract class AbstractScheduler implements Scheduler {
	@Override
	public void cancelAllTasks() {
		for (final String id : getTasks()) {
			cancelTask(id);
		}
	}
	
}
