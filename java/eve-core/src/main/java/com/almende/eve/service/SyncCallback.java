package com.almende.eve.service;

public class SyncCallback<T> implements AsyncCallback<T> {
	private T response = null;
	private Exception exception = null;
	
	@Override
	public synchronized void onSuccess(T response) {
		this.response = response;
		this.notifyAll();
	}
	
	@Override
	public synchronized void onFailure(Exception exception) {
		this.exception = exception;
		this.notifyAll();
	}
	
	/**
	 * Get will wait for the request to finish and then return the 
	 * response. If an exception is returned, the exception will be 
	 * thrown.
	 * @return response
	 * @throws Exception
	 */
	public synchronized T get() throws Exception {
		this.wait();
		if (exception != null) {
			throw exception;
		}
		return response;
	}
};
