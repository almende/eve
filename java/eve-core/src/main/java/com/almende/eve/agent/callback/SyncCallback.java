package com.almende.eve.agent.callback;

public class SyncCallback<T> implements AsyncCallback<T> {
	private T response = null;
	private Exception exception = null;
	private boolean done = false;
	
	@Override
	public synchronized void onSuccess(T response) {
		this.response = response;
		done = true;
		notifyAll();
	}
	
	@Override
	public synchronized void onFailure(Exception exception) {
		this.exception = exception;
		done = true;
		notifyAll();
	}
	
	/**
	 * Get will wait for the request to finish and then return the 
	 * response. If an exception is returned, the exception will be 
	 * thrown.
	 * @return response
	 * @throws Exception
	 */
	public synchronized T get() throws Exception {
		while (!done) {
			wait();
		}
		
		if (exception != null) {
			throw exception;
		}
		return response;
	}
};
