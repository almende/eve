package com.almende.eve.messenger;

public interface AsyncCallback<T> {
	public void onSuccess (T result);
	public void onFailure (Throwable caught);
}
