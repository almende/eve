package com.almende.eve.rpc.jsonrpc;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.almende.eve.rpc.jsonrpc.jackson.JOM;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

public class JSONResponse {
	protected ObjectNode		resp	= JOM.createObjectNode();
	private static final Logger	LOG	= Logger.getLogger(JSONResponse.class
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
	
	public JSONResponse(Object id, Object result) {
		init(id, result, null);
	}
	
	public JSONResponse(JSONRPCException error) {
		init(null, null, error);
	}
	
	public JSONResponse(Object id, JSONRPCException error) {
		init(id, null, error);
	}
	
	private final void init(ObjectNode response) throws JSONRPCException {
		if (response == null || response.isNull()) {
			throw new JSONRPCException(JSONRPCException.CODE.INVALID_REQUEST,
					"Response is null");
		}
		if (response.has("jsonrpc") && response.get("jsonrpc").isTextual() && !response.get("jsonrpc").asText().equals("2.0")) {
				throw new JSONRPCException(
						JSONRPCException.CODE.INVALID_REQUEST,
						"Value of member 'jsonrpc' must be '2.0'");
		}
		boolean hasError = response.has("error")
				&& !response.get("error").isNull();
		if (hasError && !(response.get("error").isObject())) {
				throw new JSONRPCException(
						JSONRPCException.CODE.INVALID_REQUEST,
						"Member 'error' is no ObjectNode");
		}
		
		Object id = response.get("id");
		Object result = response.get("result");
		JSONRPCException error = null;
		if (hasError) {
			error = new JSONRPCException((ObjectNode) response.get("error"));
		}
		
		init(id, result, error);
	}
	
	private final void init(Object id, Object result, JSONRPCException error) {
		setVersion();
		setId(id);
		setResult(result);
		setError(error);
	}
	
	public final void setId(Object id) {
		ObjectMapper mapper = JOM.getInstance();
		resp.put("id", mapper.convertValue(id, JsonNode.class));
	}
	
	public Object getId() {
		ObjectMapper mapper = JOM.getInstance();
		return mapper.convertValue(resp.get("id"), JsonNode.class);
	}
	
	public final void setResult(Object result) {
		if (result != null) {
			ObjectMapper mapper = JOM.getInstance();
			resp.put("result", mapper.convertValue(result, JsonNode.class));
			setError(null);
		} else {
			if (resp.has("result")) {
				resp.remove("result");
			}
		}
	}
	
	public JsonNode getResult() {
		return resp.get("result");
	}
	
	public final void setError(JSONRPCException error) {
		if (error != null) {
			resp.put("error", error.getObjectNode());
			setResult(null);
		} else {
			if (resp.has("error")) {
				resp.remove("error");
			}
		}
	}
	
	public JSONRPCException getError() {
		if (resp.has("error")) {
			ObjectNode error = (ObjectNode) resp.get("error");
			return new JSONRPCException(error);
		} else {
			return null;
		}
	}
	
	/*
	 * TODO: gives issues with Jackson
	 * public boolean isError() {
	 * return (resp.get("error") != null);
	 * }
	 */
	
	private final void setVersion() {
		resp.put("jsonrpc", "2.0");
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