package com.almende.eve.context;

import java.util.Map;

import com.almende.eve.scheduler.Scheduler;
import com.almende.eve.config.Config;

public interface Context extends Map<String, Object> {
	// servlet info
	public String getEnvironment();
	public String getServletUrl();	
	public Config getConfig();  // TODO: config should be read only
	
	// info about the agent
	public String getAgentId();
	public String getAgentClass();
	public String getAgentUrl();

	/* TODO: what more methods to take over from Map?
	// key/value properties
	public <T> T get(String key, Class<T> type);
	public boolean put(String key, Object value);
	public boolean has(String key);
	public void remove(String key);
	public void clear();
	public Set<String> keySet();
	public boolean containsKey(String key);  -> no, use has(String key)
	public boolean containsValue(Object value);
	public Set<String> entrySet();
	public boolean isEmpty();
	public void putAll(Map<String, Object> values);
	public int size();
	public Collection values();
	*/
	
	// scheduler
	public Scheduler getScheduler();	
}
