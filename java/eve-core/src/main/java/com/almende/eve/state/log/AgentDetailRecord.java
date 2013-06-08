package com.almende.eve.state.log;

import java.io.Serializable;

@SuppressWarnings("serial")
public class AgentDetailRecord implements Serializable {
	// agent url
	private String	agent;
	// agent class name
	private String	type;
	// method name
	private String	method;
	// timestamp in ISO datetime format
	private String	timestamp;
	// duration of execution of the call in ms
	private Long	duration;
	// true if call was succesfull, false if an
	// exception was thrown
	private Boolean	success;

	public AgentDetailRecord() {
	}
	
	public AgentDetailRecord(String agent, String type, String method,
			String timestamp, Long duration, Boolean success) {
		init(agent, type, method, timestamp, duration, success);
	}
	
	public final void init(String agent, String type, String method,
			String timestamp, Long duration, Boolean success) {
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
	
}
