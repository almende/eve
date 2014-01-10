package com.almende.eve.agent.callback;

public interface AsyncCallback<T> {
	void onSuccess(T result);
	
	void onFailure(Exception exception);
}
