package com.almende.eve.agent.log;

import java.io.IOException;
import java.io.Serializable;
import java.util.Date;

import com.almende.eve.json.jackson.JOM;
import com.fasterxml.jackson.core.JsonGenerationException;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;


/**
 * Helper class to store logs
 */
@SuppressWarnings("serial")
public class Log implements Serializable {
	private Long timestamp = null;
	private String agentId = null;
	private String event = null;
	private String params = null;

	public Log(String agentId, String event, ObjectNode params) 
			throws JsonGenerationException, JsonMappingException, IOException {
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

	public void setParams(ObjectNode params) 
			throws JsonGenerationException, JsonMappingException, IOException {
		ObjectMapper mapper = JOM.getInstance();
		this.params = mapper.writeValueAsString(params);
	}

	public ObjectNode getParams() 
			throws JsonParseException, JsonMappingException, IOException {
		ObjectMapper mapper = JOM.getInstance();
		return mapper.readValue(params, ObjectNode.class);
	}
}