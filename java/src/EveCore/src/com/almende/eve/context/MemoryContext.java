package com.almende.eve.context;

import java.util.HashMap;
import java.util.Map;

import com.almende.eve.config.Config;
import com.almende.eve.scheduler.RunnableScheduler;
import com.almende.eve.scheduler.Scheduler;

public class MemoryContext implements Context {
	private Config config = null;
	private String environment = null;
	private String servletUrl = null;
	private String agentUrl = null;
	private String agentClass = null;
	private String agentId = null;
	
	private Map<String, Object> properties = new HashMap<String, Object>();
	
	private Scheduler scheduler = new RunnableScheduler();
	
	public MemoryContext() {}

	public MemoryContext(String environment, String servletUrl, 
			String agentClass, String agentId, Config config) {
		this.environment = environment;
		this.servletUrl = servletUrl;
		this.agentId = agentId;
		this.agentClass = agentClass;
		this.config = config;

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
	public String getServletUrl() {
		return servletUrl;
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
						agentUrl += agentId;
					}
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

	@Override
	public Config getConfig() {
		return config;
	}
	
	@Override
	public String getEnvironment() {
		return environment;
	}
}
