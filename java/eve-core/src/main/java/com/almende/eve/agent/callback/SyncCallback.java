package com.almende.eve.agent.callback;

public class SyncCallback<T> implements AsyncCallback<T> {
	private T			response	= null;
	private Exception	exception	= null;
	private boolean		done		= false;
	
	@Override
	public void onSuccess(final T response) {
		this.response = response;
		done = true;
		synchronized (this) {
			notifyAll();
		}
	}
	
	@Override
	public void onFailure(final Exception exception) {
		this.exception = exception;
		done = true;
		synchronized (this) {
			notifyAll();
		}
	}
	
	/**
	 * Get will wait for the request to finish and then return the
	 * response. If an exception is returned, the exception will be
	 * thrown.
	 * 
	 * @return response
	 * @throws Exception
	 */
	public T get() throws Exception {
		while (!done) {
			synchronized (this) {
				wait();
			}
		}
		
		if (exception != null) {
			throw exception;
		}
		return response;
	}
};
