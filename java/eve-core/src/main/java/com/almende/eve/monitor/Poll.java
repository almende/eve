package com.almende.eve.monitor;

import com.almende.eve.agent.Agent;
import com.almende.eve.rpc.jsonrpc.JSONRequest;
import com.almende.eve.rpc.jsonrpc.jackson.JOM;
import com.fasterxml.jackson.databind.node.ObjectNode;

public class Poll implements ResultMonitorConfigType {
	private static final long	serialVersionUID	= 1521097261949700084L;
	private int					interval;
	private String				taskId				= null;
	
	public Poll(int interval) {
		this.interval = interval;
	};
	
	public Poll() {
	}
	
	public Poll onInterval(int interval) {
		this.interval = interval;
		return this;
	}
	
	public void cancel(ResultMonitor monitor, Agent agent) {
		if (taskId != null) {
			agent.getScheduler().cancelTask(taskId);
		}
	}
	
	public void init(ResultMonitor monitor, Agent agent) {
		ObjectNode params = JOM.createObjectNode();
		params.put("monitorId", monitor.getId());
		JSONRequest request = new JSONRequest("monitor.doPoll", params);
		
		taskId = agent.getScheduler()
				.createTask(request, interval, true, false);
		monitor.getPolls().add(this);
	}
}
