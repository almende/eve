package com.almende.eve.monitor;

import java.io.IOException;
import java.util.logging.Logger;

import com.almende.eve.agent.Agent;
import com.almende.eve.rpc.jsonrpc.JSONRPCException;
import com.almende.eve.rpc.jsonrpc.jackson.JOM;
import com.eaio.uuid.UUID;
import com.fasterxml.jackson.databind.node.ObjectNode;

public class Push implements ResultMonitorConfigType {
	private static final long	serialVersionUID	= -6113822981521869299L;
	private static final Logger	LOG		= Logger.getLogger(Push.class
			.getCanonicalName());
	private String  pushId      = null;
	private int		interval	= -1;
	private boolean	onEvent		= false;
	private boolean	onChange	= false;
	private String	event		= "";
	
	public Push(int interval, boolean onEvent) {
		this.pushId = new UUID().toString();
		this.interval = interval;
		this.onEvent = onEvent;
	}
	
	public Push() {
		this.pushId = new UUID().toString();
	}
	
	public Push onInterval(int interval) {
		this.interval = interval;
		return this;
	}
	
	public Push onEvent() {
		this.onEvent = true;
		return this;
	}
	
	public Push onEvent(String event) {
		this.onEvent = true;
		this.event = event;
		return this;
	}
	
	public Push onChange() {
		this.onChange = true;
		return this;
	}
	
	public void init(ResultMonitor monitor, Agent agent) throws IOException, JSONRPCException
			 {
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

		LOG.info("Registering push:"+monitor.getUrl());
		wrapper.put("pushId", monitor.getId() + "_" + pushId);

		monitor.getPushes().add(this);
		agent.sendAsync(monitor.getUrl(), "monitor.registerPush", wrapper,null,Void.class);
	}
	public void cancel(ResultMonitor monitor, Agent agent) throws IOException, JSONRPCException{
		ObjectNode params = JOM.createObjectNode();
		params.put("pushId",pushId);
		agent.sendAsync(monitor.getUrl(), "monitor.unregisterPush", params, null, Void.class);
	}

	public String getPushId() {
		return pushId;
	}

	public void setPushId(String pushId) {
		this.pushId = pushId;
	}

	public int getInterval() {
		return interval;
	}

	public void setInterval(int interval) {
		this.interval = interval;
	}

	public String getEvent() {
		return event;
	}

	public void setEvent(String event) {
		this.event = event;
	}
}
