package com.almende.eve.context;

import java.util.HashMap;
import java.util.Map;

import com.almende.eve.config.Config;
import com.almende.eve.scheduler.RunnableScheduler;
import com.almende.eve.scheduler.Scheduler;

public class MemoryContext implements Context {
	MemoryContextFactory factory = null;
	private String agentUrl = null;
	private String agentClass = null;
	private String agentId = null;
	
	private Map<String, Object> properties = new HashMap<String, Object>();
	
	private Scheduler scheduler = new RunnableScheduler();
	
	public MemoryContext() {}

	public MemoryContext(MemoryContextFactory factory, 
			String agentClass, String agentId) {
		this.factory = factory;
		this.agentId = agentId;
		this.agentClass = agentClass;
		// Note: agentUrl will be initialized when needed
	}
	
	@Override
	public synchronized String getAgentId() {
		return agentId;
	}
	
	@Override
	public synchronized String getAgentClass() {
		return agentClass;
	}

	@Override
	public String getAgentUrl() {
		if (agentUrl == null) {
			String servletUrl = getServletUrl();
			if (servletUrl != null) {
				agentUrl = servletUrl;
				if (!agentUrl.endsWith("/")) {
					agentUrl += "/";
				}
				if (agentClass != null) {
					agentUrl += agentClass + "/";
					if (agentId != null) {
						agentUrl += agentId + "/";
					}
				}
			}			
		}
		return agentUrl;
	}
	
	@SuppressWarnings("unchecked")
	@Override
	public synchronized <T> T get(String key, Class<T> type) {
		if (properties == null) {
			return null;
		}
		return (T) properties.get(key);
	}

	@Override
	public synchronized boolean put(String key, Object value) {
		if (properties == null) {
			return false;
		}		

		properties.put(key, value);
		return true;
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
	public String getServletUrl() {
		return factory.getServletUrl();
	}

	@Override
	public Config getConfig() {
		return factory.getConfig();
	}
	
	@Override
	public String getEnvironment() {
		return factory.getEnvironment();
	}
}
