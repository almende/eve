package com.almende.eve.context;

import com.almende.eve.scheduler.Scheduler;
import com.almende.eve.config.Config;

public interface Context {
	// servlet info
	public String getEnvironment();
	public String getServletUrl();	
	public Config getConfig();  // TODO: config should be read only
	
	// info about the agent
	public String getAgentId();
	public String getAgentClass();
	public String getAgentUrl();

	// key/value properties
	public <T> T get(String key, Class<T> type);
	public boolean put(String key, Object value);
	public boolean has(String key);
	public void remove(String key);
	
	// scheduler
	public Scheduler getScheduler();	
}
