package com.almende.eve.json;

import com.almende.eve.json.jackson.JOM;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

@SuppressWarnings("serial")
public class JSONRPCException extends Exception {
	protected ObjectNode error = JOM.createObjectNode();

	public static enum CODE {
		UNKNOWN_ERROR,
		PARSE_ERROR,
		INVALID_REQUEST,
		METHOD_NOT_FOUND,
		INVALID_PARAMS,
		INTERNAL_ERROR
	};
	
	public JSONRPCException () {
		init(CODE.UNKNOWN_ERROR, null);
	}

	public JSONRPCException (CODE code) {
		init(code, null);
	}

	public JSONRPCException (CODE code, String description) {
		init(code, description);
	}
	
	public JSONRPCException (JSONRPCException error) {
		if (error != null) {
			setCode(error.getCode());
			setMessage(error.getMessage());
			if (error.hasData()) {
				setData(error.getData());
			}
		}
		else {
			init(CODE.UNKNOWN_ERROR, null);
		}
	}

	public JSONRPCException (ObjectNode exception) throws JSONRPCException {
		if (exception == null || exception.isNull()) {
			throw new JSONRPCException(JSONRPCException.CODE.INVALID_REQUEST, 
				"Exception is null");
		}
		if (!exception.has("code")) {
			throw new JSONRPCException(JSONRPCException.CODE.INVALID_REQUEST, 
				"Exception is missing member 'code'");
		}
		if (!(exception.get("code").isInt())) {
			throw new JSONRPCException(JSONRPCException.CODE.INVALID_REQUEST, 
				"Member 'code' is no Integer");
		}
				
		if (!exception.has("message")) {
			throw new JSONRPCException(JSONRPCException.CODE.INVALID_REQUEST, 
				"Exception is missing member 'message'");
		}
		if (!(exception.get("message").isTextual())) {
			throw new JSONRPCException(JSONRPCException.CODE.INVALID_REQUEST, 
				"Member 'message' is no String");
		}
		
		// set code, message, and optional data
		setCode(exception.get("code").asInt());
		setMessage(exception.get("message").asText());
		if (exception.has("data")) {
			setData(exception.get("data"));
		}
	}	

	public JSONRPCException (String message) {
		setCode(0);
		setMessage(message);
	}
	
	public JSONRPCException (Integer code, String message) {
		setCode(code);
		setMessage(message);
	}

	public JSONRPCException (Integer code, String message, Object data) {
		setCode(code);
		setMessage(message);
	}
	
	private void init(CODE code, String description) {
		switch (code) {
			case UNKNOWN_ERROR: setCode (-32000); setMessage("Unknown error"); break;
			case PARSE_ERROR: setCode (-32700); setMessage("Parse error"); break;
			case INVALID_REQUEST: setCode (-32600); setMessage("Invalid request"); break;
			case METHOD_NOT_FOUND: setCode (-32601); setMessage("Method not found"); break;
			case INVALID_PARAMS: setCode (-32602); setMessage("Invalid params"); break;
			case INTERNAL_ERROR: setCode (-32603); setMessage("Internal error"); break;
		}
		
		if (description != null) {
			ObjectNode data = JOM.createObjectNode();
			data.put("description", description);
			setData(data);
		}
	}
		
	public void setCode(int code) {
		error.put("code", code);
	}
	
	public int getCode() {
		return error.get("code").asInt();
	}
	
	public void setMessage(String message) {
		error.put("message", message != null ? message : "");
	}
	
	public String getMessage() {
		return error.get("message").asText();
	}
	
	public void setData(Object data) {
		ObjectMapper mapper = JOM.getInstance();
		// TODO: test if convert value works
		error.put("data", mapper.convertValue(data, JsonNode.class));
	}
	
	public Object getData() {
		return error.get("data");
	}
	
	public boolean hasData() {
		return error.has("data");
	}
	
	public ObjectNode getObjectNode() {
		return error;
	}
	
	@Override
	public String toString() {
		ObjectMapper mapper = JOM.getInstance();
		try {
			return mapper.writeValueAsString(error);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}
}