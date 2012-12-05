package com.almende.eve.service;

public interface AsyncCallback<T> {
	public void onSuccess (T result);
	public void onFailure (Throwable caught);
}
