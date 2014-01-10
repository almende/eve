/*
 * Copyright: Almende B.V. (2014), Rotterdam, The Netherlands
 * License: The Apache Software License, Version 2.0
 */
package com.almende.eve.monitor;

import java.io.IOException;
import java.util.logging.Logger;

import com.almende.eve.agent.AgentInterface;
import com.almende.eve.rpc.jsonrpc.JSONRPCException;
import com.almende.eve.rpc.jsonrpc.jackson.JOM;
import com.almende.util.uuid.UUID;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * The Class Push.
 */
public class Push implements ResultMonitorConfigType {
	private static final long	serialVersionUID	= -6113822981521869299L;
	private static final Logger	LOG					= Logger.getLogger(Push.class
															.getCanonicalName());
	private String				pushId				= null;
	private int					interval			= -1;
	private boolean				onEvent				= false;
	private boolean				onChange			= false;
	private String				event				= "";
	
	/**
	 * Instantiates a new push.
	 * 
	 * @param interval
	 *            the interval
	 * @param onEvent
	 *            the on event
	 */
	public Push(final int interval, final boolean onEvent) {
		pushId = new UUID().toString();
		this.interval = interval;
		this.onEvent = onEvent;
	}
	
	/**
	 * Instantiates a new push.
	 */
	public Push() {
		pushId = new UUID().toString();
	}
	
	/**
	 * On interval.
	 * 
	 * @param interval
	 *            the interval
	 * @return the push
	 */
	public Push onInterval(final int interval) {
		this.interval = interval;
		return this;
	}
	
	/**
	 * On event.
	 * 
	 * @return the push
	 */
	public Push onEvent() {
		onEvent = true;
		return this;
	}
	
	/**
	 * On event.
	 * 
	 * @param event
	 *            the event
	 * @return the push
	 */
	public Push onEvent(final String event) {
		onEvent = true;
		this.event = event;
		return this;
	}
	
	/**
	 * On change.
	 * 
	 * @return the push
	 */
	public Push onChange() {
		onChange = true;
		return this;
	}
	
	/**
	 * Inits the.
	 * 
	 * @param monitor
	 *            the monitor
	 * @param agent
	 *            the agent
	 * @throws IOException
	 *             Signals that an I/O exception has occurred.
	 * @throws JSONRPCException
	 *             the jSONRPC exception
	 */
	public void init(final ResultMonitor monitor, final AgentInterface agent)
			throws IOException, JSONRPCException {
		final ObjectNode wrapper = JOM.createObjectNode();
		final ObjectNode config = JOM.createObjectNode();
		
		config.put("monitorId", monitor.getId());
		if (interval > 0) {
			config.put("interval", interval);
		}
		config.put("onEvent", onEvent);
		if (!event.equals("")) {
			config.put("event", event);
		}
		config.put("onChange", onChange);
		config.put("method", monitor.getMethod());
		config.put("params", monitor.getParams());
		
		wrapper.put("config", config);
		
		LOG.info("Registering push:" + monitor.getUrl());
		wrapper.put("pushId", monitor.getId() + "_" + pushId);
		
		monitor.getPushes().add(this);
		agent.sendAsync(monitor.getUrl(), "monitor.registerPush", wrapper,
				null, Void.class);
	}
	
	/**
	 * Cancel.
	 * 
	 * @param monitor
	 *            the monitor
	 * @param agent
	 *            the agent
	 * @throws IOException
	 *             Signals that an I/O exception has occurred.
	 * @throws JSONRPCException
	 *             the jSONRPC exception
	 */
	public void cancel(final ResultMonitor monitor, final AgentInterface agent)
			throws IOException, JSONRPCException {
		final ObjectNode params = JOM.createObjectNode();
		params.put("pushId", pushId);
		agent.sendAsync(monitor.getUrl(), "monitor.unregisterPush", params,
				null, Void.class);
	}
	
	/**
	 * Gets the push id.
	 * 
	 * @return the push id
	 */
	public String getPushId() {
		return pushId;
	}
	
	/**
	 * Sets the push id.
	 * 
	 * @param pushId
	 *            the new push id
	 */
	public void setPushId(final String pushId) {
		this.pushId = pushId;
	}
	
	/**
	 * Gets the interval.
	 * 
	 * @return the interval
	 */
	public int getInterval() {
		return interval;
	}
	
	/**
	 * Sets the interval.
	 * 
	 * @param interval
	 *            the new interval
	 */
	public void setInterval(final int interval) {
		this.interval = interval;
	}
	
	/**
	 * Gets the event.
	 * 
	 * @return the event
	 */
	public String getEvent() {
		return event;
	}
	
	/**
	 * Sets the event.
	 * 
	 * @param event
	 *            the new event
	 */
	public void setEvent(final String event) {
		this.event = event;
	}
}
