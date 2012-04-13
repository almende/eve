package com.almende.eve.json;

import java.io.IOException;

import com.almende.eve.json.jackson.JOM;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

public class JSONRequest {
	protected ObjectNode req = JOM.createObjectNode();

	public enum VERSION {ONE, TWO};
	
	public JSONRequest () {
		init(null, null, null);
	}

	public JSONRequest (String json) throws JSONRPCException, 
			JsonParseException, JsonMappingException, IOException {
		ObjectMapper mapper = JOM.getInstance();
		init(mapper.readValue(json, ObjectNode.class));
	}
	
	public JSONRequest (ObjectNode request) throws JSONRPCException {
		init(request);
	}
	
	public void init (ObjectNode request) throws JSONRPCException {
		if (request == null || request.isNull()) {
			throw new JSONRPCException(JSONRPCException.CODE.INVALID_REQUEST, 
				"Request is null");
		}
		if (request.has("jsonrpc") && request.get("jsonrpc").isTextual()) {
			if (!request.get("jsonrpc").asText().equals("2.0")) {
				throw new JSONRPCException(JSONRPCException.CODE.INVALID_REQUEST, 
						"Value of member 'jsonrpc' is not equal to '2.0'");
			}
		}
		if (!request.has("method")) {
			throw new JSONRPCException(JSONRPCException.CODE.INVALID_REQUEST, 
				"Member 'method' missing in request");
		}
		if (!(request.get("method").isTextual())) {
			throw new JSONRPCException(JSONRPCException.CODE.INVALID_REQUEST, 
				"Member 'method' is no String");
		}
		if (!request.has("params")) {
			throw new JSONRPCException(JSONRPCException.CODE.INVALID_REQUEST, 
				"Member 'params' missing in request");
		}
		if (!(request.get("params").isObject())) {
			throw new JSONRPCException(JSONRPCException.CODE.INVALID_REQUEST, 
				"Member 'params' is no ObjectNode");
		}
		
		init (request.get("id").asInt(), request.get("method").asText(), 
				(ObjectNode)request.get("params"));
	}

	/*
	public JSONRequest (String method) {
		init (null, method, null);
	}
	 */
	
	public JSONRequest (String method, ObjectNode params) {
		init (null, method, params);
	}

	public JSONRequest (Object id, String method, ObjectNode params) {
		init (id, method, params);
	}

	public JSONRequest (String method, ObjectNode params,
			String callbackUrl, String callbackMethod) {
		init (null, method, params);
		setCallback(callbackUrl, callbackMethod);
	}

	public JSONRequest (Object id, String method, ObjectNode params,
			String callbackUrl, String callbackMethod) {
		init (id, method, params);
		setCallback(callbackUrl, callbackMethod);
	}

	private void init(Object id, String method, ObjectNode params) {
		setVersion();
		setId(id);
		setMethod(method);
		setParams(params);
	}
	
	public void setId(Object id) {
		ObjectMapper mapper = JOM.getInstance();
		req.put("id", mapper.convertValue(id, JsonNode.class));
	}
	
	public Object getId() {
		ObjectMapper mapper = JOM.getInstance();
		return mapper.convertValue(req.get("id"), Object.class);
	}

	public void setMethod(String method) {
		req.put("method", method);
	}

	public String getMethod() {
		if (req.has("method")) {
			return req.get("method").asText();
		}
		return null;
	}

	public void setParams(ObjectNode params) {
		req.put("params", params != null ? params : JOM.createObjectNode());
	}
	
	public ObjectNode getParams() {
		return (ObjectNode) req.get("params");
	}
	
	public void putParam(String name, Object value) {
		ObjectMapper mapper = JOM.getInstance();
		req.with("params").put(name, mapper.convertValue(value, JsonNode.class));
	}

	public Object getParam(String name) {
		ObjectMapper mapper = JOM.getInstance();
		ObjectNode params = req.with("params");
		if (params.has(name)) {
			return mapper.convertValue(params.get(name), Object.class);
		}
		return null;
	}
	
	public Object hasParam(String name) {
		return req.get("params").has(name);
	}	

	private void setVersion() {
		req.put("jsonrpc", "2.0");
	}

	public void setCallback(String url, String method) {
		ObjectNode callback = JOM.createObjectNode();
		callback.put("url", url);
		callback.put("method", method);
		req.put("callback", callback);
	}

	public String getCallbackUrl() {
		JsonNode callback = req.get("callback");
		if (callback != null && callback.isObject() && callback.has("url")
				&& callback.get("url").isTextual()) {
			return callback.get("url").asText();
		}
		return null;
	}

	public String getCallbackMethod() {
		JsonNode callback = req.get("callback");
		if (callback != null && callback.isObject() && 
				callback.has("method") && 
				callback.get("method").isTextual()) {
			return callback.get("method").asText();
		}
		return null;
	}

	public boolean hasCallback() {
		return req.has("callback");
	}
	
	public ObjectNode getObjectNode() {
		return req;
	}
	
	@Override
	public String toString() {
		ObjectMapper mapper = JOM.getInstance();
		try {
			return mapper.writeValueAsString(req);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}
}