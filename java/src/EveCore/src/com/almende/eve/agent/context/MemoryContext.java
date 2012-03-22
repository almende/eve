package com.almende.eve.agent.context;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

/**
 * TemporaryContext
 * 
 * this class is thread safe
 */
@SuppressWarnings("serial")
public class MemoryContext implements AgentContext, Serializable {
	private String servletUrl = null;
	private String agentClass = null;
	private String id = null;

	private static Map<String, Object> nullProperties = new HashMap<String, Object>();
	private static Map<String, Map<String, Object>> allProperties = 
		new HashMap<String, Map<String, Object>>();

	private Map<String, Object> properties = nullProperties;

	public MemoryContext() {}
	
	@Override
	public synchronized void setId(String id) {
		this.id = id;

		if (this.id != null) {
			if (allProperties.containsKey(id)) {
				properties = allProperties.get(id);
			}
			else {
				properties = new HashMap<String, Object>();
				allProperties.put(id, properties);
			}
		}
		else {
			properties = nullProperties;
		}		
	}

	@Override
	public synchronized String getId() {
		return id;
	}

	@Override
	public synchronized void setServletUrlFromRequest (HttpServletRequest req) {
		// TODO: reckon with https
		servletUrl = "http://" + req.getServerName() + ":" + req.getServerPort() + 
			req.getContextPath() + req.getServletPath() + "/";
	}
	
	@Override
	public synchronized void setAgentClass(String agentClass) {
		this.agentClass = agentClass;
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
		return properties.get(key);
	}

	@Override
	public synchronized void put(String key, Object value) {
		properties.put(key, value);
	}

	@Override
	public synchronized boolean has(String key) {
		return properties.containsKey(key);
	}
}
