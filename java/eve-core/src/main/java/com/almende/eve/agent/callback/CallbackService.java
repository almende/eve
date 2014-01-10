package com.almende.eve.agent.callback;

public class CallbackService<T> implements CallbackInterface<T> {
	// Contains list of "in-memory callbacks", like AsyncCallbacks
	private final AsyncCallbackQueue<T>	queue	= new AsyncCallbackQueue<T>();
	
	@Override
	public void store(final Object id, final AsyncCallback<T> callback) {
		queue.push(id, "", callback);
	}
	
	@Override
	public AsyncCallback<T> get(final Object id) {
		return queue.pull(id);
	}
	
	// Contains utility code to get "persistent callbacks" from state
	
}
