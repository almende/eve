package com.almende.eve.monitor;

import com.almende.eve.agent.AgentInterface;

public interface PollInterface extends ResultMonitorConfigType {
	
	void setTaskId(String taskId);
	
	String getTaskId();
	
	void setInterval(int interval);
	
	int getInterval();
	
	void init(ResultMonitor monitor, AgentInterface agent);
	
	void cancel(ResultMonitor monitor, AgentInterface agent);
	
	PollInterface onInterval(int interval);
	
}
