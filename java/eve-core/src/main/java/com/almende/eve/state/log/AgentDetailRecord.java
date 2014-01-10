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
	
	public AgentDetailRecord(final String agent, final String type, final String method,
			final String timestamp, final Long duration, final Boolean success) {
		init(agent, type, method, timestamp, duration, success);
	}
	
	public final void init(final String agent, final String type, final String method,
			final String timestamp, final Long duration, final Boolean success) {
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
	
	public void setAgent(final String agent) {
		this.agent = agent;
	}
	
	public String getType() {
		return type;
	}
	
	public void setType(final String type) {
		this.type = type;
	}
	
	public String getMethod() {
		return method;
	}
	
	public void setMethod(final String method) {
		this.method = method;
	}
	
	public String getTimestamp() {
		return timestamp;
	}
	
	public void setTimestamp(final String timestamp) {
		this.timestamp = timestamp;
	}
	
	public Long getDuration() {
		return duration;
	}
	
	public void setDuration(final Long duration) {
		this.duration = duration;
	}
	
	public void setSuccess(final Boolean success) {
		this.success = success;
	}
	
	public Boolean getSuccess() {
		return success;
	}
	
}
