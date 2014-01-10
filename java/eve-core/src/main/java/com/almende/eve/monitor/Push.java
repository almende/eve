package com.almende.eve.monitor;

import java.io.IOException;
import java.util.logging.Logger;

import com.almende.eve.agent.AgentInterface;
import com.almende.eve.rpc.jsonrpc.JSONRPCException;
import com.almende.eve.rpc.jsonrpc.jackson.JOM;
import com.almende.util.uuid.UUID;
import com.fasterxml.jackson.databind.node.ObjectNode;

public class Push implements ResultMonitorConfigType {
	private static final long	serialVersionUID	= -6113822981521869299L;
	private static final Logger	LOG					= Logger.getLogger(Push.class
															.getCanonicalName());
	private String				pushId				= null;
	private int					interval			= -1;
	private boolean				onEvent				= false;
	private boolean				onChange			= false;
	private String				event				= "";
	
	public Push(final int interval, final boolean onEvent) {
		pushId = new UUID().toString();
		this.interval = interval;
		this.onEvent = onEvent;
	}
	
	public Push() {
		pushId = new UUID().toString();
	}
	
	public Push onInterval(final int interval) {
		this.interval = interval;
		return this;
	}
	
	public Push onEvent() {
		onEvent = true;
		return this;
	}
	
	public Push onEvent(final String event) {
		onEvent = true;
		this.event = event;
		return this;
	}
	
	public Push onChange() {
		onChange = true;
		return this;
	}
	
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
	
	public void cancel(final ResultMonitor monitor, final AgentInterface agent)
			throws IOException, JSONRPCException {
		final ObjectNode params = JOM.createObjectNode();
		params.put("pushId", pushId);
		agent.sendAsync(monitor.getUrl(), "monitor.unregisterPush", params,
				null, Void.class);
	}
	
	public String getPushId() {
		return pushId;
	}
	
	public void setPushId(final String pushId) {
		this.pushId = pushId;
	}
	
	public int getInterval() {
		return interval;
	}
	
	public void setInterval(final int interval) {
		this.interval = interval;
	}
	
	public String getEvent() {
		return event;
	}
	
	public void setEvent(final String event) {
		this.event = event;
	}
}
