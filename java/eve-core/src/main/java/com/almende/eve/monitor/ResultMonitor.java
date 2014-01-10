/*
 * Copyright: Almende B.V. (2014), Rotterdam, The Netherlands
 * License: The Apache Software License, Version 2.0
 */
package com.almende.eve.monitor;

import java.io.Serializable;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.almende.eve.agent.AgentHost;
import com.almende.eve.agent.AgentInterface;
import com.almende.eve.rpc.jsonrpc.jackson.JOM;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * The Class ResultMonitor.
 */
public class ResultMonitor implements Serializable {
	private static final long					serialVersionUID	= -6738643681425840533L;
	private static final Logger					LOG					= Logger.getLogger(ResultMonitor.class
																			.getCanonicalName());
	private static transient Map<String, Cache>	caches				= new HashMap<String, Cache>();
	private String								id;
	private String								agentId;
	private URI									url;
	private String								method;
	private String								params;
	private String								callbackMethod;
	private List<Poll>							polls				= new ArrayList<Poll>();
	private List<Push>							pushes				= new ArrayList<Push>();
	private String								cacheType;
	private transient AgentInterface			myAgent				= null;
	
	/**
	 * Instantiates a new result monitor.
	 */
	public ResultMonitor() {
	};
	
	/**
	 * Instantiates a new result monitor.
	 * 
	 * @param id
	 *            the id
	 * @param agentId
	 *            the agent id
	 * @param url
	 *            the url
	 * @param method
	 *            the method
	 * @param params
	 *            the params
	 * @param callbackMethod
	 *            the callback method
	 */
	public ResultMonitor(final String id, final String agentId, final URI url,
			final String method, final ObjectNode params,
			final String callbackMethod) {
		this.id = id;
		this.agentId = agentId;
		this.url = url;
		this.method = method;
		try {
			this.params = JOM.getInstance().writeValueAsString(params);
		} catch (final JsonProcessingException e) {
			LOG.log(Level.SEVERE, "Failed to process params.", e);
		}
		this.callbackMethod = callbackMethod;
		loadAgent();
	}
	
	/**
	 * Load agent.
	 */
	public final void loadAgent() {
		if (myAgent == null) {
			final AgentHost host = AgentHost.getInstance();
			
			try {
				myAgent = host.getAgent(agentId);
			} catch (final Exception e) {
				LOG.severe("Couldn't load agent of ResultMonitor."
						+ e.getLocalizedMessage());
			}
		}
	}
	
	/**
	 * Instantiates a new result monitor.
	 * 
	 * @param id
	 *            the id
	 * @param agentId
	 *            the agent id
	 * @param url
	 *            the url
	 * @param method
	 *            the method
	 * @param params
	 *            the params
	 */
	public ResultMonitor(final String id, final String agentId, final URI url,
			final String method, final ObjectNode params) {
		this(id, agentId, url, method, params, null);
	}
	
	/**
	 * Inits the.
	 */
	public void init() {
		loadAgent();
		if (!caches.containsKey(id) && cacheType != null) {
			try {
				addCache((Cache) Class.forName(cacheType).newInstance());
			} catch (final Exception e) {
				LOG.warning("Couldn't load cache for monitor:" + id + " "
						+ e.getLocalizedMessage());
			}
		}
	}
	
	/**
	 * Adds the.
	 * 
	 * @param config
	 *            the config
	 * @return the result monitor
	 */
	public ResultMonitor add(final ResultMonitorConfigType config) {
		if (config instanceof Cache) {
			addCache((Cache) config);
		}
		if (config instanceof Poll) {
			addPoll((Poll) config);
		}
		if (config instanceof Push) {
			addPush((Push) config);
		}
		return this;
	}
	
	/**
	 * Checks for cache.
	 * 
	 * @return true, if successful
	 */
	public boolean hasCache() {
		return cacheType != null;
	}
	
	/**
	 * Adds the cache.
	 * 
	 * @param config
	 *            the config
	 */
	public void addCache(final Cache config) {
		cacheType = config.getClass().getName();
		synchronized (caches) {
			caches.put(id, config);
		}
	}
	
