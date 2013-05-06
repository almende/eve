package com.almende.eve.entity;

import java.util.List;

import com.almende.eve.agent.Agent;
import com.almende.eve.rpc.jsonrpc.jackson.JOM;
import com.fasterxml.jackson.databind.node.ObjectNode;

public class Push implements RepeatConfigType {
	int		interval	= -1;
	boolean	onEvent	= false;
	
	public Push(int interval, boolean onEvent) {
		this.interval = interval;
		this.onEvent = onEvent;
	}
	
	@SuppressWarnings("unchecked")
	public List<String> init(Repeat repeat, Agent agent) throws Exception {
		ObjectNode wrapper = JOM.createObjectNode();
		ObjectNode params = JOM.createObjectNode();
		
		params.put("repeatId", repeat.id);
		if (interval > 0) {
			params.put("interval", interval);
		}
		params.put("onEvent", onEvent);
		params.put("method", repeat.method);
		params.put("params", repeat.params);
		
		wrapper.put("params", params);
		return agent.send(repeat.url, "registerPush", wrapper, List.class);
		
	}
}
