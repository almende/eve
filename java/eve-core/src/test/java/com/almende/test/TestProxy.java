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

public class TestProxy extends TestCase {

	@Test
	public void testProxy() throws Exception {
		//Create TestAgent according to TestInterface
		AgentHost factory = AgentHost.getInstance();
		FileStateFactory stateFactory = new FileStateFactory(".eveagents");
		factory.setStateFactory(stateFactory);
		
		if (factory.hasAgent("TestAgent")){
			factory.deleteAgent("TestAgent");
		}
		@SuppressWarnings("unused")
		TestAgent agent = factory.createAgent(TestAgent.class, "TestAgent");
		
		//generate sync proxy from TestInterface
		TestInterface proxy = factory.createAgentProxy(null, URI.create("local:TestAgent"), TestInterface.class);
		assertEquals("Hello world, you said: nice weather, isn't it?",proxy.helloWorld("nice weather, isn't it?"));
		assertEquals(15,proxy.testPrimitive(5,10));
		proxy.testVoid();

		
		Map<String, List<Person>> result = proxy.complexResult();
		assertEquals("Ludo", result.get("result").get(0).getName());
		
		//Generate asyncproxy from TestInterface
		AsyncProxy<TestInterface> aProxy = factory.createAsyncAgentProxy(null,URI.create("local:TestAgent"), TestInterface.class);
		Future<?> res = aProxy.call("helloWorld","hi");
		assertEquals("Hello world, you said: hi",res.get());
		Future<?> voidRes = aProxy.call("testVoid");
		voidRes.get();
		
		Future<?> intRes = aProxy.call("testPrimitive",5,10);
		assertEquals(new Integer(15),intRes.get());
		
/*		long delay=10000;
		
		aProxy = factory.createAsyncAgentProxy(null,"local:TestAgent", TestInterface.class);
		aProxy = factory.createAsyncAgentProxy(null,"local:TestAgent", TestInterface.class);
		aProxy = factory.createAsyncAgentProxy(null,"local:TestAgent", TestInterface.class);
		aProxy = factory.createAsyncAgentProxy(null,"local:TestAgent", TestInterface.class);
		Thread.sleep(delay);
		
		aProxy = factory.createAsyncAgentProxy(null,"local:TestAgent", TestInterface.class);
		aProxy = factory.createAsyncAgentProxy(null,"local:TestAgent", TestInterface.class);
		aProxy = factory.createAsyncAgentProxy(null,"local:TestAgent", TestInterface.class);
		aProxy = factory.createAsyncAgentProxy(null,"local:TestAgent", TestInterface.class);
		Thread.sleep(delay);
		
		aProxy = factory.createAsyncAgentProxy(null,"local:TestAgent", TestInterface.class);
		aProxy = factory.createAsyncAgentProxy(null,"local:TestAgent", TestInterface.class);
		aProxy = factory.createAsyncAgentProxy(null,"local:TestAgent", TestInterface.class);
		aProxy = factory.createAsyncAgentProxy(null,"local:TestAgent", TestInterface.class);
		Thread.sleep(delay);
		
		aProxy = factory.createAsyncAgentProxy(null,"local:TestAgent", TestInterface.class);
		aProxy = factory.createAsyncAgentProxy(null,"local:TestAgent", TestInterface.class);
		aProxy = factory.createAsyncAgentProxy(null,"local:TestAgent", TestInterface.class);
		aProxy = factory.createAsyncAgentProxy(null,"local:TestAgent", TestInterface.class);
		Thread.sleep(delay);
		
		aProxy = factory.createAsyncAgentProxy(null,"local:TestAgent", TestInterface.class);
		aProxy = factory.createAsyncAgentProxy(null,"local:TestAgent", TestInterface.class);
		aProxy = factory.createAsyncAgentProxy(null,"local:TestAgent", TestInterface.class);
		aProxy = factory.createAsyncAgentProxy(null,"local:TestAgent", TestInterface.class);
		Thread.sleep(delay);
*/		
	}

}
