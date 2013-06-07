package com.almende.eve.transport;

public interface AsyncCallback<T> {
	void onSuccess (T result);
	void onFailure (Exception exception);
}
