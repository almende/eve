package com.almende.eve.json;

import java.io.Writer;

import net.sf.json.JSON;
import net.sf.json.JSONObject;

@SuppressWarnings("serial")
public class JSONResponse implements JSON {
	protected JSONObject resp = new JSONObject();

	public JSONResponse () {
		init(null, null, null);
	}

	public JSONResponse (JSONObject object) throws JSONRPCException {
		// TODO: check if this is a valid response object
		if (object.has("jsonrpc")) {
			if (!object.getString("jsonrpc").equals("2.0")) {
				throw new JSONRPCException(JSONRPCException.CODE.INVALID_REQUEST, 
						"Value of member 'jsonrpc' must be '2.0'");
			}
		}
		if (object.has("result") && object.has("error")) {
			throw new JSONRPCException(JSONRPCException.CODE.INVALID_REQUEST, 
				"Response contains both members 'result' and 'error' but may not contain both.");
		}
		if (!object.has("result") && !object.has("error")) {
			throw new JSONRPCException(JSONRPCException.CODE.INVALID_REQUEST, 
				"Response is missing member 'result' or 'error'");
		}
		if (object.has("error")) {
			if (!(object.get("error") instanceof JSONObject)) {
				throw new JSONRPCException(JSONRPCException.CODE.INVALID_REQUEST, 
					"Member 'error' is no JSONObject");					
			}
		}			
		
		init(object.get("id"), object.get("result"), 
				new JSONRPCException(object.getJSONObject("error")));
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
	
	private void init(Object id, Object result, JSONRPCException error) {
		setVersion();
		setId(id);
		setError(error);
		setResult(result);
	}
	
	public void setId(Object id) {
		resp.put("id", id != null ? id : 1);
	}

	public Object getId() {
		return resp.get("id");
	}

	public void setResult(Object result) {
		if (result != null) {
			resp.put("result", result);
			setError(null);
		}
		else {
			if (resp.has("result")) {
				resp.remove("result");
			}			
		}
	}

	public Object getResult() {
		return resp.get("result");
	}

	public void setError(JSONRPCException error) {
		if (error != null) {
			resp.put("error", error.getJSONObject());
			setResult(null);
		}
		else {
			if (resp.has("error")) {
				resp.remove("error");
			}
		}
	}

	public JSONRPCException getError() {
		if (resp.has("error")) {
			JSONObject error = resp.getJSONObject("error");
			return new JSONRPCException(error);
		}
		else {
			return null;
		}
	}

	public boolean isError() {
		return (resp.get("error") != null);
	}

	private void setVersion() {
		resp.put("jsonrpc", "2.0");
	}

	@Override
	public boolean isArray() {
		return resp.isArray();
	}

	@Override
	public boolean isEmpty() {
		return resp.isEmpty();
	}

	@Override
	public int size() {
		return resp.size();
	}

	@Override
	public String toString() {
		return resp.toString();
	}
	
	@Override
	public String toString(int arg0) {
		return resp.toString(arg0);
	}

	@Override
	public String toString(int arg0, int arg1) {
		return resp.toString(arg0, arg1);
	}

	@Override
	public Writer write(Writer arg0) {
		return resp.write(arg0);
	}
}
