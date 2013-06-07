package com.almende.test;

import junit.framework.TestCase;

import org.junit.Test;

import com.almende.eve.agent.AgentFactory;
import com.almende.eve.agent.AspectAgent;
import com.almende.eve.rpc.annotation.Access;
import com.almende.eve.rpc.annotation.AccessType;

public class TestAspects extends TestCase {

	@Test
	public void testAspect() throws Exception {
		AgentFactory factory = AgentFactory.getInstance();
		if (factory == null){
			factory = AgentFactory.createInstance();
		}
		AspectAgent<TestAspects> agent = factory.createAspectAgent(this.getClass(), "AspectAgent");
		
		String result  = agent.send(agent.getFirstUrl(),"sub.callMe",String.class);
		assertEquals("Hello World",result);
	}
	
	@Access(AccessType.PUBLIC)
	public String callMe(){
		return "Hello World";
	}
}
