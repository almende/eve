package com.almende.eve.agent.callback;

import com.almende.eve.rpc.jsonrpc.JSONResponse;

public class CallbackService implements CallbackInterface {
	//Contains list of "in-memory callbacks", like AsyncCallbacks
	private AsyncCallbackQueue<JSONResponse> queue = new AsyncCallbackQueue<JSONResponse>();
	
	public void store(String id, AsyncCallback<JSONResponse> callback){
		queue.push(id, "", callback);
	}
	public AsyncCallback<JSONResponse> get(String id){
		return queue.pull(id);
	}
	
	//Contains utility code to get "persistent callbacks" from state
	
}
