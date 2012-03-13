package eve.json;

import java.io.Writer;

import net.sf.json.JSON;
import net.sf.json.JSONObject;

@SuppressWarnings("serial")
public class JSONRequest implements JSON {
	protected JSONObject req = new JSONObject();

	public enum VERSION {ONE, TWO};
	
	public JSONRequest () {
		init(null, null, null);
	}

	public JSONRequest (JSONObject object) throws JSONRPCException {
		// check if the object contains a valid JSON-RPC 2.0 message		
		if (object.has("jsonrpc")) {
			if (!object.getString("jsonrpc").equals("2.0")) {
				throw new JSONRPCException(JSONRPCException.CODE.INVALID_REQUEST, 
						"Value of member 'jsonrpc' is not equal to '2.0'");
			}
		}
		if (!object.has("method")) {
			throw new JSONRPCException(JSONRPCException.CODE.INVALID_REQUEST, 
				"Member 'method' missing in request");
		}
		if (!(object.get("method") instanceof String)) {
			throw new JSONRPCException(JSONRPCException.CODE.INVALID_REQUEST, 
				"Member 'method' is no String");
		}
		if (!object.has("params")) {
			throw new JSONRPCException(JSONRPCException.CODE.INVALID_REQUEST, 
				"Member 'params' missing in request");
		}
		if (!(object.get("params") instanceof JSONObject)) {
			throw new JSONRPCException(JSONRPCException.CODE.INVALID_REQUEST, 
				"Member 'params' is no JSONObject");
		}
		
		init (object.get("id"), object.getString("method"), 
				object.getJSONObject("params"));
	}

	public JSONRequest (String method, JSONObject params) {
		init (null, method, params);
	}

	public JSONRequest (Object id, String method, JSONObject params) {
		init (id, method, params);
	}

	public JSONRequest (String method, JSONObject params,
			String callbackUrl, String callbackMethod) {
		init (null, method, params);
		setCallback(callbackUrl, callbackMethod);
	}

	public JSONRequest (Object id, String method, JSONObject params,
			String callbackUrl, String callbackMethod) {
		init (id, method, params);
		setCallback(callbackUrl, callbackMethod);
	}

	private void init(Object id, String method, JSONObject params) {
		setVersion();
		setId(id);
		setMethod(method);
		setParams(params);
	}
	
	public void setId(Object id) {
		req.put("id", id);
	}
	
	public Object getId() {
		return req.get("id");
	}

	public void setMethod(String method) {
		req.put("method", method);
	}

	public String getMethod() {
		return req.getString("method");
	}

	public void setParams(JSONObject params) {
		req.put("params", params != null ? params : new JSONObject());
	}
	
	public JSONObject getParams() {
		return req.getJSONObject("params");
	}
	
	public void putParam(String name, Object value) {
		req.getJSONObject("params").put(name, value);
	}

	public Object getParam(String name) {
		return req.getJSONObject("params").get(name);
	}
	
	public Object hasParam(String name) {
		return req.getJSONObject("params").has(name);
	}	

	private void setVersion() {
		req.put("jsonrpc", "2.0");
	}

	public void setCallback(String url, String method) {
		JSONObject callback = new JSONObject();
		callback.put("url", url);
		callback.put("method", method);
		req.put("callback", callback);
	}

	public String getCallbackUrl() {
		JSONObject callback = req.getJSONObject("callback");
		return (callback != null) ? callback.getString("url") : null;
	}

	public String getCallbackMethod() {
		JSONObject callback = req.getJSONObject("callback");
		return (callback != null) ? callback.getString("method") : null;
	}

	public boolean hasCallback() {
		return req.has("callback");
	}
	
	
	@Override
	public boolean isArray() {
		return req.isArray();
	}

	@Override
	public boolean isEmpty() {
		return req.isEmpty();
	}

	@Override
	public int size() {
		return req.size();
	}
	
	@Override
	public String toString() {
		return req.toString();
	}
	
	@Override
	public String toString(int arg0) {
		return req.toString(arg0);
	}

	@Override
	public String toString(int arg0, int arg1) {
		return req.toString(arg0, arg1);
	}

	@Override
	public Writer write(Writer arg0) {
		return req.write(arg0);
	}
}
