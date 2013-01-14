package com.almende.eve.json;

import java.io.IOException;

import com.almende.eve.json.jackson.JOM;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

public class JSONResponse {
	protected ObjectNode resp = JOM.createObjectNode();

	public JSONResponse () {
		init(null, null, null);
	}

	public JSONResponse (String json) 
			throws JSONRPCException, JsonParseException, JsonMappingException, 
			IOException {
		ObjectMapper mapper = JOM.getInstance();
		init(mapper.readValue(json, ObjectNode.class));
	}

	public JSONResponse (ObjectNode response) throws JSONRPCException {
		init(response);
	}

	public JSONResponse (Object result) {
		init(null, result, null);
	}

	public JSONResponse (Object id, Object result) {
		init(id, result, null);
	}

	public JSONResponse (JSONRPCException error) {
		init(null, null, error);
	}

	public JSONResponse (Object id, JSONRPCException error) {
		init(id, null, error);
	}

	private void init (ObjectNode response) throws JSONRPCException {
		if (response == null || response.isNull()) {
			throw new JSONRPCException(JSONRPCException.CODE.INVALID_REQUEST, 
				"Response is null");
		}
		if (response.has("jsonrpc") && response.get("jsonrpc").isTextual()) {
			if (!response.get("jsonrpc").asText().equals("2.0")) {
				throw new JSONRPCException(JSONRPCException.CODE.INVALID_REQUEST, 
						"Value of member 'jsonrpc' must be '2.0'");
			}
		}
		boolean hasError = response.has("error") && !response.get("error").isNull();
		/* TODO: cleanup
		if (hasResult && hasError) {
			throw new JSONRPCException(JSONRPCException.CODE.INVALID_REQUEST, 
				"Response contains both members 'result' and 'error' but may not contain both.");
		}
		if (!hasResult && !hasError) {
			throw new JSONRPCException(JSONRPCException.CODE.INVALID_REQUEST, 
				"Response is missing member 'result' or 'error'");
		}
		*/
		if (hasError) {
			if (!(response.get("error").isObject())) {
				throw new JSONRPCException(JSONRPCException.CODE.INVALID_REQUEST, 
					"Member 'error' is no ObjectNode");					
			}
		}
		
		Object id = response.get("id"); 
		Object result = response.get("result");
		JSONRPCException error = null;
		if (hasError) {
			error = new JSONRPCException((ObjectNode)response.get("error"));
		}
		
		init(id, result, error);
	}

	private void init(Object id, Object result, JSONRPCException error) {
		setVersion();
		setId(id);
		setResult(result);
		setError(error);
	}
	
	public void setId(Object id) {
		ObjectMapper mapper = JOM.getInstance();
		resp.put("id", mapper.convertValue(id, JsonNode.class));
	}

	public Object getId() {
		ObjectMapper mapper = JOM.getInstance();
		return mapper.convertValue(resp.get("id"), JsonNode.class);
	}

	public void setResult(Object result) {
		if (result != null) {
			ObjectMapper mapper = JOM.getInstance();
			resp.put("result", mapper.convertValue(result, JsonNode.class));
			setError(null);
		}
		else {
			if (resp.has("result")) {
				resp.remove("result");
			}			
		}
	}

	public JsonNode getResult() {
		return resp.get("result");
	}

	public <T> T getResult(Class<T> type) {
		ObjectMapper mapper = JOM.getInstance();
		return mapper.convertValue(resp.get("result"), type);
	}

	public void setError(JSONRPCException error) {
		if (error != null) {
			resp.put("error", error.getObjectNode());
			setResult(null);
		}
		else {
			if (resp.has("error")) {
				resp.remove("error");
			}
		}
	}

	public JSONRPCException getError() throws JSONRPCException {
		if (resp.has("error")) {
			ObjectNode error = (ObjectNode) resp.get("error");
			return new JSONRPCException(error);
		}
		else {
			return null;
		}
	}

	/* TODO: gives issues with Jackson
	public boolean isError() {
		return (resp.get("error") != null);
	}
	*/

	private void setVersion() {
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
			e.printStackTrace();
		}
		return null;
	}
}