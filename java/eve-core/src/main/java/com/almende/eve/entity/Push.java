package com.almende.eve.entity;

import java.util.List;

import com.almende.eve.agent.Agent;
import com.almende.eve.rpc.jsonrpc.jackson.JOM;
import com.fasterxml.jackson.databind.node.ObjectNode;

public class Push implements ResultMonitorConfigType {
	int		interval	= -1;
	boolean	onEvent	= false;
	
	public Push(int interval, boolean onEvent) {
		this.interval = interval;
		this.onEvent = onEvent;
	}
	
	@SuppressWarnings("unchecked")
	public List<String> init(ResultMonitor monitor, Agent agent) throws Exception {
		ObjectNode wrapper = JOM.createObjectNode();
		ObjectNode params = JOM.createObjectNode();
		
		params.put("monitorId", monitor.id);
		if (interval > 0) {
			params.put("interval", interval);
		}
		params.put("onEvent", onEvent);
		params.put("method", monitor.method);
		params.put("params", monitor.params);
		
		wrapper.put("params", params);
		return agent.send(monitor.url, "registerPush", wrapper, List.class);
		
	}
}
