/*
 * Copyright: Almende B.V. (2014), Rotterdam, The Netherlands
 * License: The Apache Software License, Version 2.0
 */
package com.almende.eve.agent.callback;

/**
 * The Class CallbackService.
 *
 * @param <T> the generic type
 */
public class CallbackService<T> implements CallbackInterface<T> {
	/** Contains list of "in-memory callbacks", like AsyncCallbacks */
	private final AsyncCallbackQueue<T>	queue	= new AsyncCallbackQueue<T>();
	
	/* (non-Javadoc)
	 * @see com.almende.eve.agent.callback.CallbackInterface#store(java.lang.Object, com.almende.eve.agent.callback.AsyncCallback)
	 */
	@Override
	public void store(final Object id, final AsyncCallback<T> callback) {
		queue.push(id, "", callback);
	}
	
	/* (non-Javadoc)
	 * @see com.almende.eve.agent.callback.CallbackInterface#get(java.lang.Object)
	 */
	@Override
	public AsyncCallback<T> get(final Object id) {
		return queue.pull(id);
	}
	
	// Contains utility code to get "persistent callbacks" from state
	
}
