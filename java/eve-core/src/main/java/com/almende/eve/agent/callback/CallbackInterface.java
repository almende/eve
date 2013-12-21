package com.almende.eve.agent.callback;

import com.almende.eve.rpc.jsonrpc.JSONResponse;

public interface CallbackInterface {
	
	void store(Object id, AsyncCallback<JSONResponse> callback);
	AsyncCallback<JSONResponse> get(Object id);
}
