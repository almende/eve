package com.almende.eve.monitor;

import java.io.Serializable;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.almende.eve.agent.Agent;
import com.almende.eve.agent.AgentHostDefImpl;
import com.almende.eve.rpc.jsonrpc.jackson.JOM;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.core.JsonProcessingException;
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
	private List<Poll>							polls				= new ArrayList<Poll>();
	private List<Push>							pushes				= new ArrayList<Push>();
	private String								cacheType;
	
	private static transient Map<String, Cache>	caches				= new HashMap<String, Cache>();
	private transient Agent						myAgent				= null;
	
	public ResultMonitor(){};
	
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
			AgentHostDefImpl factory = AgentHostDefImpl.getInstance();
			
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
	
	@JsonIgnore
	public Cache getCache() {
		synchronized (caches) {
			return caches.get(id);
		}
	}
	
	public void addPoll(Poll config) {
		loadAgent();
		config.init(this, myAgent);
	}
	
	public void addPush(final Push config) {
		loadAgent();
		try {
			config.init(this, myAgent);
		} catch (Exception e) {
			LOG.warning("Failed to register push:" + e);
		}
	}
	
	public void cancel() {
		LOG.info("Canceling monitor:"+this.id);
		for (Poll poll : getPolls()) {
			poll.cancel(this, myAgent);
		}
		for (Push push : getPushes()) {
			try {
				push.cancel(this, myAgent);
			} catch (Exception e) {
				LOG.warning("Failed to cancel push:" + e.getLocalizedMessage());
			}
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
	
	public String getParams() {
		return params;
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
	
	public List<Poll> getPolls() {
		return polls;
	}
	
	public void setPolls(List<Poll> polls) {
		this.polls = polls;
	}
	
	public List<Push> getPushes() {
		return pushes;
	}
	
	public void setPushes(List<Push> pushes) {
		this.pushes = pushes;
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
