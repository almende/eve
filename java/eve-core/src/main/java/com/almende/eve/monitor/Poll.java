package com.almende.eve.monitor;

import java.util.logging.Logger;

import com.almende.eve.agent.AgentInterface;
import com.almende.eve.rpc.jsonrpc.JSONRequest;
import com.almende.eve.rpc.jsonrpc.jackson.JOM;
import com.fasterxml.jackson.databind.node.ObjectNode;

public class Poll implements ResultMonitorConfigType {
	private static final long	serialVersionUID	= 1521097261949700084L;
	private static final Logger	LOG					= Logger.getLogger(Poll.class
															.getCanonicalName());
	
	private int					interval;
	private String				taskId				= null;
	
	public Poll(final int interval) {
		this.interval = interval;
	};
	
	public Poll() {
	}
	
	public Poll onInterval(final int interval) {
		this.interval = interval;
		return this;
	}
	
	public void cancel(final ResultMonitor monitor, final AgentInterface agent) {
		if (taskId != null && agent.getScheduler() != null) {
			agent.getScheduler().cancelTask(taskId);
		}
	}
	
	public void init(final ResultMonitor monitor, final AgentInterface agent) {
		final ObjectNode params = JOM.createObjectNode();
		params.put("monitorId", monitor.getId());
		final JSONRequest request = new JSONRequest("monitor.doPoll", params);
		
		// Try to cancel any protential existing tasks.
		cancel(monitor, agent);
		
		taskId = agent.getScheduler()
				.createTask(request, interval, true, false);
		
		LOG.info("Poll task created:" + monitor.getUrl());
		monitor.getPolls().add(this);
	}
	
	public int getInterval() {
		return interval;
	}
	
	public void setInterval(final int interval) {
		this.interval = interval;
	}
	
	public String getTaskId() {
		return taskId;
	}
	
	public void setTaskId(final String taskId) {
		this.taskId = taskId;
	}
}
