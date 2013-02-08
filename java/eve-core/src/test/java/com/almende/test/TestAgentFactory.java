package com.almende.test;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import junit.framework.TestCase;

import org.junit.Test;

import com.almende.eve.agent.AgentFactory;
import com.almende.eve.agent.example.TestAgentInterface;
import com.almende.eve.transport.SyncCallback;

public class TestAgentFactory extends TestCase {

	@Test
	public void testAgentCall() {
		AgentFactory factory = new AgentFactory();
		
		TestAgentInterface agent = factory.createAgentProxy(null, 
				"http://eveagents.appspot.com/agents/testagent/1/", 
				TestAgentInterface.class);
		
		Double res = agent.add(3.1, 4.2);
		assertEquals(new Double(7.300000000000001),res); //result not exact due to intermediate binary representation
		
		res = agent.multiply(3.1, 4.2);
		assertEquals(new Double(13.020000000000001),res);
		
	}
	
	@Test
	public void testSyncCall(){
		ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(10);
		final SyncCallback<Integer> callback1 = new SyncCallback<Integer>();
		final SyncCallback<Integer> callback2 = new SyncCallback<Integer>();
		assertNotSame(callback1,callback2);
		
		scheduler.schedule(new Runnable(){

			@Override
			public void run() {
				System.err.println("Send something to callback 1");
				callback1.onSuccess(1);
			}
			
		}, 900, TimeUnit.MILLISECONDS);
		scheduler.schedule(new Runnable(){

			@Override
			public void run() {
			System.err.println("Send something to callback 2");
			callback2.onSuccess(1);
			}
			
		}, 500, TimeUnit.MILLISECONDS);
		scheduler.schedule(new Runnable(){

			@Override
			public void run() {
				System.err.println("Starting waiting for callback 2");
				try {
					Integer res = callback2.get();
					assertEquals(new Integer(1),res);
				} catch (Exception e) {
					e.printStackTrace();
				}				
			}
			
		}, 100, TimeUnit.MILLISECONDS);
		System.err.println("Starting waiting for callback 1");
		try {
			Integer res = callback1.get();
			assertEquals(new Integer(1),res);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
