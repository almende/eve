/*
 * Copyright: Almende B.V. (2014), Rotterdam, The Netherlands
 * License: The Apache Software License, Version 2.0
 */
package com.almende.eve.agent.callback;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import com.almende.eve.config.Config;

/**
 * Queue to hold a list with callbacks in progress.
 * The Queue handles timeouts on the callbacks.
 * 
 * @param <T>
 *            the generic type
 */
public class AsyncCallbackQueue<T> {
	private final Map<Object, CallbackHandler>	queue		= new ConcurrentHashMap<Object, CallbackHandler>();
	private static ScheduledThreadPoolExecutor	scheduler	= new ScheduledThreadPoolExecutor(
																	1,
																	Config.getThreadFactory());
	
	static {
		try {
			ScheduledThreadPoolExecutor.class.getMethod(
					"setRemoveOnCancelPolicy", Boolean.class);
			scheduler.setRemoveOnCancelPolicy(true);
		} catch (NoSuchMethodException e) {
		}
	}
	/** timeout in seconds */
	private int									timeout		= 30;
	
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
	 * @param id
	 *            the id
	 * @param description
	 *            the description
	 * @param callback
	 *            the callback
	 */
	public void push(final Object id, final String description,
			final AsyncCallback<T> callback) {
		if (queue.containsKey(id)) {
			throw new IllegalStateException("Callback with id '" + id
					+ "' already in queue");
		}
		
		final AsyncCallbackQueue<T> me = this;
		final CallbackHandler handler = new CallbackHandler();
		handler.callback = callback;
		handler.timeout = scheduler.schedule(new Runnable() {
			@Override
			public void run() {
				final AsyncCallback<T> callback = me.pull(id);
				if (callback != null) {
					callback.onFailure(new TimeoutException(
							"Timeout occurred for request with id '" + id
									+ "': " + description));
				}
			}
		}, timeout, TimeUnit.SECONDS);
		queue.put(id, handler);
	}
	
	/**
	 * Pull a callback from the queue. The callback can be pulled from the
	 * queue only once. If no callback is found with given id, null will
	 * be returned.
	 * 
	 * @param id
	 *            the id
	 * @return the async callback
	 */
	public AsyncCallback<T> pull(final Object id) {
		final CallbackHandler handler = queue.remove(id);
		if (handler != null) {
			// stop the timeout
			handler.timeout.cancel(true);
			handler.timeout = null;
			return handler.callback;
		}
		return null;
	}
	
	/**
	 * Remove all callbacks from the queue.
	 */
	public synchronized void clear() {
		queue.clear();
		scheduler.shutdownNow();
		scheduler = new ScheduledThreadPoolExecutor(1,
				Config.getThreadFactory());
		scheduler.setRemoveOnCancelPolicy(true);
	}
	
	/**
	 * Helper class to store a callback and its timeout task.
	 */
	private class CallbackHandler {
		private AsyncCallback<T>	callback;
		private ScheduledFuture<?>	timeout;
	}
	
}
