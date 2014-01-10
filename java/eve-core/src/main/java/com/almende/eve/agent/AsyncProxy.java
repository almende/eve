/*
 * Copyright: Almende B.V. (2014), Rotterdam, The Netherlands
 * License: The Apache Software License, Version 2.0
 */
package com.almende.eve.agent;

import java.lang.reflect.InvocationTargetException;
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
 *
 * @param <T> the generic type
 * @author ludo
 */
public class AsyncProxy<T> {
	private final ScheduledExecutorService	pool	= Executors
															.newScheduledThreadPool(50);
	private final T							proxy;
	
	/**
	 * Instantiates a new async proxy.
	 *
	 * @param proxy the proxy
	 */
	public AsyncProxy(final T proxy) {
		this.proxy = proxy;
	}
	
	/**
	 * Call the given method on the wrapped proxy, returning a Future which can
	 * be used to wait for the result and/or cancel the task.
	 *
	 * @param functionName the function name
	 * @param args the args
	 * @return Future<?>
	 * @throws NoSuchMethodException the no such method exception
	 */
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public Future<?> call(final String functionName, final Object... args)
			throws NoSuchMethodException {
		final ArrayList<Class> classes = new ArrayList<Class>(args.length);
		for (final Object obj : args) {
			classes.add(obj.getClass());
		}
		final Method method = ClassUtil.searchForMethod(proxy.getClass(),
				functionName, classes.toArray(new Class[0]));
		
		return new DecoratedFuture(pool.submit(new Callable<Object>() {
			@Override
			public Object call() throws IllegalAccessException,
					InvocationTargetException {
				return method.invoke(proxy, args);
			}
		}), ClassUtil.wrap(method.getReturnType()));
	}
	
	/**
	 * The Class DecoratedFuture.
	 *
	 * @param <V> the value type
	 */
	class DecoratedFuture<V> implements Future<V> {
		
		/** The future. */
		private final Future<?>	future;
		
		/** The my type. */
		private final Class<V>	myType;
		
		/**
		 * Instantiates a new decorated future.
		 *
		 * @param future the future
		 * @param type the type
		 */
		DecoratedFuture(final Future<?> future, final Class<V> type) {
			this.future = future;
			this.myType = type;
		}
		
		/* (non-Javadoc)
		 * @see java.util.concurrent.Future#cancel(boolean)
		 */
		@Override
		public boolean cancel(final boolean mayInterruptIfRunning) {
			return future.cancel(mayInterruptIfRunning);
		}
		
		/* (non-Javadoc)
		 * @see java.util.concurrent.Future#isCancelled()
		 */
		@Override
		public boolean isCancelled() {
			return future.isCancelled();
		}
		
		/* (non-Javadoc)
		 * @see java.util.concurrent.Future#isDone()
		 */
		@Override
		public boolean isDone() {
			return future.isDone();
		}
		
		/* (non-Javadoc)
		 * @see java.util.concurrent.Future#get()
		 */
		@Override
		public V get() throws InterruptedException, ExecutionException {
			return myType.cast(future.get());
		}
		
		/* (non-Javadoc)
		 * @see java.util.concurrent.Future#get(long, java.util.concurrent.TimeUnit)
		 */
		@Override
		public V get(final long timeout, final TimeUnit unit)
				throws InterruptedException, ExecutionException,
				TimeoutException {
			return myType.cast(future.get(timeout, unit));
		}
	}
}
