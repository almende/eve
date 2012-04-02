package com.almende.eve.context;

import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import com.almende.eve.scheduler.RunnableScheduler;
import com.almende.eve.scheduler.Scheduler;

public class MemoryContext implements AgentContext {	
	// properties for a single context
	private String agentClass = null;
	private String id = null;
	private Map<String, Object> properties = new HashMap<String, Object>();
	private Scheduler scheduler = new RunnableScheduler();

	// Singleton containing all contexts, stored in a Map[agentClass][id]
	private static String servletUrl = null;
	private static Map<String, Map<String, MemoryContext>> contexts = 
		new HashMap<String, Map<String, MemoryContext>>();
	
	public MemoryContext() {}
	public MemoryContext(String agentClass, String id) {
		this.id = id;
		this.agentClass = agentClass;
	}
	
	@Override
	public synchronized String getId() {
		return id;
	}

	@Override
	public synchronized void setServletUrl (HttpServletRequest req) {
		// TODO: reckon with https
		servletUrl = "http://" + req.getServerName() + ":" + req.getServerPort() + 
			req.getContextPath() + req.getServletPath() + "/";
	}
	
	@Override
	public synchronized String getAgentUrl() {
		String agentUrl = null;
		if (servletUrl != null) {
			agentUrl = servletUrl;
			if (agentClass != null) {
				agentUrl += agentClass;
				if (id != null) {
					agentUrl += "/" + id;
				}
			}
		}
		
		return agentUrl;
	}
	
	@Override
	public synchronized Object get(String key) {
		if (properties == null) {
			return null;
		}
		return properties.get(key);
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
	public AgentContext getInstance(String agentClass, String id) {
		MemoryContext context = null;

		if (agentClass != null) {
			// get map with the current agentClass
			Map<String, MemoryContext> classContexts = contexts.get(agentClass);
			if (classContexts == null) {
				classContexts = new HashMap<String, MemoryContext>();
				contexts.put(agentClass, classContexts);
			}
	
			// get map with the current id
			if (id != null && classContexts != null) {
				context = classContexts.get(id);
				if (context == null) {
					context = new MemoryContext(agentClass, id);
					classContexts.put(id, context);
				}
			}
		}
		
		return context;
	}
}
