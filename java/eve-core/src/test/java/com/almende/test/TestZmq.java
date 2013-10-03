package com.almende.test;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import junit.framework.TestCase;

import org.eclipse.jetty.util.ConcurrentHashSet;
import org.junit.Test;

import com.almende.eve.agent.AgentHost;
import com.almende.eve.rpc.jsonrpc.jackson.JOM;
import com.almende.eve.state.MemoryStateFactory;
import com.almende.eve.transport.AsyncCallback;
import com.almende.eve.transport.zmq.ZmqService;
import com.almende.test.agents.Test2Agent;
import com.fasterxml.jackson.databind.node.ObjectNode;

public class TestZmq extends TestCase {
	private static final Logger	LOG	= Logger.getLogger(TestZmq.class
											.getCanonicalName());
	
	@Test
	public void testZmq() throws Exception {
		AgentHost host = AgentHost.getInstance();
		host.setDoesShortcut(false);
		host.setStateFactory(new MemoryStateFactory());
		Map<String, Object> params = new HashMap<String, Object>();
		params.put("baseUrl", "tcp://127.0.0.1");
		params.put("basePort", 5555);
		host.addTransportService(new ZmqService(host, params));
		
		Test2Agent test = host.createAgent(Test2Agent.class, "test");
		Test2Agent test2 = host.createAgent(Test2Agent.class, "test2");
		
		
		final Set<String> results = new ConcurrentHashSet<String>();
		
		AsyncCallback<String> callback = new AsyncCallback<String>() {
			
			@Override
			public void onSuccess(String result) {
				results.add(result);
			}
			
			@Override
			public void onFailure(Exception e) {
				LOG.log(Level.SEVERE, "Oeps, exception:", e);
			}
			
		};
		
		ObjectNode parms = JOM.createObjectNode();
		parms.put("message", "1");
		test.sendAsync(URI.create("zmq:tcp://127.0.0.1:5556"), "slowPing",
				parms, callback, String.class);
		
		parms = JOM.createObjectNode();
		parms.put("message", "2");
		test.sendAsync(URI.create("zmq:tcp://127.0.0.1:5556"), "slowPing",
				parms, callback, String.class);

		parms = JOM.createObjectNode();
		parms.put("message", "3");
		test.sendAsync(URI.create("zmq:tcp://127.0.0.1:5555"), "slowPing",
				parms, callback, String.class);
		
		parms = JOM.createObjectNode();
		parms.put("message", "4");
		test.sendAsync(URI.create("zmq:tcp://127.0.0.1:5555"), "slowPing",
				parms, callback, String.class);
		
		
		parms = JOM.createObjectNode();
		parms.put("message", "5");
		test2.sendAsync(URI.create("zmq:tcp://127.0.0.1:5555"), "slowPing",
				parms, callback, String.class);
		
		parms = JOM.createObjectNode();
		parms.put("message", "6");
		test2.sendAsync(URI.create("zmq:tcp://127.0.0.1:5555"), "slowPing",
				parms, callback, String.class);

		parms = JOM.createObjectNode();
		parms.put("message", "7");
		test2.sendAsync(URI.create("zmq:tcp://127.0.0.1:5556"), "slowPing",
				parms, callback, String.class);
		
		parms = JOM.createObjectNode();
		parms.put("message", "8");
		test2.sendAsync(URI.create("zmq:tcp://127.0.0.1:5556"), "slowPing",
				parms, callback, String.class);
		Thread.sleep(2000);
		
		System.err.println("results:"+results);
		assertEquals(8,results.size());
		
	}
	
}
