package com.almende.eve.rpc.jsonrpc;

import com.almende.eve.rpc.jsonrpc.jackson.JOM;
import com.fasterxml.jackson.core.JsonProcessingException;
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
		REMOTE_EXCEPTION,
		METHOD_NOT_FOUND,
		INVALID_PARAMS,
		INTERNAL_ERROR,
		NOT_FOUND
	};
	
	public JSONRPCException () {
		super();
		init(CODE.UNKNOWN_ERROR, null,null);
	}

	public JSONRPCException (CODE code) {
		super();
		init(code, null,null);
	}

	public JSONRPCException (CODE code, String description) {
		super(description);
		init(code, description,null);
	}
	public JSONRPCException (CODE code, String description, final Throwable t) {
		super(description,t);
		init(code, description,t);
	}
	
	public JSONRPCException (JSONRPCException error) {
		super(error);
		if (error != null) {
			setCode(error.getCode());
			setMessage(error.getMessage());
			if (error.hasData()) {
				setData(error.getData());
			}
		}
		else {
			init(CODE.UNKNOWN_ERROR, null, null);
		}
	}
	public JSONRPCException (String message) {
		super(message);
		setCode(0);
		setMessage(message);
	}
	public JSONRPCException (String message, Throwable t) {
		super(message,t);
		setCode(0);
		setMessage(message);
		try {
			setData(JOM.getInstance().writeValueAsString(t));
		} catch (JsonProcessingException e) {}
	}
	
	public JSONRPCException (Integer code, String message) {
		super(message);
		setCode(code);
		setMessage(message);
	}

	public JSONRPCException (Integer code, String message, Object data) {
		super(message);
		setCode(code);
		setMessage(message);
		setData(data);
	}
	

	public JSONRPCException (ObjectNode exception) {
		this(CODE.REMOTE_EXCEPTION,JOM.getInstance().convertValue(
				exception.get("data"), Exception.class).getMessage(), JOM.getInstance().convertValue(
				exception.get("data"), Exception.class));
		if (exception != null && !exception.isNull()) {
			// set code, message, and optional data
			if (exception.has("code")) {
				setCode(exception.get("code").asInt());
			}
			if (exception.has("message")) {
				setMessage(exception.get("message").asText());
			}
			if (exception.has("data")) {
				setData(exception.get("data"));
			}
		}
	}	


	private void init(CODE code, String message, Throwable t) {
		switch (code) {
			case UNKNOWN_ERROR: setCode (-32000); setMessage("Unknown error"); break;
			case PARSE_ERROR: setCode (-32700); setMessage("Parse error"); break;
			case INVALID_REQUEST: setCode (-32600); setMessage("Invalid request"); break;
			case REMOTE_EXCEPTION: setCode (-32500); setMessage("Remote application error"); break;
			case METHOD_NOT_FOUND: setCode (-32601); setMessage("Method not found"); break;
			case INVALID_PARAMS: setCode (-32602); setMessage("Invalid params"); break;
			case INTERNAL_ERROR: setCode (-32603); setMessage("Internal error"); break;
			case NOT_FOUND: setCode(404); setMessage("Not found"); break;
		}
		
		if (message != null) {
			error.put("message", message);
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
		error.put("data", data != null ? mapper.convertValue(data, JsonNode.class) : null);
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