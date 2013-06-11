package com.almende.eve.monitor;

import java.io.Serializable;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.almende.eve.agent.Agent;
import com.almende.eve.agent.AgentHost;
import com.almende.eve.rpc.jsonrpc.jackson.JOM;
import com.fasterxml.jackson.databind.node.ObjectNode;

public class ResultMonitor implements Serializable {
	private static final long					serialVersionUID	= -6738643681425840533L;
	private static final Logger					LOG					= Logger.getLogger(ResultMonitor.class
																			.getCanonicalName());
	
	public String								id;
	public String								agentId;
	public URI									url;
	public String								method;
	public ObjectNode							params;
	public String								callbackMethod;
	public List<String>							schedulerIds		= new ArrayList<String>();
	public List<String>							remoteIds			= new ArrayList<String>();
	public String								cacheType;
	
	private static transient Map<String, Cache>	caches				= new HashMap<String, Cache>();
	
	public ResultMonitor(String agentId, URI url, String method,
			ObjectNode params, String callbackMethod) {
		this.id = UUID.randomUUID().toString();
		this.agentId = agentId;
		this.url = url;
		this.method = method;
		this.params = params;
		this.callbackMethod = callbackMethod;
	}
	
	public ResultMonitor(String agentId, URI url, String method,
			ObjectNode params) {
		this(agentId, url, method, params, null);
	}
	
	public ResultMonitor add(ResultMonitorConfigType config) {
		if (config instanceof Cache) {
			this.addCache((Cache) config);
		}
		if (config instanceof Poll) {
			this.addPoll((Poll) config);
		}
		if (config instanceof Push) {
			this.addPush((Push) config);
		}
		return this;
	}
	
	public boolean hasCache() {
		return cacheType != null;
	}
	
	public void addCache(Cache config) {
		cacheType = config.getClass().getName();
		synchronized (caches) {
			caches.put(id, config);
		}
	}
	
	public Cache getCache() {
		synchronized (caches) {
			return caches.get(id);
		}
	}
	
	public void addPoll(Poll config) {
		AgentHost factory = AgentHost.getInstance();
		
		try {
			Agent agent = factory.getAgent(agentId);
			String taskId = config.init(this, agent);
			schedulerIds.add(taskId);
		} catch (Exception e) {
			LOG.log(Level.WARNING, "Couldn't init polling!", e);
		}
	}
	
	public void addPush(Push config) {
		AgentHost factory = AgentHost.getInstance();
		
		try {
			Agent agent = factory.getAgent(agentId);
			List<String> remoteIds = config.init(this, agent);
			this.remoteIds = remoteIds;
		} catch (Exception e) {
			LOG.log(Level.WARNING, "Couldn't init Pushing!", e);
		}
	}
	
	public String store() {
		AgentHost factory = AgentHost.getInstance();
		
		try {
			Agent agent = factory.getAgent(agentId);
			@SuppressWarnings("unchecked")
			Map<String, ResultMonitor> monitors = (Map<String, ResultMonitor>) agent
					.getState().get("_monitors");
			Map<String, ResultMonitor> newmonitors = new HashMap<String, ResultMonitor>();
			if (monitors != null) {
				newmonitors.putAll(monitors);
			}
			newmonitors.put(id, this);
			if (!agent.getState().putIfUnchanged("_monitors",
					(Serializable) newmonitors, (Serializable) monitors)) {
				// recursive retry.
				store();			}
		} catch (Exception e) {
			LOG.log(Level.WARNING, "Couldn't find monitors:" + agentId + "."
					+ id, e);
		}
		return id;
	}
	
	public void delete() {
		AgentHost factory = AgentHost.getInstance();
		
		try {
			Agent agent = factory.getAgent(agentId);
			@SuppressWarnings("unchecked")
			Map<String, ResultMonitor> monitors = (Map<String, ResultMonitor>) agent
					.getState().get("_monitors");
			Map<String, ResultMonitor> newmonitors = new HashMap<String, ResultMonitor>();
			if (monitors != null) {
				newmonitors.putAll(monitors);
			}
			newmonitors.remove(id);
			
			if (!agent.getState().putIfUnchanged("_monitors",
					(Serializable) newmonitors, (Serializable) monitors)) {
				// recursive retry.
				delete(); 
			}
		} catch (Exception e) {
			LOG.log(Level.WARNING, "Couldn't delete monitor:" + agentId + "."
					+ id, e);
		}
	}
	
	public static ResultMonitor getMonitorById(String agentId, String id) {
		AgentHost factory = AgentHost.getInstance();
		
		try {
			Agent agent = factory.getAgent(agentId);
			@SuppressWarnings("unchecked")
			Map<String, ResultMonitor> monitors = (Map<String, ResultMonitor>) agent
					.getState().get("_monitors");
			if (monitors == null) {
				monitors = new HashMap<String, ResultMonitor>();
			}
			ResultMonitor result = monitors.get(id);
			if (result != null && !caches.containsKey(id)
					&& result.cacheType != null) {
				result.addCache((Cache) Class.forName(result.cacheType)
						.newInstance());
			}
			return result;
		} catch (Exception e) {
			LOG.log(Level.WARNING, "Couldn't find monitor:" + agentId + "."
					+ id, e);
		}
		return null;
	}
	
	public String toString() {
		try {
			return JOM.getInstance().writeValueAsString(this);
		} catch (Exception e) {
			LOG.log(Level.WARNING, "", e);
			return "";
		}
	}
}
