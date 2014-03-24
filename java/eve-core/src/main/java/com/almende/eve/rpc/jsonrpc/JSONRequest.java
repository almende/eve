/*
 * Copyright: Almende B.V. (2014), Rotterdam, The Netherlands
 * License: The Apache Software License, Version 2.0
 */
package com.almende.eve.rpc.jsonrpc;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.almende.eve.rpc.jsonrpc.jackson.JOM;
import com.almende.util.uuid.UUID;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * The Class JSONRequest.
 */
public final class JSONRequest extends JSONMessage {
	private static final Logger	LOG					= Logger.getLogger(JSONRequest.class
															.getCanonicalName());
	private static final long	serialVersionUID	= 1970046457233622444L;
	private ObjectNode			req					= JOM.createObjectNode();
	
	/**
	 * Instantiates a new jSON request.
	 */
	public JSONRequest() {
		init(null, null, null);
	}
	
	/**
	 * Instantiates a new jSON request.
	 *
	 * @param json the json
	 * @throws JSONRPCException the jSONRPC exception
	 * @throws IOException Signals that an I/O exception has occurred.
	 */
	public JSONRequest(final String json) throws JSONRPCException, IOException {
		final ObjectMapper mapper = JOM.getInstance();
		init(mapper.readTree(json));
	}
	
	/**
	 * Instantiates a new jSON request.
	 *
	 * @param request the request
	 * @throws JSONRPCException the jSONRPC exception
	 */
	public JSONRequest(final JsonNode request) throws JSONRPCException {
		init(request);
	}
	
	/**
	 * Inits the.
	 *
	 * @param request the request
	 * @throws JSONRPCException the jSONRPC exception
	 */
	public void init(final JsonNode request) throws JSONRPCException {
		if (request == null || request.isNull()) {
			throw new JSONRPCException(JSONRPCException.CODE.INVALID_REQUEST,
					"Request is null");
		}
		if (request.has(JSONRPC) && request.get(JSONRPC).isTextual()
				&& !request.get(JSONRPC).asText().equals(VERSION)) {
			throw new JSONRPCException(JSONRPCException.CODE.INVALID_REQUEST,
					"Value of member 'jsonrpc' is not equal to '2.0'");
		}
		if (!request.has(METHOD)) {
			throw new JSONRPCException(JSONRPCException.CODE.INVALID_REQUEST,
					"Member 'method' missing in request");
		}
		if (!(request.get(METHOD).isTextual())) {
			throw new JSONRPCException(JSONRPCException.CODE.INVALID_REQUEST,
					"Member 'method' is no String");
		}
		if (request.has(PARAMS) && !(request.get(PARAMS).isObject())) {
			throw new JSONRPCException(JSONRPCException.CODE.INVALID_REQUEST,
					"Member 'params' is no ObjectNode");
		}
		
		init(request.get(ID), request.get(METHOD).asText(),
				(ObjectNode) request.get(PARAMS));
	}
	
	/**
	 * Instantiates a new jSON request.
	 *
	 * @param method the method
	 * @param params the params
	 */
	public JSONRequest(final String method, final ObjectNode params) {
		init(null, method, params);
	}
	
	/**
	 * Instantiates a new jSON request.
	 *
	 * @param id the id
	 * @param method the method
	 * @param params the params
	 */
	public JSONRequest(final JsonNode id, final String method,
			final ObjectNode params) {
		init(id, method, params);
	}
	
	/**
	 * Instantiates a new jSON request.
	 *
	 * @param method the method
	 * @param params the params
	 * @param callbackUrl the callback url
	 * @param callbackMethod the callback method
	 */
	public JSONRequest(final String method, final ObjectNode params,
			final String callbackUrl, final String callbackMethod) {
		init(null, method, params);
		setCallback(callbackUrl, callbackMethod);
	}
	
	/**
	 * Instantiates a new jSON request.
	 *
	 * @param id the id
	 * @param method the method
	 * @param params the params
	 * @param callbackUrl the callback url
	 * @param callbackMethod the callback method
	 */
	public JSONRequest(final JsonNode id, final String method,
			final ObjectNode params, final String callbackUrl,
			final String callbackMethod) {
		init(id, method, params);
		setCallback(callbackUrl, callbackMethod);
	}
	
	/**
	 * Inits the.
	 *
	 * @param id the id
	 * @param method the method
	 * @param params the params
	 */
	private void init(final JsonNode id, final String method,
			final ObjectNode params) {
		setVersion();
		setId(id);
		setMethod(method);
		setParams(params);
	}
	
	/**
	 * Sets the id.
	 *
	 * @param id the new id
	 */
	public void setId(final JsonNode id) {
		if (id == null || id.isNull()) {
			req.put(ID, new UUID().toString());
		} else {
			req.put(ID, id);
		}
	}
	
