package com.almende.eve.agent.callback;

public interface CallbackInterface<T> {
	
	void store(Object id, AsyncCallback<T> callback);
	
	AsyncCallback<T> get(Object id);
}
