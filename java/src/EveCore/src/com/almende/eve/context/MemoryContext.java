package com.almende.eve.context;

import java.util.HashMap;
import java.util.Map;

import com.almende.eve.scheduler.RunnableScheduler;
import com.almende.eve.scheduler.Scheduler;

public class MemoryContext implements Context {	
	private String agentClass = null;
	private String id = null;
	private String servletUrl = null;
	
	private Map<String, Object> properties = new HashMap<String, Object>();
	
	private Scheduler scheduler = new RunnableScheduler();
	
	public MemoryContext() {}
	public MemoryContext(String agentClass, String id, String servletUrl) {
		this.id = id;
		this.agentClass = agentClass;
		this.servletUrl = servletUrl;
	}
	
	@Override
	public synchronized String getId() {
		return id;
	}

	@Override
	public String getAgentUrl() {
		String agentUrl = null;
		if (servletUrl != null) {
			agentUrl = servletUrl;
			if (agentClass != null) {
				agentUrl += agentClass + "/";
				if (id != null) {
					agentUrl += id;
				}
			}
		}
		
		return agentUrl;
	}
	
	@SuppressWarnings("unchecked")
	@Override
	public synchronized <T> T get(String key) {
		if (properties == null) {
			return null;
		}
		return (T) properties.get(key);
	}

	@Override
	public synchronized void put(String key, Object value) {
		if (properties == null) {
			return;
		}		
		properties.put(key, value);
	}

	@Override
	public synchronized boolean has(String key) {
		if (properties == null) {
			return false;
		}		
		return properties.containsKey(key);
	}

	@Override
	public synchronized void remove(String key) {
		if (properties != null) {
			properties.remove(key);
		}	
	}
	
	@Override
	public Scheduler getScheduler() {
		return scheduler;
	}

	@Override
	public void beginTransaction() {
		// TODO: transaction
	}

	@Override
	public void commitTransaction() {
		// TODO: transaction
	}
	
	@Override
	public void rollbackTransaction() {
		// TODO: transaction
	}
}
