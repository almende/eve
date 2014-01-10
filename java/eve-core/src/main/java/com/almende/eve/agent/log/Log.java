package com.almende.eve.agent.log;

import java.io.IOException;
import java.io.Serializable;
import java.util.Date;

import com.almende.eve.rpc.jsonrpc.jackson.JOM;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Helper class to store logs
 */
@SuppressWarnings("serial")
public class Log implements Serializable {
	private Long	timestamp	= null;
	private String	agentId		= null;
	private String	event		= null;
	private String	params		= null;
	
	public Log(final String agentId, final String event, final Object params) throws IOException {
		init(agentId, event, params);
	}
	
	public final void init(final String agentId, final String event, final Object params)
			throws IOException {
		setTimestamp(new Date().getTime());
		setAgentId(agentId);
		setEvent(event);
		setParams(params);
	}
	
	public void setTimestamp(final Long timestamp) {
		this.timestamp = timestamp;
	}
	
	public Long getTimestamp() {
		return timestamp;
	}
	
	public void setAgentId(final String agentId) {
		this.agentId = agentId;
	}
	
	public String getAgentId() {
		return agentId;
	}
	
	public void setEvent(final String event) {
		this.event = event;
	}
	
	public String getEvent() {
		return event;
	}
	
	public void setParams(final Object params) throws IOException {
		final ObjectMapper mapper = JOM.getInstance();
		this.params = mapper.writeValueAsString(params);
	}
	
	public Object getParams() throws IOException {
		final ObjectMapper mapper = JOM.getInstance();
		return mapper.readValue(params, Object.class);
	}
}