	/* (non-Javadoc)
	 * @see com.almende.eve.rpc.jsonrpc.JSONMessage#getId()
	 */
	@Override
	public JsonNode getId() {
		return req.get(ID);
	}
	
	/**
	 * Sets the method.
	 *
	 * @param method the new method
	 */
	public void setMethod(final String method) {
		req.put(METHOD, method);
	}
	
	/**
	 * Gets the method.
	 *
	 * @return the method
	 */
	public String getMethod() {
		if (req.has(METHOD)) {
			return req.get(METHOD).asText();
		}
		return null;
	}
	
	/**
	 * Sets the params.
	 *
	 * @param params the new params
	 */
	public void setParams(final ObjectNode params) {
		final ObjectNode newParams = JOM.createObjectNode();
		if (params != null) {
			newParams.setAll(params);
		}
		req.put(PARAMS, newParams);
	}
	
	/**
	 * Gets the params.
	 *
	 * @return the params
	 */
	public ObjectNode getParams() {
		return (ObjectNode) req.get(PARAMS);
	}
	
	/**
	 * Put param.
	 *
	 * @param name the name
	 * @param value the value
	 */
	public void putParam(final String name, final Object value) {
		final ObjectMapper mapper = JOM.getInstance();
		req.with(PARAMS).put(name, mapper.convertValue(value, JsonNode.class));
	}
	
	/**
	 * Gets the param.
	 *
	 * @param name the name
	 * @return the param
	 */
	public Object getParam(final String name) {
		final ObjectMapper mapper = JOM.getInstance();
		final ObjectNode params = req.with(PARAMS);
		if (params.has(name)) {
			return mapper.convertValue(params.get(name), Object.class);
		}
		return null;
	}
	
	/**
	 * Checks for param.
	 *
	 * @param name the name
	 * @return the object
	 */
	public Object hasParam(final String name) {
		return req.get(PARAMS).has(name);
	}
	
	/**
	 * Sets the version.
	 */
	private void setVersion() {
		req.put(JSONRPC, VERSION);
	}
	
	/**
	 * Sets the callback.
	 *
	 * @param url the url
	 * @param method the method
	 */
	public void setCallback(final String url, final String method) {
		final ObjectNode callback = JOM.createObjectNode();
		callback.put(URL, url);
		callback.put(METHOD, method);
		req.put(CALLBACK, callback);
	}
	
	/**
	 * Gets the callback url.
	 *
	 * @return the callback url
	 */
	public String getCallbackUrl() {
		final JsonNode callback = req.get(CALLBACK);
		if (callback != null && callback.isObject() && callback.has(URL)
				&& callback.get(URL).isTextual()) {
			return callback.get(URL).asText();
		}
		return null;
	}
	
	/**
	 * Gets the callback method.
	 *
	 * @return the callback method
	 */
	public String getCallbackMethod() {
		final JsonNode callback = req.get(CALLBACK);
		if (callback != null && callback.isObject() && callback.has(METHOD)
				&& callback.get(METHOD).isTextual()) {
			return callback.get(METHOD).asText();
		}
		return null;
	}
	
	/**
	 * Checks for callback.
	 *
	 * @return true, if successful
	 */
	public boolean hasCallback() {
		return req.has(CALLBACK);
	}
	
	/**
	 * Gets the object node.
	 *
	 * @return the object node
	 */
	@JsonIgnore
	public ObjectNode getObjectNode() {
		return req;
	}
	
	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		final ObjectMapper mapper = JOM.getInstance();
		try {
			return mapper.writeValueAsString(req);
		} catch (final Exception e) {
			LOG.log(Level.WARNING, "", e);
		}
		return null;
	}
	
	/**
	 * Write object.
	 *
	 * @param out the out
	 * @throws IOException Signals that an I/O exception has occurred.
	 */
	private void writeObject(final java.io.ObjectOutputStream out) throws IOException {
		final ObjectMapper mapper = new ObjectMapper();
		mapper.configure(JsonGenerator.Feature.AUTO_CLOSE_TARGET, false);
		mapper.writeValue(out, req);
	}
	
	/**
	 * Read object.
	 *
	 * @param in the in
	 * @throws IOException Signals that an I/O exception has occurred.
	 * @throws ClassNotFoundException the class not found exception
	 */
	private void readObject(final java.io.ObjectInputStream in) throws IOException,
			ClassNotFoundException {
		final ObjectMapper mapper = new ObjectMapper();
		mapper.configure(JsonParser.Feature.AUTO_CLOSE_SOURCE, false);
		req = mapper.readValue(in, ObjectNode.class);
	}
	
}