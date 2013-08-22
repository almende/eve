package com.almende.eve.monitor;

import java.io.IOException;
import java.io.Serializable;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
	private List<String>						schedulerIds		= new ArrayList<String>();
	private List<String>						remoteIds			= new ArrayList<String>();
	private String								cacheType;
	
	private static transient Map<String, Cache>	caches				= new HashMap<String, Cache>();
	private transient Agent						myAgent				= null;
	
	public ResultMonitor(String id, String agentId, URI url, String method,
			ObjectNode params, String callbackMethod) {
		this.id = id;
		this.agentId = agentId;
		this.url = url;
		this.method = method;
		try {
			this.params = JOM.getInstance().writeValueAsString(params);
		} catch (JsonProcessingException e) {
			LOG.log(Level.SEVERE, "Failed to process params.", e);
		}
		this.callbackMethod = callbackMethod;
		loadAgent();
	}
	
	public final void loadAgent() {
		if (myAgent == null) {
			AgentHost factory = AgentHost.getInstance();
			
			try {
				myAgent = factory.getAgent(agentId);
			} catch (Exception e) {
				LOG.severe("Couldn't load agent of ResultMonitor."
						+ e.getLocalizedMessage());
			}
		}
	}
	
	public ResultMonitor(String id, String agentId, URI url, String method,
			ObjectNode params) {
		this(id, agentId, url, method, params, null);
	}
	
	public void init() {
		loadAgent();
		if (!caches.containsKey(id) && cacheType != null) {
			try {
				addCache((Cache) Class.forName(cacheType).newInstance());
			} catch (Exception e) {
				LOG.warning("Couldn't load cache for monitor:" + id + " "
						+ e.getLocalizedMessage());
			}
		}
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
		loadAgent();
		String taskId = config.init(this, myAgent);
		schedulerIds.add(taskId);
	}
	
	public void addPush(final Push config) {
		loadAgent();
		final ResultMonitor _this = this;
		List<String> result = new ArrayList<String>();
		try {
			config.init(this, myAgent, new AsyncCallback<List<String>>() {
				
				@Override
				public void onSuccess(List<String> result) {
					_this.remoteIds = result;
				}
				
				@Override
				public void onFailure(Exception exception) {
					LOG.log(Level.WARNING,
							"Couldn't init Pushing: " + config.toString());
					LOG.log(Level.WARNING, "Exception:", exception);
				}
				
			}, result.getClass());
		} catch (Exception e) {
			LOG.warning("Couldn't init Pushing:" + e);
		}
	}
	
	/**
	 * Conveniency method to store ResultMonitor, equivalent to
	 * ResultMonitorFactory.store(this);
	 * 
	 * @return MonitorId
	 */
	public String store() {
		loadAgent();
		ResultMonitorFactoryInterface factory = myAgent
				.getResultMonitorFactory();
		return factory.store(this);
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
