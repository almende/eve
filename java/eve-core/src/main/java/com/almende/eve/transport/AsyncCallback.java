package com.almende.eve.transport;

public interface AsyncCallback<T> {
	public void onSuccess (T result);
	public void onFailure (Exception exception);
}
