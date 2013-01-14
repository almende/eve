package com.almende.eve.service;

import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeoutException;

/**
 * Queue to hold a list with callbacks in progress.
 * The Queue handles timeouts on the callbacks.
 */
public class AsyncCallbackQueue<T> {
	private static int TIMEOUT = 30000; // timeout in milliseconds
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
	 * @param id
	 * @param callback
	 * @throws Exception
	 */
	public synchronized void push(final String id, 
			AsyncCallback<T> callback) throws Exception {
		final AsyncCallbackQueue<T> me = this;
		CallbackHandler handler = new CallbackHandler();
		handler.callback = callback;
		handler.timeout = new TimerTask() {
			@Override
			public void run() {
				AsyncCallback<T> callback = me.pull(id);
				if (callback != null) {
					callback.onFailure(new TimeoutException(
						"Timeout occurred for request with id '" + id + "'"));
				}
			}
        };
		timer.schedule(handler.timeout, TIMEOUT);
		if (queue.containsKey(id)) {
			throw new Exception("Callback with id '" + id + "' already in queue");
		}
		queue.put(id, handler);
	}

	/**
	 * Pull a callback from the queue. The callback can be pulled from the
	 * queue only once. If no callback is found with given id, null will
	 * be returned.
	 * @param id
	 * @return
	 */
	public synchronized AsyncCallback<T> pull(String id) {
		CallbackHandler handler = queue.get(id);
		if (handler != null) {
			queue.remove(id);
			handler.timeout.cancel(); // stop the timeout
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
	 * Helper class to store a callback and its timeout task
	 */
	private class CallbackHandler {
		public AsyncCallback<T> callback;
		public TimerTask timeout;
	}
	
	private Map<String, CallbackHandler> queue = 
			new ConcurrentHashMap<String, CallbackHandler>();
	private Timer timer = new Timer(true);  // deamon timer
}
