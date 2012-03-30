package com.almende.eve.json;

import net.sf.json.JSONObject;

@SuppressWarnings("serial")
public class JSONRPCException extends Throwable {
	protected JSONObject error = new JSONObject();

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

	public JSONRPCException (JSONObject error) throws JSONRPCException {
		if (error != null && !error.isNullObject()) {
			//* TODO: do I want this exception class to throw itself?
			// TODO: throw a JSONException instead of a JSONRPCException?
			if (!error.has("code")) {
				throw new JSONRPCException(JSONRPCException.CODE.INVALID_REQUEST, 
					"Exception is missing member 'code'");
			}
			if (!error.has("message")) {
				throw new JSONRPCException(JSONRPCException.CODE.INVALID_REQUEST, 
					"Exception is missing member 'message'");
			}
			//*/
			
			int code = 0;
			if (error.has("code")) {
				code = error.getInt("code");
			}
			setCode(code);
			setMessage(error.getString("message"));
			if (error.has("data")) {
				setData(error.get("data"));
			}
		}
		else {
			init(CODE.UNKNOWN_ERROR, null);
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
