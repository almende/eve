package com.almende.eve.agent.callback;


public class CallbackService<T> implements CallbackInterface<T> {
	//Contains list of "in-memory callbacks", like AsyncCallbacks
	private AsyncCallbackQueue<T> queue = new AsyncCallbackQueue<T>();
	
	public void store(Object id, AsyncCallback<T> callback){
		queue.push(id, "", callback);
	}
	@Override
	public AsyncCallback<T> get(Object id){
		return queue.pull(id);
	}
	
	//Contains utility code to get "persistent callbacks" from state
	
}
