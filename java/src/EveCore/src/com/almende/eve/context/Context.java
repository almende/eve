package com.almende.eve.context;

import com.almende.eve.scheduler.Scheduler;

public interface Context {
	// getting info about the agent
	public String getId();
	public String getAgentUrl();

	// key/value properties
	public <T> T get(String key);
	public void put(String key, Object value);
	public boolean has(String key);
	public void remove(String key);
	
	// transactional changes
	public void beginTransaction();
	public void commitTransaction();
	public void rollbackTransaction();
	
	// scheduler
	public Scheduler getScheduler();
}
