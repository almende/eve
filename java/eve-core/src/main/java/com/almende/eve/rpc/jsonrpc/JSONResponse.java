/*
 * Copyright: Almende B.V. (2014), Rotterdam, The Netherlands
 * License: The Apache Software License, Version 2.0
 */
package com.almende.eve.rpc.jsonrpc;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.almende.eve.rpc.jsonrpc.jackson.JOM;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * The Class JSONResponse.
 */
public final class JSONResponse extends JSONMessage {
	private static final long	serialVersionUID	= 12392962249054051L;
	private final ObjectNode			resp				= JOM.createObjectNode();
	private static final Logger	LOG					= Logger.getLogger(JSONResponse.class
															.getName());
	
	/**
	 * Instantiates a new jSON response.
	 */
	public JSONResponse() {
		init(null, null, null);
	}
	
	/**
	 * Instantiates a new jSON response.
	 *
	 * @param json the json
	 * @throws JSONRPCException the jSONRPC exception
	 * @throws IOException Signals that an I/O exception has occurred.
	 */
	public JSONResponse(final String json) throws JSONRPCException, IOException {
		final ObjectMapper mapper = JOM.getInstance();
		try {
			init(mapper.readValue(json, ObjectNode.class));
		} catch (final JsonParseException e) {
			LOG.warning("Failed to parse JSON: '" + json + "'");
			throw e;
		}
	}
	
	/**
	 * Instantiates a new jSON response.
	 *
	 * @param response the response
	 * @throws JSONRPCException the jSONRPC exception
	 */
	public JSONResponse(final ObjectNode response) throws JSONRPCException {
		init(response);
	}
	
	/**
	 * Instantiates a new jSON response.
	 *
	 * @param result the result
	 */
	public JSONResponse(final Object result) {
		init(null, result, null);
	}
	
	/**
	 * Instantiates a new jSON response.
	 *
	 * @param id the id
	 * @param result the result
	 */
	public JSONResponse(final JsonNode id, final Object result) {
		init(id, result, null);
	}
	
	/**
	 * Instantiates a new jSON response.
	 *
	 * @param error the error
	 */
	public JSONResponse(final JSONRPCException error) {
		init(null, null, error);
	}
	
	/**
	 * Instantiates a new jSON response.
	 *
	 * @param id the id
	 * @param error the error
	 */
	public JSONResponse(final JsonNode id, final JSONRPCException error) {
		init(id, null, error);
	}
	
	/**
	 * Inits the.
	 *
	 * @param response the response
	 * @throws JSONRPCException the jSONRPC exception
	 */
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
	
	/**
	 * Inits the.
	 *
	 * @param id the id
	 * @param result the result
	 * @param error the error
	 */
	private void init(final JsonNode id, final Object result, final JSONRPCException error) {
		setVersion();
		setId(id);
		setResult(result);
		setError(error);
	}
	
	/**
	 * Sets the id.
	 *
	 * @param id the new id
	 */
	public void setId(final JsonNode id) {
		resp.put(ID, id);
	}
	
	/* (non-Javadoc)
	 * @see com.almende.eve.rpc.jsonrpc.JSONMessage#getId()
	 */
	@Override
	public JsonNode getId() {
		return resp.get(ID);
	}
	
	/**
	 * Sets the result.
	 *
	 * @param result the new result
	 */
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
	
	/**
	 * Gets the result.
	 *
	 * @return the result
	 */
	public JsonNode getResult() {
		return resp.get(RESULT);
	}
	
	/**
	 * Sets the error.
	 *
	 * @param error the new error
	 */
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
	
	/**
	 * Gets the error.
	 *
	 * @return the error
	 */
	public JSONRPCException getError() {
		if (resp.has(ERROR)) {
			final ObjectNode error = (ObjectNode) resp.get(ERROR);
			return new JSONRPCException(error);
		} else {
			return null;
		}
	}
	
	/**
	 * Sets the version.
	 */
	private void setVersion() {
		resp.put(JSONRPC, VERSION);
	}
	
	/**
	 * Gets the object node.
	 *
	 * @return the object node
	 */
	public ObjectNode getObjectNode() {
		return resp;
	}
	
	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
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