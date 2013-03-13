package com.almende.test;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import junit.framework.TestCase;

import org.junit.Test;

import com.almende.eve.agent.AgentFactory;
import com.almende.eve.transport.SyncCallback;
import com.almende.test.agents.Test2AgentInterface;

public class TestAgentFactory extends TestCase {

	@Test
	public void testAgentCall() {
		AgentFactory factory = new AgentFactory();
		
		Test2AgentInterface agent = factory.createAgentProxy(null, 
				"http://eveagents.appspot.com/agents/test/", 
				Test2AgentInterface.class);
		
		Double res = agent.add(3.1, 4.2);
		assertEquals(new Double(7.300000000000001),res); //result not exact due to intermediate binary representation
		
		res = agent.multiply(3.1, 4.2);
		assertEquals(new Double(13.020000000000001),res);

		agent = factory.createAgentProxy(null, 
				"https://localhost:8443/agents/test/", 
				Test2AgentInterface.class);
		
		res = agent.add(3.1, 4.2);
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
