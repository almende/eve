package com.almende.test;

import java.net.ProtocolException;
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
import com.almende.eve.agent.callback.AsyncCallback;
import com.almende.eve.rpc.jsonrpc.JSONRPCException;
import com.almende.eve.rpc.jsonrpc.jackson.JOM;
import com.almende.eve.state.MemoryStateFactory;
import com.almende.eve.transport.zmq.ZmqService;
import com.almende.test.agents.Test2Agent;
import com.fasterxml.jackson.databind.node.ObjectNode;


public class TestZmq extends TestCase {
	private static final Logger	LOG	= Logger.getLogger(TestZmq.class
											.getCanonicalName());
	
	private URI getUrl(String type,String agentId){
		if ("tcp".equals(type)){
			int port = agentId.equals("test")?5555:5556;
			return URI.create("zmq:tcp://127.0.0.1:"+port);
		} else if ("inproc".equals(type)){
			return URI.create("zmq:inproc://"+agentId);
		} else if ("ipc".equals(type)){
			return URI.create("zmq:ipc:///tmp/"+agentId);
		}
		return null;
	}
	
	private void runTest(Test2Agent test, Test2Agent test2, String type) throws ProtocolException, JSONRPCException, InterruptedException{
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
		test.sendAsync(getUrl(type,test2.getId()), "slowPing",
				parms, callback, String.class);
		
		parms = JOM.createObjectNode();
		parms.put("message", "2");
		test.sendAsync(getUrl(type,test2.getId()), "slowPing",
				parms, callback, String.class);

		parms = JOM.createObjectNode();
		parms.put("message", "3");
		test.sendAsync(getUrl(type,test.getId()), "slowPing",
				parms, callback, String.class);
		
		parms = JOM.createObjectNode();
		parms.put("message", "4");
		test.sendAsync(getUrl(type,test.getId()), "slowPing",
				parms, callback, String.class);
		
		
		parms = JOM.createObjectNode();
		parms.put("message", "5");
		test2.sendAsync(getUrl(type,test.getId()), "slowPing",
				parms, callback, String.class);
		
		parms = JOM.createObjectNode();
		parms.put("message", "6");
		test2.sendAsync(getUrl(type,test.getId()), "slowPing",
				parms, callback, String.class);

		parms = JOM.createObjectNode();
		parms.put("message", "7");
		test2.sendAsync(getUrl(type,test2.getId()), "slowPing",
				parms, callback, String.class);
		
		parms = JOM.createObjectNode();
		parms.put("message", "8");
		test2.sendAsync(getUrl(type,test2.getId()), "slowPing",
				parms, callback, String.class);
		Thread.sleep(2000);
		
		System.err.println("results:"+results);
		assertEquals(8,results.size());

	}
	
	@Test
	public void testZmq() throws Exception {
		AgentHost host = AgentHost.getInstance();
		host.setDoesShortcut(false);
		host.setStateFactory(new MemoryStateFactory());
		Map<String, Object> params = new HashMap<String, Object>();
		params.put("baseUrl", "tcp://127.0.0.1:5555");
		host.addTransportService(new ZmqService(host, params));
		
		params = new HashMap<String, Object>();
		params.put("baseUrl", "inproc://");
		host.addTransportService(new ZmqService(host, params));

		params = new HashMap<String, Object>();
		params.put("baseUrl", "ipc:///tmp/");
		host.addTransportService(new ZmqService(host, params));

		Test2Agent test = host.createAgent(Test2Agent.class, "test");
		Test2Agent test2 = host.createAgent(Test2Agent.class, "test2");
		
		runTest(test,test2,"tcp");
		runTest(test,test2,"inproc");
		runTest(test,test2,"ipc");
	}
	
}
