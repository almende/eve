package com.almende.eve.context.log;

import java.io.Serializable;

@SuppressWarnings("serial")
public class AgentDetailRecord implements Serializable {
	public AgentDetailRecord () {}
	
	public AgentDetailRecord (String agent, String type, 
			String method, String timestamp, Long duration, Boolean success) {
		setAgent(agent);
		setType(type);
		setMethod(method);
		setTimestamp(timestamp);
		setDuration(duration);
		setSuccess(success);
	}
	
	public String getAgent() {
		return agent;
	}
	
	public void setAgent(String agent) {
		this.agent = agent;
	}
	
	public String getType() {
		return type;
	}
	
	public void setType(String type) {
		this.type = type;
	}
	
	public String getMethod() {
		return method;
	}
	
	public void setMethod(String method) {
		this.method = method;
	}
	
	public String getTimestamp() {
		return timestamp;
	}
	
	public void setTimestamp(String timestamp) {
		this.timestamp = timestamp;
	}
	
	public Long getDuration() {
		return duration;
	}
	
	public void setDuration(Long duration) {
		this.duration = duration;
	}
	
	public void setSuccess(Boolean success) {
		this.success = success;
	}
	
	public Boolean getSuccess() {
		return success;
	}

	private String agent;     // agent url
	private String type;      // agent class name
	private String method;    // method name
	private String timestamp; // timestamp in ISO datetime format
	private Long duration;    // duration of execution of the call in ms	
	private Boolean success;  // true if call was succesfull, false if an 
	                           // exception was thrown
}
