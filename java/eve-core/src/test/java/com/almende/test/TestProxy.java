/*
 * Copyright: Almende B.V. (2014), Rotterdam, The Netherlands
 * License: The Apache Software License, Version 2.0
 */
package com.almende.test;

import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Future;

import junit.framework.TestCase;

import org.junit.Test;

import com.almende.eve.agent.AgentHost;
import com.almende.eve.agent.AsyncProxy;
import com.almende.eve.state.FileStateFactory;
import com.almende.test.agents.TestAgent;
import com.almende.test.agents.TestInterface;
import com.almende.test.agents.entity.Person;

/**
 * The Class TestProxy.
 */
public class TestProxy extends TestCase {
	
	/**
	 * Test proxy.
	 *
	 * @throws Exception the exception
	 */
	@Test
	public void testProxy() throws Exception {
		// Create TestAgent according to TestInterface
		final AgentHost host = AgentHost.getInstance();
		final FileStateFactory stateFactory = new FileStateFactory(".eveagents");
		host.setStateFactory(stateFactory);
		
		if (host.hasAgent("TestAgent")) {
			host.deleteAgent("TestAgent");
		}
		@SuppressWarnings("unused")
		final TestAgent agent = host.createAgent(TestAgent.class, "TestAgent");
		
		// generate sync proxy from TestInterface
		final TestInterface proxy = host.createAgentProxy(null,
				URI.create("local:TestAgent"), TestInterface.class);
		assertEquals("Hello world, you said: nice weather, isn't it?",
				proxy.helloWorld("nice weather, isn't it?"));
		assertEquals(15, proxy.testPrimitive(5, 10));
		proxy.testVoid();
		
		final Map<String, List<Person>> result = proxy.complexResult();
		assertEquals("Ludo", result.get("result").get(0).getName());
		
		// Generate asyncproxy from TestInterface
		final AsyncProxy<TestInterface> aProxy = host.createAsyncAgentProxy(
				null, URI.create("local:TestAgent"), TestInterface.class);
		final Future<?> res = aProxy.call("helloWorld", "hi");
		assertEquals("Hello world, you said: hi", res.get());
		final Future<?> voidRes = aProxy.call("testVoid");
		voidRes.get();
		
		final Future<?> intRes = aProxy.call("testPrimitive", 5, 10);
		assertEquals(new Integer(15), intRes.get());
		
		/*
		 * long delay=10000;
		 * 
		 * aProxy = factory.createAsyncAgentProxy(null,"local:TestAgent",
		 * TestInterface.class);
		 * aProxy = factory.createAsyncAgentProxy(null,"local:TestAgent",
		 * TestInterface.class);
		 * aProxy = factory.createAsyncAgentProxy(null,"local:TestAgent",
		 * TestInterface.class);
		 * aProxy = factory.createAsyncAgentProxy(null,"local:TestAgent",
		 * TestInterface.class);
		 * Thread.sleep(delay);
		 * 
		 * aProxy = factory.createAsyncAgentProxy(null,"local:TestAgent",
		 * TestInterface.class);
		 * aProxy = factory.createAsyncAgentProxy(null,"local:TestAgent",
		 * TestInterface.class);
		 * aProxy = factory.createAsyncAgentProxy(null,"local:TestAgent",
		 * TestInterface.class);
		 * aProxy = factory.createAsyncAgentProxy(null,"local:TestAgent",
		 * TestInterface.class);
		 * Thread.sleep(delay);
		 * 
		 * aProxy = factory.createAsyncAgentProxy(null,"local:TestAgent",
		 * TestInterface.class);
		 * aProxy = factory.createAsyncAgentProxy(null,"local:TestAgent",
		 * TestInterface.class);
		 * aProxy = factory.createAsyncAgentProxy(null,"local:TestAgent",
		 * TestInterface.class);
		 * aProxy = factory.createAsyncAgentProxy(null,"local:TestAgent",
		 * TestInterface.class);
		 * Thread.sleep(delay);
		 * 
		 * aProxy = factory.createAsyncAgentProxy(null,"local:TestAgent",
		 * TestInterface.class);
		 * aProxy = factory.createAsyncAgentProxy(null,"local:TestAgent",
		 * TestInterface.class);
		 * aProxy = factory.createAsyncAgentProxy(null,"local:TestAgent",
		 * TestInterface.class);
		 * aProxy = factory.createAsyncAgentProxy(null,"local:TestAgent",
		 * TestInterface.class);
		 * Thread.sleep(delay);
		 * 
		 * aProxy = factory.createAsyncAgentProxy(null,"local:TestAgent",
		 * TestInterface.class);
		 * aProxy = factory.createAsyncAgentProxy(null,"local:TestAgent",
		 * TestInterface.class);
		 * aProxy = factory.createAsyncAgentProxy(null,"local:TestAgent",
		 * TestInterface.class);
		 * aProxy = factory.createAsyncAgentProxy(null,"local:TestAgent",
		 * TestInterface.class);
		 * Thread.sleep(delay);
		 */
	}
	
}
