package com.almende.eve.monitor;

import java.io.IOException;
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
import com.almende.eve.transport.AsyncCallback;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

public class ResultMonitor implements Serializable {
	private static final long					serialVersionUID	= -6738643681425840533L;
	private static final Logger					LOG					= Logger.getLogger(ResultMonitor.class
																			.getCanonicalName());
	
	private String								id;
	private String								agentId;
	private URI									url;
	private String								method;
	private String								params;
	private String								callbackMethod;
	private List<String>							schedulerIds		= new ArrayList<String>();
	private List<String>							remoteIds			= new ArrayList<String>();
	private String								cacheType;
	
	private static transient Map<String, Cache>	caches				= new HashMap<String, Cache>();
	
	public ResultMonitor(String agentId, URI url, String method,
			ObjectNode params, String callbackMethod) {
		this.id = UUID.randomUUID().toString();
		this.agentId = agentId;
		this.url = url;
		this.method = method;
		try {
			this.params = JOM.getInstance().writeValueAsString(params);
		} catch (JsonProcessingException e) {
			LOG.log(Level.SEVERE,"Failed to process params.",e);
		}
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
	
	public void addPush(final Push config) {
		AgentHost factory = AgentHost.getInstance();
		final ResultMonitor _this = this;
		List<String> result = new ArrayList<String>();
		try {
			Agent agent = factory.getAgent(agentId);
			config.init(this, agent, new AsyncCallback<List<String>>(){

				@Override
				public void onSuccess(List<String> result) {
					_this.remoteIds=result;
				}

				@Override
				public void onFailure(Exception exception) {
					LOG.log(Level.WARNING, "Couldn't init Pushing: "+config.toString());
					LOG.log(Level.WARNING, "Exception:", exception);
				}
				
			},result.getClass());
			
		} catch (Exception e) {
			LOG.log(Level.WARNING, "Couldn't init Pushing!", e);
		}
	}
	
	public String store() {
		AgentHost factory = AgentHost.getInstance();
		
		try {
			Agent agent = factory.getAgent(agentId);
			@SuppressWarnings("unchecked")
			HashMap<String, ResultMonitor> monitors = (HashMap<String, ResultMonitor>) agent
					.getState().get("_monitors");
			HashMap<String, ResultMonitor> newmonitors = new HashMap<String, ResultMonitor>();
			if (monitors != null) {
				newmonitors.putAll(monitors);
			}
			newmonitors.put(id, this);
			if (!agent.getState().putIfUnchanged("_monitors",
					newmonitors, monitors)) {
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
	
	public static void cancelAll(String agentId){
		for (ResultMonitor monitor: getMonitors(agentId).values()){
			monitor.delete();
		}
	}
	
	public static Map<String,ResultMonitor> getMonitors(String agentId) {
		AgentHost factory = AgentHost.getInstance();
		
		try {
			Agent agent = factory.getAgent(agentId);
			@SuppressWarnings("unchecked")
			Map<String, ResultMonitor> monitors = (Map<String, ResultMonitor>) agent
					.getState().get("_monitors");
			if (monitors == null) {
				monitors = new HashMap<String, ResultMonitor>();
			}
			return monitors;
		} catch (Exception e) {
			LOG.log(Level.WARNING, "Couldn't find monitors.", e);
		}
		return null;
	}
	
	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getAgentId() {
		return agentId;
	}

	public void setAgentId(String agentId) {
		this.agentId = agentId;
	}

	public URI getUrl() {
		return url;
	}

	public void setUrl(URI url) {
		this.url = url;
	}

	public String getMethod() {
		return method;
	}

	public void setMethod(String method) {
		this.method = method;
	}

	public JsonNode getParams() throws IOException {
		return JOM.getInstance().readTree(params);
	}

	public void setParams(String params) {
		this.params = params;
	}

	public String getCallbackMethod() {
		return callbackMethod;
	}

	public void setCallbackMethod(String callbackMethod) {
		this.callbackMethod = callbackMethod;
	}

	public List<String> getSchedulerIds() {
		return schedulerIds;
	}

	public void setSchedulerIds(List<String> schedulerIds) {
		this.schedulerIds = schedulerIds;
	}

	public List<String> getRemoteIds() {
		return remoteIds;
	}

	public void setRemoteIds(List<String> remoteIds) {
		this.remoteIds = remoteIds;
	}

	public String getCacheType() {
		return cacheType;
	}

	public void setCacheType(String cacheType) {
		this.cacheType = cacheType;
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