	/**
	 * Gets the cache.
	 * 
	 * @return the cache
	 */
	@JsonIgnore
	public Cache getCache() {
		synchronized (caches) {
			return caches.get(id);
		}
	}
	
	/**
	 * Adds the poll.
	 * 
	 * @param config
	 *            the config
	 */
	public void addPoll(final Poll config) {
		loadAgent();
		config.init(this, myAgent);
	}
	
	/**
	 * Adds the push.
	 * 
	 * @param config
	 *            the config
	 */
	public void addPush(final Push config) {
		loadAgent();
		try {
			config.init(this, myAgent);
		} catch (final Exception e) {
			LOG.warning("Failed to register push:" + e);
		}
	}
	
	/**
	 * Cancel.
	 */
	public void cancel() {
		LOG.info("Canceling monitor:" + id);
		for (final Poll poll : getPolls()) {
			poll.cancel(this, myAgent);
		}
		for (final Push push : getPushes()) {
			try {
				push.cancel(this, myAgent);
			} catch (final Exception e) {
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
		final ResultMonitorFactoryInterface factory = myAgent
				.getResultMonitorFactory();
		return factory.store(this);
	}
	
	/**
	 * Gets the id.
	 * 
	 * @return the id
	 */
	public String getId() {
		return id;
	}
	
	/**
	 * Sets the id.
	 * 
	 * @param id
	 *            the new id
	 */
	public void setId(final String id) {
		this.id = id;
	}
	
	/**
	 * Gets the agent id.
	 * 
	 * @return the agent id
	 */
	public String getAgentId() {
		return agentId;
	}
	
	/**
	 * Sets the agent id.
	 * 
	 * @param agentId
	 *            the new agent id
	 */
	public void setAgentId(final String agentId) {
		this.agentId = agentId;
	}
	
	/**
	 * Gets the url.
	 * 
	 * @return the url
	 */
	public URI getUrl() {
		return url;
	}
	
	/**
	 * Sets the url.
	 * 
	 * @param url
	 *            the new url
	 */
	public void setUrl(final URI url) {
		this.url = url;
	}
	
	/**
	 * Gets the method.
	 * 
	 * @return the method
	 */
	public String getMethod() {
		return method;
	}
	
	/**
	 * Sets the method.
	 * 
	 * @param method
	 *            the new method
	 */
	public void setMethod(final String method) {
		this.method = method;
	}
	
	/**
	 * Gets the params.
	 * 
	 * @return the params
	 */
	public String getParams() {
		return params;
	}
	
	/**
	 * Sets the params.
	 * 
	 * @param params
	 *            the new params
	 */
	public void setParams(final String params) {
		this.params = params;
	}
	
	/**
	 * Gets the callback method.
	 * 
	 * @return the callback method
	 */
	public String getCallbackMethod() {
		return callbackMethod;
	}
	
	/**
	 * Sets the callback method.
	 * 
	 * @param callbackMethod
	 *            the new callback method
	 */
	public void setCallbackMethod(final String callbackMethod) {
		this.callbackMethod = callbackMethod;
	}
	
	/**
	 * Gets the polls.
	 * 
	 * @return the polls
	 */
	public List<Poll> getPolls() {
		return polls;
	}
	
	/**
	 * Sets the polls.
	 * 
	 * @param polls
	 *            the new polls
	 */
	public void setPolls(final List<Poll> polls) {
		this.polls = polls;
	}
	
	/**
	 * Gets the pushes.
	 * 
	 * @return the pushes
	 */
	public List<Push> getPushes() {
		return pushes;
	}
	
	/**
	 * Sets the pushes.
	 * 
	 * @param pushes
	 *            the new pushes
	 */
	public void setPushes(final List<Push> pushes) {
		this.pushes = pushes;
	}
	
	/**
	 * Gets the cache type.
	 * 
	 * @return the cache type
	 */
	public String getCacheType() {
		return cacheType;
	}
	
	/**
	 * Sets the cache type.
	 * 
	 * @param cacheType
	 *            the new cache type
	 */
	public void setCacheType(final String cacheType) {
		this.cacheType = cacheType;
	}
	
	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		try {
			return JOM.getInstance().writeValueAsString(this);
		} catch (final Exception e) {
			LOG.log(Level.WARNING, "", e);
			return "";
		}
	}
}
