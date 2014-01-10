/*
 * Copyright: Almende B.V. (2014), Rotterdam, The Netherlands
 * License: The Apache Software License, Version 2.0
 */
package com.almende.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotSame;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.junit.Test;

import com.almende.eve.agent.callback.SyncCallback;

/**
 * The Class TestAsyncCallback.
 */
public class TestAsyncCallback {
	
	/**
	 * Test sync call.
	 */
	@Test
	public void testSyncCall() {
		final ScheduledExecutorService scheduler = Executors
				.newScheduledThreadPool(10);
		final SyncCallback<Integer> callback1 = new SyncCallback<Integer>();
		final SyncCallback<Integer> callback2 = new SyncCallback<Integer>();
		assertNotSame(callback1, callback2);
		
		scheduler.schedule(new Runnable() {
			
			@Override
			public void run() {
				System.err.println("Send something to callback 1");
				callback1.onSuccess(1);
			}
			
		}, 900, TimeUnit.MILLISECONDS);
		scheduler.schedule(new Runnable() {
			
			@Override
			public void run() {
				System.err.println("Send something to callback 2");
				callback2.onSuccess(1);
			}
			
		}, 500, TimeUnit.MILLISECONDS);
		scheduler.schedule(new Runnable() {
			
			@Override
			public void run() {
				System.err.println("Starting waiting for callback 2");
				try {
					final Integer res = callback2.get();
					assertEquals(new Integer(1), res);
				} catch (final Exception e) {
					e.printStackTrace();
				}
			}
			
		}, 100, TimeUnit.MILLISECONDS);
		System.err.println("Starting waiting for callback 1");
		try {
			final Integer res = callback1.get();
			assertEquals(new Integer(1), res);
		} catch (final Exception e) {
			e.printStackTrace();
		}
	}
	
}
