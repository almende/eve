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
	//TODO: prove that a collision occurs, possibly by measuring the starttime and runtime of each run.
	//TODO: alternatively: implement a non-locking, non-thread-safe version of the context and see it break:)
	
	
	private void testRun(final Context context){
		//context.clear();
		context.put("test", "test");
		context.put("test2", "test2");
		
		ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(10);
		final ScheduledFuture<?> thread1 = scheduler.scheduleAtFixedRate(new Runnable(){
			@Override
			public void run() {
				context.put("test","test1");
				context.put("test","test1");
				context.get("test");
				context.put("test1","test");
				context.get("test1");
			}
			
		}, 0, 100, TimeUnit.MILLISECONDS);
		final ScheduledFuture<?> thread2 = scheduler.scheduleWithFixedDelay(new Runnable(){
			@Override
			public void run() {
				context.put("test","test2");
				context.put("test","test2");
				context.get("test");
				context.put("test1","test");
				context.put("test1","test");
				context.get("test1");
			}
			
		}, 110, 95, TimeUnit.MILLISECONDS);
		final ScheduledFuture<?> thread3 = scheduler.scheduleWithFixedDelay(new Runnable(){
			@Override
			public void run() {
				context.put("test","test3");
				context.put("test","test3");
				context.get("test");
				context.put("test1","test");
				context.put("test1","test");
				context.get("test1");
			}
			
		}, 105, 97, TimeUnit.MILLISECONDS);
		scheduler.schedule(new Runnable(){

			@Override
			public void run() {
				thread1.cancel(false);
				thread2.cancel(false);
				thread3.cancel(false);
			}
		}, 1450, TimeUnit.MILLISECONDS);
		long start = System.currentTimeMillis();
		try {
			Thread.sleep(1500);
		} catch (InterruptedException e) {
			System.out.println("Sleep interrupted after:"+(System.currentTimeMillis()-start)+" ms.");
		}
		assertEquals("test",(String)context.get("test1"));
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
