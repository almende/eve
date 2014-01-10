/*
 * Copyright: Almende B.V. (2014), Rotterdam, The Netherlands
 * License: The Apache Software License, Version 2.0
 */
package com.almende.eve.monitor;

import java.util.logging.Logger;

import com.almende.eve.agent.AgentInterface;
import com.almende.eve.rpc.jsonrpc.JSONRequest;
import com.almende.eve.rpc.jsonrpc.jackson.JOM;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * The Class Poll.
 */
public class Poll implements ResultMonitorConfigType {
	private static final long	serialVersionUID	= 1521097261949700084L;
	private static final Logger	LOG					= Logger.getLogger(Poll.class
															.getCanonicalName());
	private int					interval;
	private String				taskId				= null;
	
	/**
	 * Instantiates a new poll.
	 * 
	 * @param interval
	 *            the interval
	 */
	public Poll(final int interval) {
		this.interval = interval;
	};
	
	/**
	 * Instantiates a new poll.
	 */
	public Poll() {
	}
	
	/**
	 * On interval.
	 * 
	 * @param interval
	 *            the interval
	 * @return the poll
	 */
	public Poll onInterval(final int interval) {
		this.interval = interval;
		return this;
	}
	
	/**
	 * Cancel.
	 * 
	 * @param monitor
	 *            the monitor
	 * @param agent
	 *            the agent
	 */
	public void cancel(final ResultMonitor monitor, final AgentInterface agent) {
		if (taskId != null && agent.getScheduler() != null) {
			agent.getScheduler().cancelTask(taskId);
		}
	}
	
	/**
	 * Inits the.
	 * 
	 * @param monitor
	 *            the monitor
	 * @param agent
	 *            the agent
	 */
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
	
	/**
	 * Gets the interval.
	 * 
	 * @return the interval
	 */
	public int getInterval() {
		return interval;
	}
	
	/**
	 * Sets the interval.
	 * 
	 * @param interval
	 *            the new interval
	 */
	public void setInterval(final int interval) {
		this.interval = interval;
	}
	
	/**
	 * Gets the task id.
	 * 
	 * @return the task id
	 */
	public String getTaskId() {
		return taskId;
	}
	
	/**
	 * Sets the task id.
	 * 
	 * @param taskId
	 *            the new task id
	 */
	public void setTaskId(final String taskId) {
		this.taskId = taskId;
	}
}
