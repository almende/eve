package com.almende.eve.scheduler.google;

import com.google.code.twig.annotation.Id;

/**
 * Helper class to persist and search all running tasks.
 */
public class AppEngineTask {
	public AppEngineTask () {}
	
	public AppEngineTask (String taskId, String agentId, String timestamp) {
		this.taskId = taskId;
		this.agentId = agentId;
		this.timestamp = timestamp;
	}
	
	public String getTaskId() {
		return taskId;
	}
	public void setTaskId(String taskId) {
		this.taskId = taskId;
	}
	
	public String getAgentId() {
		return agentId;
	}
	public void setAgentId(String agentId) {
		this.agentId = agentId;
	}

	public String getTimestamp() {
		return timestamp;
	}
	public void setTimestamp(String timestamp) {
		this.timestamp = timestamp;
	}

	@Id private String taskId = null;
	private String agentId = null;
	private String timestamp = null;
}
