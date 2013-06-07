package com.almende.eve.agent.proxy;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import com.almende.util.ClassUtil;

/**
 * Asynchronous proxy wrapper, which can be used to decorate a generated proxy.
 * @author ludo
 *
 * @param <T>
 */
public class AsyncProxy<T> {
	private ScheduledExecutorService pool = Executors
			.newScheduledThreadPool(50);
	private T proxy;
	
	public AsyncProxy(T proxy) {
		this.proxy = proxy;
	}
	
	/**
	 * Call the given method on the wrapped proxy, returning a Future which can be used to wait for the result and/or cancel the task.
	 * 
	 * @param functionName
	 * @param args
	 * @return Future<?>
	 * @throws SecurityException
	 * @throws NoSuchMethodException
	 */
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public Future<?> call(String functionName, final Object... args) throws NoSuchMethodException{
		ArrayList<Class> classes = new ArrayList<Class>(args.length);
		for (Object obj : args){
			classes.add(obj.getClass());
		}
		final Method method = ClassUtil.searchForMethod(proxy.getClass(), functionName, classes.toArray(new Class[0]));
		
		return new DecoratedFuture(pool.submit(new Callable<Object>(){
			@Override
			public Object call() throws Exception {
				return method.invoke(proxy, args);
			}
		}),ClassUtil.wrap(method.getReturnType()));
	}
	class DecoratedFuture<V> implements Future<V>{
		Future<?> future;
		Class<V> myType;
		
		DecoratedFuture(Future<?> future, Class<V> type){
			this.future=future;
			this.myType=type;
		}
		
		@Override
		public boolean cancel(boolean mayInterruptIfRunning) {
			return future.cancel(mayInterruptIfRunning);
		}

		@Override
		public boolean isCancelled() {
			return future.isCancelled();
		}

		@Override
		public boolean isDone() {
			return future.isDone();
		}

		@Override
		public V get() throws InterruptedException, ExecutionException {
			return myType.cast(future.get());
		}

		@Override
		public V get(long timeout, TimeUnit unit) throws InterruptedException,
				ExecutionException, TimeoutException {
			return myType.cast(future.get(timeout, unit));
		}
	}
}
