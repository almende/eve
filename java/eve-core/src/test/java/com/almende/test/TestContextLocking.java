package com.almende.test;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import junit.framework.TestCase;

import org.junit.Test;

import com.almende.eve.context.ConcurrentFileContext;
import com.almende.eve.context.Context;
import com.almende.eve.context.FileContext;

public class TestContextLocking extends TestCase {
	
	private void testRun(final Context context){
		//context.clear();
		context.put("test", "test");
		context.put("test2", "test2");
		
		ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(10);
		final ScheduledFuture<?> thread1 = scheduler.scheduleAtFixedRate(new Runnable(){
			@Override
			public void run() {
				context.put("test","test1");
			}
			
		}, 0, 300, TimeUnit.MILLISECONDS);
		final ScheduledFuture<?> thread2 = scheduler.scheduleAtFixedRate(new Runnable(){
			@Override
			public void run() {
				context.put("test","test2");
			}
			
		}, 100, 100, TimeUnit.MILLISECONDS);
		scheduler.schedule(new Runnable(){

			@Override
			public void run() {
				thread1.cancel(false);
				thread2.cancel(false);
			}
		}, 1200, TimeUnit.MILLISECONDS);
		assertEquals("test2",(String)context.get("test2"));
		assertTrue(((String)context.get("test")).startsWith("test"));
		
	}
	
	@Test
	public void testFileContext() throws Exception{
		FileContext fc = new FileContext("test",".testFileContextRun");
		testRun(fc);
	}
	@Test
	public void testConcurrentFileContext() throws Exception{
		ConcurrentFileContext fc = new ConcurrentFileContext("test",".testConcurrentFileContextRun");
		testRun(fc);
	}
}
