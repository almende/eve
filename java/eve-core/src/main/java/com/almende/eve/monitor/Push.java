package com.almende.eve.monitor;

import java.io.IOException;
import java.util.logging.Logger;

import com.almende.eve.agent.Agent;
import com.almende.eve.rpc.jsonrpc.JSONRPCException;
import com.almende.eve.rpc.jsonrpc.jackson.JOM;
import com.almende.eve.transport.AsyncCallback;
import com.fasterxml.jackson.databind.node.ObjectNode;

public class Push implements ResultMonitorConfigType {
	private static final Logger	LOG		= Logger.getLogger(Push.class
			.getCanonicalName());
	private int		interval	= -1;
	private boolean	onEvent		= false;
	private boolean	onChange	= false;
	private String	event		= "";
	
	public Push(int interval, boolean onEvent) {
		this.interval = interval;
		this.onEvent = onEvent;
	}
	
	public Push() {
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
	
	public void init(ResultMonitor monitor, Agent agent, AsyncCallback<?> callback, Class<?> callbackType) throws IOException, JSONRPCException
			 {
		ObjectNode wrapper = JOM.createObjectNode();
		ObjectNode pushParams = JOM.createObjectNode();
		
		pushParams.put("monitorId", monitor.getId());
		if (interval > 0) {
			pushParams.put("interval", interval);
		}
		pushParams.put("onEvent", onEvent);
		if (!event.equals("")) {
			pushParams.put("event", event);
		}
		pushParams.put("onChange", onChange);
		pushParams.put("method", monitor.getMethod());
		pushParams.put("params", monitor.getParams());
		
		wrapper.put("pushParams", pushParams);

		LOG.info("Registering push:"+monitor.getUrl());
		agent.sendAsync(monitor.getUrl(), "monitor.registerPush", wrapper, callback, callbackType);
	}
}
