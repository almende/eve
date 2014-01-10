/*
 * Copyright: Almende B.V. (2014), Rotterdam, The Netherlands
 * License: The Apache Software License, Version 2.0
 */
package com.almende.eve.state.log;

import java.io.Serializable;

/**
 * The Class AgentDetailRecord.
 */
@SuppressWarnings("serial")
public class AgentDetailRecord implements Serializable {
	/** The agent url. */
	private String	agent;
	/** The agent class name */
	private String	type;
	/** The method name. */
	private String	method;
	/** The timestamp in ISO datetime format */
	private String	timestamp;
	/** duration of execution of the call in ms */
	private Long	duration;
	/**
	 * true if call was succesfull, false if an
	 * exception was thrown
	 */
	private Boolean	success;
	
	/**
	 * Instantiates a new agent detail record.
	 */
	public AgentDetailRecord() {
	}
	
	/**
	 * Instantiates a new agent detail record.
	 * 
	 * @param agent
	 *            the agent
	 * @param type
	 *            the type
	 * @param method
	 *            the method
	 * @param timestamp
	 *            the timestamp
	 * @param duration
	 *            the duration
	 * @param success
	 *            the success
	 */
	public AgentDetailRecord(final String agent, final String type,
			final String method, final String timestamp, final Long duration,
			final Boolean success) {
		init(agent, type, method, timestamp, duration, success);
	}
	
	/**
	 * Inits the.
	 * 
	 * @param agent
	 *            the agent
	 * @param type
	 *            the type
	 * @param method
	 *            the method
	 * @param timestamp
	 *            the timestamp
	 * @param duration
	 *            the duration
	 * @param success
	 *            the success
	 */
	public final void init(final String agent, final String type,
			final String method, final String timestamp, final Long duration,
			final Boolean success) {
		setAgent(agent);
		setType(type);
		setMethod(method);
		setTimestamp(timestamp);
		setDuration(duration);
		setSuccess(success);
	}
	
	/**
	 * Gets the agent.
	 * 
	 * @return the agent
	 */
	public String getAgent() {
		return agent;
	}
	
	/**
	 * Sets the agent.
	 * 
	 * @param agent
	 *            the new agent
	 */
	public void setAgent(final String agent) {
		this.agent = agent;
	}
	
	/**
	 * Gets the type.
	 * 
	 * @return the type
	 */
	public String getType() {
		return type;
	}
	
	/**
	 * Sets the type.
	 * 
	 * @param type
	 *            the new type
	 */
	public void setType(final String type) {
		this.type = type;
	}
	
	/**
	 * Gets the method.
	 * 
	 * @return the method
	 */
	public String getMethod() {
		return method;
	}
	
	/**
	 * Sets the method.
	 * 
	 * @param method
	 *            the new method
	 */
	public void setMethod(final String method) {
		this.method = method;
	}
	
	/**
	 * Gets the timestamp.
	 * 
	 * @return the timestamp
	 */
	public String getTimestamp() {
		return timestamp;
	}
	
	/**
	 * Sets the timestamp.
	 * 
	 * @param timestamp
	 *            the new timestamp
	 */
	public void setTimestamp(final String timestamp) {
		this.timestamp = timestamp;
	}
	
	/**
	 * Gets the duration.
	 * 
	 * @return the duration
	 */
	public Long getDuration() {
		return duration;
	}
	
	/**
	 * Sets the duration.
	 * 
	 * @param duration
	 *            the new duration
	 */
	public void setDuration(final Long duration) {
		this.duration = duration;
	}
	
	/**
	 * Sets the success.
	 * 
	 * @param success
	 *            the new success
	 */
	public void setSuccess(final Boolean success) {
		this.success = success;
	}
	
	/**
	 * Gets the success.
	 * 
	 * @return the success
	 */
	public Boolean getSuccess() {
		return success;
	}
	
}
