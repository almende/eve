package com.almende.eve.monitor;

import java.util.logging.Logger;

import com.almende.eve.agent.Agent;
import com.almende.eve.rpc.jsonrpc.JSONRequest;
import com.almende.eve.rpc.jsonrpc.jackson.JOM;
import com.fasterxml.jackson.databind.node.ObjectNode;

public class Poll implements ResultMonitorConfigType {
	private static final long	serialVersionUID	= 1521097261949700084L;
	private static final Logger	LOG		= Logger.getLogger(Push.class
			.getCanonicalName());

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
		
		//Try to cancel any protential existing tasks.
		cancel(monitor,agent);
		
		taskId = agent.getScheduler()
				.createTask(request, interval, true, false);
		
		LOG.info("Poll task created:"+monitor.getUrl());
		monitor.getPolls().add(this);
	}

	public int getInterval() {
		return interval;
	}

	public void setInterval(int interval) {
		this.interval = interval;
	}

	public String getTaskId() {
		return taskId;
	}

	public void setTaskId(String taskId) {
		this.taskId = taskId;
	}
}
