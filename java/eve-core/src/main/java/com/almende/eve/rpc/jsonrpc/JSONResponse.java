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
	private final ObjectNode			resp				= JOM.createObjectNode();
	private static final Logger	LOG					= Logger.getLogger(JSONResponse.class
															.getName());
	
	public JSONResponse() {
		init(null, null, null);
	}
	
	public JSONResponse(final String json) throws JSONRPCException, IOException {
		final ObjectMapper mapper = JOM.getInstance();
		try {
			init(mapper.readValue(json, ObjectNode.class));
		} catch (final JsonParseException e) {
			LOG.warning("Failed to parse JSON: '" + json + "'");
			throw e;
		}
	}
	
	public JSONResponse(final ObjectNode response) throws JSONRPCException {
		init(response);
	}
	
	public JSONResponse(final Object result) {
		init(null, result, null);
	}
	
	public JSONResponse(final JsonNode id, final Object result) {
		init(id, result, null);
	}
	
	public JSONResponse(final JSONRPCException error) {
		init(null, null, error);
	}
	
	public JSONResponse(final JsonNode id, final JSONRPCException error) {
		init(id, null, error);
	}
	
	private void init(final ObjectNode response) throws JSONRPCException {
		if (response == null || response.isNull()) {
			throw new JSONRPCException(JSONRPCException.CODE.INVALID_REQUEST,
					"Response is null");
		}
		if (response.has(JSONRPC) && response.get(JSONRPC).isTextual()
				&& !response.get(JSONRPC).asText().equals(VERSION)) {
			throw new JSONRPCException(JSONRPCException.CODE.INVALID_REQUEST,
					"Value of member 'jsonrpc' must be '2.0'");
		}
		final boolean hasError = response.has(ERROR) && !response.get(ERROR).isNull();
		if (hasError && !(response.get(ERROR).isObject())) {
			throw new JSONRPCException(JSONRPCException.CODE.INVALID_REQUEST,
					"Member 'error' is no ObjectNode");
		}
		
		final JsonNode id = response.get(ID);
		final Object result = response.get(RESULT);
		JSONRPCException error = null;
		if (hasError) {
			error = new JSONRPCException((ObjectNode) response.get(ERROR));
		}
		
		init(id, result, error);
	}
	
	private void init(final JsonNode id, final Object result, final JSONRPCException error) {
		setVersion();
		setId(id);
		setResult(result);
		setError(error);
	}
	
	public void setId(final JsonNode id) {
		resp.put(ID, id);
	}
	
	@Override
	public JsonNode getId() {
		return resp.get(ID);
	}
	
	public void setResult(final Object result) {
		if (result != null) {
			final ObjectMapper mapper = JOM.getInstance();
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
	
	public void setError(final JSONRPCException error) {
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
			final ObjectNode error = (ObjectNode) resp.get(ERROR);
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
		final ObjectMapper mapper = JOM.getInstance();
		try {
			return mapper.writeValueAsString(resp);
		} catch (final Exception e) {
			LOG.log(Level.SEVERE, "Failed to stringify response.", e);
		}
		return null;
	}
}