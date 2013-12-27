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

public final class JSONRequest extends JSONMessage {
	private static final Logger	LOG					= Logger.getLogger(JSONRequest.class
															.getCanonicalName());
	private static final long	serialVersionUID	= 1970046457233622444L;
	private ObjectNode			req					= JOM.createObjectNode();
	
	public enum VERSION {
		ONE, TWO
	};
	
	public JSONRequest() {
		init(null, null, null);
	}
	
	public JSONRequest(String json) throws JSONRPCException, IOException {
		ObjectMapper mapper = JOM.getInstance();
		init(mapper.readValue(json, ObjectNode.class));
	}
	
	public JSONRequest(ObjectNode request) throws JSONRPCException {
		init(request);
	}
	
	public void init(ObjectNode request) throws JSONRPCException {
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
	
	public JSONRequest(String method, ObjectNode params) {
		init(null, method, params);
	}
	
	public JSONRequest(final JsonNode id, final String method,
			final ObjectNode params) {
		init(id, method, params);
	}
	
	public JSONRequest(final String method, final ObjectNode params,
			final String callbackUrl, final String callbackMethod) {
		init(null, method, params);
		setCallback(callbackUrl, callbackMethod);
	}
	
	public JSONRequest(final JsonNode id, final String method,
			final ObjectNode params, final String callbackUrl,
			final String callbackMethod) {
		init(id, method, params);
		setCallback(callbackUrl, callbackMethod);
	}
	
	private void init(final JsonNode id, final String method,
			final ObjectNode params) {
		setVersion();
		setId(id);
		setMethod(method);
		setParams(params);
	}
	
	public void setId(String id) {
		if (id == null) {
			id = new UUID().toString();
		}
		req.put(ID, id);
	}
	
	public void setId(JsonNode id) {
		if (id == null || id.isNull()) {
			setId(new UUID().toString());
		} else {
			req.put(ID, id);
		}
	}
	
	public JsonNode getId() {
		return req.get(ID);
	}
	
	public void setMethod(String method) {
		req.put(METHOD, method);
	}
	
	public String getMethod() {
		if (req.has(METHOD)) {
			return req.get(METHOD).asText();
		}
		return null;
	}
	
	public void setParams(ObjectNode params) {
		ObjectNode newParams = JOM.createObjectNode();
		if (params != null) {
			newParams.setAll(params);
		}
		req.put(PARAMS, newParams);
	}
	
	public ObjectNode getParams() {
		return (ObjectNode) req.get(PARAMS);
	}
	
	public void putParam(String name, Object value) {
		ObjectMapper mapper = JOM.getInstance();
		req.with(PARAMS).put(name, mapper.convertValue(value, JsonNode.class));
	}
	
	public Object getParam(String name) {
		ObjectMapper mapper = JOM.getInstance();
		ObjectNode params = req.with(PARAMS);
		if (params.has(name)) {
			return mapper.convertValue(params.get(name), Object.class);
		}
		return null;
	}
	
	public Object hasParam(String name) {
		return req.get(PARAMS).has(name);
	}
	
	private void setVersion() {
		req.put(JSONRPC, VERSION);
	}
	
	public void setCallback(String url, String method) {
		ObjectNode callback = JOM.createObjectNode();
		callback.put(URL, url);
		callback.put(METHOD, method);
		req.put(CALLBACK, callback);
	}
	
	public String getCallbackUrl() {
		JsonNode callback = req.get(CALLBACK);
		if (callback != null && callback.isObject() && callback.has(URL)
				&& callback.get(URL).isTextual()) {
			return callback.get(URL).asText();
		}
		return null;
	}
	
	public String getCallbackMethod() {
		JsonNode callback = req.get(CALLBACK);
		if (callback != null && callback.isObject() && callback.has(METHOD)
				&& callback.get(METHOD).isTextual()) {
			return callback.get(METHOD).asText();
		}
		return null;
	}
	
	public boolean hasCallback() {
		return req.has(CALLBACK);
	}
	
	@JsonIgnore
	public ObjectNode getObjectNode() {
		return req;
	}
	
	@Override
	public String toString() {
		ObjectMapper mapper = JOM.getInstance();
		try {
			return mapper.writeValueAsString(req);
		} catch (Exception e) {
			LOG.log(Level.WARNING, "", e);
		}
		return null;
	}
	
	private void writeObject(java.io.ObjectOutputStream out) throws IOException {
		ObjectMapper mapper = new ObjectMapper();
		mapper.configure(JsonGenerator.Feature.AUTO_CLOSE_TARGET, false);
		mapper.writeValue(out, req);
	}
	
	private void readObject(java.io.ObjectInputStream in) throws IOException,
			ClassNotFoundException {
		ObjectMapper mapper = new ObjectMapper();
		mapper.configure(JsonParser.Feature.AUTO_CLOSE_SOURCE, false);
		req = mapper.readValue(in, ObjectNode.class);
	}
	
}