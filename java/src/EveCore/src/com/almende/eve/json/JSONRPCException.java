package com.almende.eve.json;

import net.sf.json.JSONObject;

@SuppressWarnings("serial")
public class JSONRPCException extends Throwable {
	protected JSONObject error = new JSONObject();

	public static enum CODE {
		PARSE_ERROR,
		INVALID_REQUEST,
		METHOD_NOT_FOUND,
		INVALID_PARAMS,
		INTERNAL_ERROR
	};
	
	public JSONRPCException () {
		setCode(-32000);
		setMessage("Unknown Error");
	}

	public JSONRPCException (CODE code) {
		init(code, null);
	}

	public JSONRPCException (CODE code, String description) {
		init(code, description);
	}
	
	public JSONRPCException (JSONRPCException error) {
		setCode(error.getCode());
		setMessage(error.getMessage());
		if (error.hasData()) {
			setData(error.getData());
		}
	}

	public JSONRPCException (JSONObject error) {
		/* TODO: do I want this exception class to throw itself?
		if (!error.has("code")) {
			throw new JSONRPCException(JSONRPCException.CODE.INVALID_REQUEST, 
				"Exception is missing member 'code'");
		}
		if (!error.has("message")) {
			throw new JSONRPCException(JSONRPCException.CODE.INVALID_REQUEST, 
				"Exception is missing member 'message'");
		}
		*/
		
		setCode(error.getInt("code"));
		setMessage(error.getString("message"));
		if (error.has("data")) {
			setData(error.get("data"));
		}
	}	

	public JSONRPCException (String message) {
		setCode(-32000);
		setMessage(message);
	}
	
	public JSONRPCException (int code, String message) {
		setCode(code);
		setMessage(message);
	}

	public JSONRPCException (int code, String message, Object data) {
		setCode(code);
		setMessage(message);
	}
	
	private void init(CODE code, String description) {
		switch (code) {
		case PARSE_ERROR: setCode (-32700); setMessage("Parse error"); break;
		case INVALID_REQUEST: setCode (-32600); setMessage("Invalid request"); break;
		case METHOD_NOT_FOUND: setCode (-32601); setMessage("Method not found"); break;
		case INVALID_PARAMS: setCode (-32602); setMessage("Invalid params"); break;
		case INTERNAL_ERROR: setCode (-32603); setMessage("Internal error"); break;
		}
		
		if (description != null) {
			JSONObject data = new JSONObject();
			data.put("description", description);
			setData(data);
		}
	}
	
	public void setCode(int code) {
		error.put("code", code);
	}
	
	public int getCode() {
		return error.getInt("code");
	}
	
	public void setMessage(String message) {
		error.put("message", message != null ? message : "");
	}
	
	public String getMessage() {
		return error.getString("message");
	}
	
	public void setData(Object data) {
		error.put("data", data);
	}
	
	public Object getData() {
		return error.get("data");
	}
	
	public boolean hasData() {
		return error.has("data");
	}
	
	public JSONObject getJSONObject() {
		return error;
	}

	@Override
	public String toString() {
		return error.toString();
	}
}
