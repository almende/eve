/*
 * Copyright: Almende B.V. (2014), Rotterdam, The Netherlands
 * License: The Apache Software License, Version 2.0
 */
package com.almende.eve.agent.callback;

import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeoutException;

/**
 * Queue to hold a list with callbacks in progress.
 * The Queue handles timeouts on the callbacks.
 *
 * @param <T> the generic type
 */
public class AsyncCallbackQueue<T> {
	private final Map<Object, CallbackHandler>	queue	= new ConcurrentHashMap<Object, CallbackHandler>();
	private Timer							timer	= new Timer(true);
	/** timeout in milliseconds */
	private static final int				TIMEOUT	= 30000;
	// TODO: make the timeout customizable in eve.yaml
	
	/**
	 * Append a callback to the queue.
	 * 
	 * The callback must be pulled from the queue again within the
	 * timeout. If not, the callback.onFailure will be called with a
	 * TimeoutException as argument, and is deleted from the queue.
	 * 
	 * The method will throw an exception when a callback with the same id
	 * is already in the queue.
	 *
	 * @param id the id
	 * @param description the description
	 * @param callback the callback
	 */
	public synchronized void push(final Object id, final String description,
			final AsyncCallback<T> callback) {
		if (queue.containsKey(id)) {
			throw new IllegalStateException("Callback with id '" + id
					+ "' already in queue");
		}
		
		final AsyncCallbackQueue<T> me = this;
		final CallbackHandler handler = new CallbackHandler();
		handler.callback = callback;
		handler.timeout = new TimerTask() {
			@Override
			public void run() {
				final AsyncCallback<T> callback = me.pull(id);
				if (callback != null) {
					callback.onFailure(new TimeoutException(
							"Timeout occurred for request with id '" + id
									+ "': " + description));
				}
			}
		};
		try {
			timer.schedule(handler.timeout, TIMEOUT);
		} catch (final IllegalStateException e) {
			timer = new Timer(true);
			timer.schedule(handler.timeout, TIMEOUT);
		}
		queue.put(id, handler);
	}
	
	/**
	 * Pull a callback from the queue. The callback can be pulled from the
	 * queue only once. If no callback is found with given id, null will
	 * be returned.
	 *
	 * @param id the id
	 * @return the async callback
	 */
	public synchronized AsyncCallback<T> pull(final Object id) {
		final CallbackHandler handler = queue.get(id);
		if (handler != null) {
			queue.remove(id);
			// stop the timeout
			handler.timeout.cancel();
			return handler.callback;
		}
		return null;
	}
	
	/**
	 * Remove all callbacks from the queue.
	 */
	public void clear() {
		queue.clear();
		timer.cancel();
	}
	
	/**
	 * Helper class to store a callback and its timeout task.
	 */
	private class CallbackHandler {
		private AsyncCallback<T>	callback;
		private TimerTask			timeout;
	}
	
}
