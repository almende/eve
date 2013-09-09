package com.almende.test;

import java.io.File;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

import junit.framework.TestCase;

import org.junit.Test;

import com.almende.eve.state.ConcurrentSerializableFileState;
import com.almende.eve.state.FileStateFactory;
import com.almende.eve.state.State;
import com.almende.eve.state.TypedKey;

public class TestStateLocking extends TestCase {
	//TODO: prove that a collision occurs, possibly by measuring the starttime and runtime of each run.
	//TODO: alternatively: implement a non-locking, non-thread-safe version of the state and see it break:)
	public static final Logger LOG = Logger.getLogger(TestStateLocking.class.getName());
	
	private void testRun(final State state){
		
		//state.clear();
		state.put("test", "test");
		state.put("test2", "test2");
		
		ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(10);
		final ScheduledFuture<?> thread1 = scheduler.scheduleAtFixedRate(new Runnable(){
			@Override
			public void run() {
				state.put("test","test1");
				state.put("test1","test");
				state.put("test","test1");
				state.get("test",String.class);
				state.put("test1","test");
				state.get("test1",String.class);
			}
			
		}, 0, 100, TimeUnit.MILLISECONDS);
		final ScheduledFuture<?> thread2 = scheduler.scheduleWithFixedDelay(new Runnable(){
			@Override
			public void run() {
				state.put("test","test2");
				state.put("test","test2");
				state.get("test",String.class);
				state.put("test","test2");
				state.put("test1","test");
				state.put("test1","test");
				state.get("test1",String.class);
			}
			
		}, 110, 95, TimeUnit.MILLISECONDS);
		final ScheduledFuture<?> thread3 = scheduler.scheduleWithFixedDelay(new Runnable(){
			@Override
			public void run() {
				state.put("test","test3");
				state.put("test","test3");
				state.get("test",String.class);
				state.put("test1","test");
				state.put("test","test3");
				state.put("test1","test");
				state.get("test1",String.class);
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
		assertEquals("test",state.get(new TypedKey<String>("test1")));
		assertEquals("test2",state.get("test2",String.class));
		assertTrue(state.get("test",String.class).startsWith("test"));
		
		LOG.info("Done test!");
		
	}
	
	@Test
	public void testFileState() throws Exception{
		File dir = new File(".testStates");
		if ((!dir.exists() && !dir.mkdir()) || !dir.isDirectory()) fail("Couldn't create .testStates folder");
		State fc = new ConcurrentSerializableFileState("test",".testStates/FileStateRun");
		testRun(fc);
	}
	@Test
	public void testConcurrentFileState() throws Exception{
		File dir = new File(".testStates");
		if ((!dir.exists() && !dir.mkdir()) || !dir.isDirectory()) fail("Couldn't create .testStates folder");
		FileStateFactory sf = new FileStateFactory(".testStates"); //Defaults to ConcurrentFileState
		
		String agentId = "ConcurrentFileStateRun";
		if (sf.exists(agentId)) sf.delete(agentId);
		State fc = sf.create(agentId);
		testRun(fc);
	}
	@Test
	public void testConcurrentJsonFileState() throws Exception{
		File dir = new File(".testStates");
		if ((!dir.exists() && !dir.mkdir()) || !dir.isDirectory()) fail("Couldn't create .testStates folder");
		FileStateFactory sf = new FileStateFactory(".testStates",true); //Defaults to ConcurrentFileState
		
		String agentId = "ConcurrentJsonFileStateRun";
		if (sf.exists(agentId)) sf.delete(agentId);
		State fc = sf.create(agentId);
		testRun(fc);
	}

}
