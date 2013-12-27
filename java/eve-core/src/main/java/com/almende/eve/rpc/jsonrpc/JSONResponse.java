package com.almende.eve.rpc.jsonrpc;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.almende.eve.rpc.jsonrpc.jackson.JOM;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

public final class JSONResponse extends JSONMessage {
	private static final long	serialVersionUID	= 12392962249054051L;
	private ObjectNode			resp				= JOM.createObjectNode();
	private static final Logger	LOG					= Logger.getLogger(JSONResponse.class
															.getName());
	
	public JSONResponse() {
		init(null, null, null);
	}
	
	public JSONResponse(String json) throws JSONRPCException, IOException {
		ObjectMapper mapper = JOM.getInstance();
		try {
			init(mapper.readValue(json, ObjectNode.class));
		} catch (JsonParseException e) {
			LOG.warning("Failed to parse JSON: '" + json + "'");
			throw e;
		}
	}
	
	public JSONResponse(ObjectNode response) throws JSONRPCException {
		init(response);
	}
	
	public JSONResponse(Object result) {
		init(null, result, null);
	}
	
	public JSONResponse(JsonNode id, Object result) {
		init(id, result, null);
	}
	
	public JSONResponse(JSONRPCException error) {
		init(null, null, error);
	}
	
	public JSONResponse(JsonNode id, JSONRPCException error) {
		init(id, null, error);
	}
	
	private void init(ObjectNode response) throws JSONRPCException {
		if (response == null || response.isNull()) {
			throw new JSONRPCException(JSONRPCException.CODE.INVALID_REQUEST,
					"Response is null");
		}
		if (response.has(JSONRPC) && response.get(JSONRPC).isTextual()
				&& !response.get(JSONRPC).asText().equals(VERSION)) {
			throw new JSONRPCException(JSONRPCException.CODE.INVALID_REQUEST,
					"Value of member 'jsonrpc' must be '2.0'");
		}
		boolean hasError = response.has(ERROR) && !response.get(ERROR).isNull();
		if (hasError && !(response.get(ERROR).isObject())) {
			throw new JSONRPCException(JSONRPCException.CODE.INVALID_REQUEST,
					"Member 'error' is no ObjectNode");
		}
		
		JsonNode id = response.get(ID);
		Object result = response.get(RESULT);
		JSONRPCException error = null;
		if (hasError) {
			error = new JSONRPCException((ObjectNode) response.get(ERROR));
		}
		
		init(id, result, error);
	}
	
	private void init(JsonNode id, Object result, JSONRPCException error) {
		setVersion();
		setId(id);
		setResult(result);
		setError(error);
	}
	
	public void setId(JsonNode id) {
		resp.put(ID, id);
	}
	
	public JsonNode getId() {
		return resp.get(ID);
	}
	
	public void setResult(Object result) {
		if (result != null) {
			ObjectMapper mapper = JOM.getInstance();
			resp.put(RESULT, mapper.convertValue(result, JsonNode.class));
			setError(null);
		} else {
			if (resp.has(RESULT)) {
				resp.remove(RESULT);
			}
		}
	}
	
	public JsonNode getResult() {
		return resp.get(RESULT);
	}
	
	public void setError(JSONRPCException error) {
		if (error != null) {
			resp.put(ERROR, error.getObjectNode());
			setResult(null);
		} else {
			if (resp.has(ERROR)) {
				resp.remove(ERROR);
			}
		}
	}
	
	public JSONRPCException getError() {
		if (resp.has(ERROR)) {
			ObjectNode error = (ObjectNode) resp.get(ERROR);
			return new JSONRPCException(error);
		} else {
			return null;
		}
	}
	
	private void setVersion() {
		resp.put(JSONRPC, VERSION);
	}
	
	public ObjectNode getObjectNode() {
		return resp;
	}
	
	@Override
	public String toString() {
		ObjectMapper mapper = JOM.getInstance();
		try {
			return mapper.writeValueAsString(resp);
		} catch (Exception e) {
			LOG.log(Level.SEVERE, "Failed to stringify response.", e);
		}
		return null;
	}
}