package com.almende.eve.agent.callback;

import com.almende.eve.rpc.jsonrpc.JSONResponse;

public interface CallbackInterface {
	
	void store(String id, AsyncCallback<JSONResponse> callback);
	AsyncCallback<JSONResponse> get(String id);
}
