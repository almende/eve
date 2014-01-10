/*
 * Copyright: Almende B.V. (2014), Rotterdam, The Netherlands
 * License: The Apache Software License, Version 2.0
 */
package com.almende.eve.agent.callback;

/**
 * The Interface AsyncCallback.
 *
 * @param <T> the generic type
 */
public interface AsyncCallback<T> {
	
	/**
	 * On success.
	 *
	 * @param result the result
	 */
	void onSuccess(T result);
	
	/**
	 * On failure.
	 *
	 * @param exception the exception
	 */
	void onFailure(Exception exception);
}
