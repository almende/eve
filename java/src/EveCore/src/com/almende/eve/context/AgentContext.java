package com.almende.eve.context;

import javax.servlet.http.HttpServletRequest;

import com.almende.eve.scheduler.Scheduler;

public interface AgentContext {
	// constructor
	public AgentContext getInstance(String agentClass, String id);

	// getting info about the agent
	public void setServletUrl(HttpServletRequest req);
	public String getId();
	public String getAgentUrl();

	// key/value properties
	public <T> T get(String key);
	public void put(String key, Object value);
	public boolean has(String key);
	public void remove(String key);
	
	// scheduler
	public Scheduler getScheduler();
}
