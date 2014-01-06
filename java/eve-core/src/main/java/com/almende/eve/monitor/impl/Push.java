package com.almende.eve.monitor.impl;

import java.io.IOException;
import java.util.logging.Logger;

import com.almende.eve.agent.AgentInterface;
import com.almende.eve.monitor.PushInterface;
import com.almende.eve.monitor.ResultMonitor;
import com.almende.eve.rpc.jsonrpc.JSONRPCException;
import com.almende.eve.rpc.jsonrpc.jackson.JOM;
import com.almende.util.uuid.UUID;
import com.fasterxml.jackson.databind.node.ObjectNode;

public class Push implements PushInterface {
	private static final long	serialVersionUID	= -6113822981521869299L;
	private static final Logger	LOG					= Logger.getLogger(PushInterface.class
															.getCanonicalName());
	private String				pushId				= null;
	private int					interval			= -1;
	private boolean				onEvent				= false;
	private boolean				onChange			= false;
	private String				event				= "";
	
	public Push(int interval, boolean onEvent) {
		this.pushId = new UUID().toString();
		this.interval = interval;
		this.onEvent = onEvent;
	}
	
	public Push() {
		this.pushId = new UUID().toString();
	}
	
	@Override
	public PushInterface onInterval(int interval) {
		this.interval = interval;
		return this;
	}
	
	@Override
	public PushInterface onEvent() {
		this.onEvent = true;
		return this;
	}
	
	@Override
	public PushInterface onEvent(String event) {
		this.onEvent = true;
		this.event = event;
		return this;
	}
	
	@Override
	public PushInterface onChange() {
		this.onChange = true;
		return this;
	}
	
	@Override
	public void init(ResultMonitor monitor, AgentInterface agent)
			throws IOException, JSONRPCException {
		ObjectNode wrapper = JOM.createObjectNode();
		ObjectNode config = JOM.createObjectNode();
		
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
	
	@Override
	public void cancel(ResultMonitor monitor, AgentInterface agent)
			throws IOException, JSONRPCException {
		ObjectNode params = JOM.createObjectNode();
		params.put("pushId", pushId);
		agent.sendAsync(monitor.getUrl(), "monitor.unregisterPush", params,
				null, Void.class);
	}
	
	@Override
	public String getPushId() {
		return pushId;
	}
	
	@Override
	public void setPushId(String pushId) {
		this.pushId = pushId;
	}
	
	@Override
	public int getInterval() {
		return interval;
	}
	
	@Override
	public void setInterval(int interval) {
		this.interval = interval;
	}
	
	@Override
	public String getEvent() {
		return event;
	}
	
	@Override
	public void setEvent(String event) {
		this.event = event;
	}
}
