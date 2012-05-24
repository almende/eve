package com.almende.eve.context;

import com.almende.eve.scheduler.Scheduler;
import com.almende.eve.config.Config;

public interface Context {
	// servlet info
	public String getEnvironment();
	public String getServletUrl();	
	public Config getConfig();  // TODO: config should be read only
	
	// getting info about the agent
	public String getAgentId();
	public String getAgentClass();
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
