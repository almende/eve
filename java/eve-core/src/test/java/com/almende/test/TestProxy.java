package com.almende.test;

import java.util.concurrent.Future;

import junit.framework.TestCase;

import org.junit.Test;

import com.almende.eve.agent.AgentFactory;
import com.almende.test.agents.TestAgent;
import com.almende.eve.agent.proxy.AsyncProxy;
import com.almende.test.agents.TestInterface;

public class TestProxy extends TestCase {

	@Test
	public void testProxy() throws Exception {
		//Create TestAgent according to TestInterface
		AgentFactory factory = new AgentFactory();
		factory.createAgent(TestAgent.class, "TestAgent");
		
		
		//generate sync proxy from TestInterface
		TestInterface proxy = factory.createAgentProxy(null, "local:TestAgent", TestInterface.class);
		assertEquals("Hello world, you said: nice weather, isn't it?",proxy.helloWorld("nice weather, isn't it?"));
		assertEquals(15,proxy.testPrimitive(5));
		proxy.testVoid();

		
		//Generate asyncproxy from TestInterface
		AsyncProxy<TestInterface> aProxy = factory.createAsyncAgentProxy(null,"local:TestAgent", TestInterface.class);
		Future<String> res = aProxy.call(TestInterface.class.getMethod("helloWorld", String.class), new Object[]{"hi"},String.class);
		assertEquals("Hello world, you said: hi",res.get());
		Future<Void> voidRes = aProxy.call(TestInterface.class.getMethod("testVoid"),null,Void.class);
		voidRes.get();
		
		Future<Integer> intRes = aProxy.call(TestInterface.class.getMethod("testPrimitive",int.class), new Object[]{5},Integer.class);
		assertEquals(new Integer(15),intRes.get());
		
		
		//Call some functions, wait for result
		
	}

}
