/*
 * Copyright: Almende B.V. (2014), Rotterdam, The Netherlands
 * License: The Apache Software License, Version 2.0
 */
package com.almende.eve.agent.callback;

/**
 * The Interface CallbackInterface.
 *
 * @param <T> the generic type
 */
public interface CallbackInterface<T> {
	
	/**
	 * Store.
	 *
	 * @param id the id
	 * @param callback the callback
	 */
	void store(Object id, AsyncCallback<T> callback);
	
	/**
	 * Gets the.
	 *
	 * @param id the id
	 * @return the async callback
	 */
	AsyncCallback<T> get(Object id);
}
