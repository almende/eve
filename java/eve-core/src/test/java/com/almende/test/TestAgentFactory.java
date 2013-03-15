package com.almende.test;

import junit.framework.TestCase;

import org.junit.Test;

import com.almende.eve.agent.AgentFactory;
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
		
		System.err.println("checking local https call 1:");
		res = agent.add(3.1, 4.2);
		assertEquals(new Double(7.300000000000001),res); //result not exact due to intermediate binary representation
		
		System.err.println("checking local https call 2:");
		res = agent.multiply(3.1, 4.2);
		assertEquals(new Double(13.020000000000001),res);
		System.err.println("Done");
		
	}
	
}
