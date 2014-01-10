/*
 * Copyright: Almende B.V. (2014), Rotterdam, The Netherlands
 * License: The Apache Software License, Version 2.0
 */
package com.almende.eve.agent.log;

import java.io.IOException;
import java.io.Serializable;
import java.util.Date;

import com.almende.eve.rpc.jsonrpc.jackson.JOM;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Helper class to store logs.
 */
@SuppressWarnings("serial")
public class Log implements Serializable {
	private Long	timestamp	= null;
	private String	agentId		= null;
	private String	event		= null;
	private String	params		= null;
	
	/**
	 * Instantiates a new log.
	 *
	 * @param agentId the agent id
	 * @param event the event
	 * @param params the params
	 * @throws IOException Signals that an I/O exception has occurred.
	 */
	public Log(final String agentId, final String event, final Object params) throws IOException {
		init(agentId, event, params);
	}
	
	/**
	 * Inits the.
	 *
	 * @param agentId the agent id
	 * @param event the event
	 * @param params the params
	 * @throws IOException Signals that an I/O exception has occurred.
	 */
	public final void init(final String agentId, final String event, final Object params)
			throws IOException {
		setTimestamp(new Date().getTime());
		setAgentId(agentId);
		setEvent(event);
		setParams(params);
	}
	
	/**
	 * Sets the timestamp.
	 *
	 * @param timestamp the new timestamp
	 */
	public void setTimestamp(final Long timestamp) {
		this.timestamp = timestamp;
	}
	
	/**
	 * Gets the timestamp.
	 *
	 * @return the timestamp
	 */
	public Long getTimestamp() {
		return timestamp;
	}
	
	/**
	 * Sets the agent id.
	 *
	 * @param agentId the new agent id
	 */
	public void setAgentId(final String agentId) {
		this.agentId = agentId;
	}
	
	/**
	 * Gets the agent id.
	 *
	 * @return the agent id
	 */
	public String getAgentId() {
		return agentId;
	}
	
	/**
	 * Sets the event.
	 *
	 * @param event the new event
	 */
	public void setEvent(final String event) {
		this.event = event;
	}
	
	/**
	 * Gets the event.
	 *
	 * @return the event
	 */
	public String getEvent() {
		return event;
	}
	
	/**
	 * Sets the params.
	 *
	 * @param params the new params
	 * @throws IOException Signals that an I/O exception has occurred.
	 */
	public void setParams(final Object params) throws IOException {
		final ObjectMapper mapper = JOM.getInstance();
		this.params = mapper.writeValueAsString(params);
	}
	
	/**
	 * Gets the params.
	 *
	 * @return the params
	 * @throws IOException Signals that an I/O exception has occurred.
	 */
	public Object getParams() throws IOException {
		final ObjectMapper mapper = JOM.getInstance();
		return mapper.readValue(params, Object.class);
	}
}