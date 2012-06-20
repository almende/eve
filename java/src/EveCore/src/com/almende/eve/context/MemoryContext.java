package com.almende.eve.context;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

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

	/* TODO: cleanup
	@SuppressWarnings("unchecked")
	@Override
	public synchronized <T> T get(String key, Class<T> type) {
		return (T) properties.get(key);
	}

	@Override
	public synchronized boolean put(String key, Object value) {
		properties.put(key, value);
		return true;
	}

	@Override
	public synchronized boolean has(String key) {
		return properties.containsKey(key);
	}

	@Override
	public synchronized void remove(String key) {
		properties.remove(key);
	}
	*/
	
	@Override
	public synchronized Scheduler getScheduler() {
		return scheduler;
	}

	@Override
	public synchronized String getServletUrl() {
		return factory.getServletUrl();
	}

	@Override
	public synchronized Config getConfig() {
		return factory.getConfig();
	}
	
	@Override
	public synchronized String getEnvironment() {
		return factory.getEnvironment();
	}

	@Override
	public synchronized void clear() {
		properties.clear();
	}

	@Override
	public synchronized Set<String> keySet() {
		return properties.keySet();
	}

	@Override
	public synchronized boolean containsKey(Object key) {
		return properties.containsKey(key);
	}

	@Override
	public synchronized boolean containsValue(Object value) {
		return properties.containsValue(value);
	}

	@Override
	public synchronized Set<java.util.Map.Entry<String, Object>> entrySet() {
		return properties.entrySet();
	}

	@Override
	public synchronized Object get(Object key) {
		return properties.get(key);
	}

	@Override
	public synchronized boolean isEmpty() {
		return properties.isEmpty();
	}

	@Override
	public synchronized Object put(String key, Object value) {
		return properties.put(key, value);
	}

	@Override
	public synchronized void putAll(Map<? extends String, ? extends Object> map) {
		properties.putAll(map);
	}

	@Override
	public synchronized Object remove(Object key) {
		return properties.remove(key);
	}

	@Override
	public synchronized int size() {
		return properties.size();
	}

	@Override
	public synchronized Collection<Object> values() {
		return properties.values();
	}
}
