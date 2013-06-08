package com.almende.eve.monitor;

import java.util.ArrayList;
import java.util.List;

import com.almende.eve.agent.Agent;
import com.almende.eve.rpc.jsonrpc.jackson.JOM;
import com.fasterxml.jackson.databind.node.ObjectNode;

public class Push implements ResultMonitorConfigType {
	int		interval	= -1;
	boolean	onEvent		= false;
	boolean	onChange	= false;
	String	event		= "";
	
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
	
	public List<String> init(ResultMonitor monitor, Agent agent)
			throws Exception {
		ObjectNode wrapper = JOM.createObjectNode();
		ObjectNode pushParams = JOM.createObjectNode();
		
		pushParams.put("monitorId", monitor.id);
		if (interval > 0) {
			pushParams.put("interval", interval);
		}
		pushParams.put("onEvent", onEvent);
		if (!event.equals("")) pushParams.put("event", event);
		pushParams.put("onChange", onChange);
		pushParams.put("method", monitor.method);
		pushParams.put("params", monitor.params);
		
		wrapper.put("pushParams", pushParams);
		List<String> result = new ArrayList<String>();
		return agent.send(result, monitor.url, "monitor.registerPush", wrapper);
	}
}
