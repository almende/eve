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
	private Long timestamp = null;
	private String agentId = null;
	private String event = null;
	private String params = null;

	public Log(String agentId, String event, Object params) 
			throws IOException {
		this.setTimestamp(new Date().getTime());
		this.setAgentId(agentId);
		this.setEvent(event);
		this.setParams(params);
	}

	public void setTimestamp(Long timestamp) {
		this.timestamp = timestamp;
	}

	public Long getTimestamp() {
		return timestamp;
	}

	public void setAgentId(String agentId) {
		this.agentId = agentId;
	}

	public String getAgentId() {
		return agentId;
	}

	public void setEvent(String event) {
		this.event = event;
	}

	public String getEvent() {
		return event;
	}

	public void setParams(Object params) 
			throws IOException {
		ObjectMapper mapper = JOM.getInstance();
		this.params = mapper.writeValueAsString(params);
	}

	public Object getParams() 
			throws IOException {
		ObjectMapper mapper = JOM.getInstance();
		return mapper.readValue(params, Object.class);
	}
}