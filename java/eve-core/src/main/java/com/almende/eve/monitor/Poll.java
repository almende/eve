package com.almende.eve.monitor;

import com.almende.eve.agent.Agent;
import com.almende.eve.rpc.jsonrpc.JSONRequest;
import com.almende.eve.rpc.jsonrpc.jackson.JOM;
import com.fasterxml.jackson.databind.node.ObjectNode;

public class Poll implements ResultMonitorConfigType {
	int interval;
	
	public Poll(int interval){
		this.interval=interval;
	};
	
	public Poll(){}
	
	public Poll onInterval(int interval){
		this.interval=interval;
		return this;
	}
	
	public String init(ResultMonitor monitor, Agent agent){
		ObjectNode params = JOM.createObjectNode();
		params.put("monitorId",monitor.getId());
		JSONRequest request = new JSONRequest("monitor.doPoll",params);
		
		return agent.getScheduler().createTask(request, interval, true, false);
	}
}
