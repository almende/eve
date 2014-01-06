package com.almende.eve.monitor.impl;

import java.util.logging.Logger;

import com.almende.eve.agent.AgentInterface;
import com.almende.eve.monitor.PollInterface;
import com.almende.eve.monitor.ResultMonitor;
import com.almende.eve.rpc.jsonrpc.JSONRequest;
import com.almende.eve.rpc.jsonrpc.jackson.JOM;
import com.fasterxml.jackson.databind.node.ObjectNode;

public class Poll implements PollInterface {
	private static final long	serialVersionUID	= 1521097261949700084L;
	private static final Logger	LOG					= Logger.getLogger(Poll.class
															.getCanonicalName());
	
	private int					interval;
	private String				taskId				= null;
	
	public Poll(int interval) {
		this.interval = interval;
	};
	
	public Poll() {
	}
	
	@Override
	public PollInterface onInterval(int interval) {
		this.interval = interval;
		return this;
	}
	
	@Override
	public void cancel(ResultMonitor monitor, AgentInterface agent) {
		if (taskId != null && agent.getScheduler() != null) {
			agent.getScheduler().cancelTask(taskId);
		}
	}
	
	@Override
	public void init(ResultMonitor monitor, AgentInterface agent) {
		ObjectNode params = JOM.createObjectNode();
		params.put("monitorId", monitor.getId());
		JSONRequest request = new JSONRequest("monitor.doPoll", params);
		
		// Try to cancel any protential existing tasks.
		cancel(monitor, agent);
		
		taskId = agent.getScheduler()
				.createTask(request, interval, true, false);
		
		LOG.info("Poll task created:" + monitor.getUrl());
		monitor.getPolls().add(this);
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
	public String getTaskId() {
		return taskId;
	}
	
	@Override
	public void setTaskId(String taskId) {
		this.taskId = taskId;
	}
}
