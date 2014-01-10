/*
 * Copyright: Almende B.V. (2014), Rotterdam, The Netherlands
 * License: The Apache Software License, Version 2.0
 */
package com.almende.eve.agent.callback;


/**
 * The Class SyncCallback.
 *
 * @param <T> the generic type
 */
public class SyncCallback<T> implements AsyncCallback<T> {
	private T			response	= null;
	private Exception	exception	= null;
	private boolean		done		= false;
	
	/* (non-Javadoc)
	 * @see com.almende.eve.agent.callback.AsyncCallback#onSuccess(java.lang.Object)
	 */
	@Override
	public void onSuccess(final T response) {
		this.response = response;
		done = true;
		synchronized (this) {
			notifyAll();
		}
	}
	
	/* (non-Javadoc)
	 * @see com.almende.eve.agent.callback.AsyncCallback#onFailure(java.lang.Exception)
	 */
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
	 * @throws Exception the exception
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
