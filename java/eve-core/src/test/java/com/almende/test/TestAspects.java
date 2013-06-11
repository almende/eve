package com.almende.test;

import junit.framework.TestCase;

import org.junit.Test;

import com.almende.eve.agent.AgentHost;
import com.almende.eve.agent.AspectAgent;
import com.almende.eve.rpc.annotation.Access;
import com.almende.eve.rpc.annotation.AccessType;
import com.almende.eve.state.FileStateFactory;

public class TestAspects extends TestCase {

	@Test
	public void testAspect() throws Exception {
		final String TEST_AGENT = "AspectAgent"; 
		AgentHost factory = AgentHost.getInstance();
		FileStateFactory stateFactory = new FileStateFactory(".eveagents");
		factory.setStateFactory(stateFactory);
		
		if (factory.hasAgent(TEST_AGENT)) factory.deleteAgent(TEST_AGENT);
		AspectAgent<TestAspects> agent = factory.createAspectAgent(this.getClass(), TEST_AGENT);
		
		String result  = agent.send(agent.getFirstUrl(),"aspect.callMe",String.class);
		assertEquals("Hello World",result);
	}
	
	@Access(AccessType.PUBLIC)
	public String callMe(){
		return "Hello World";
	}
}
