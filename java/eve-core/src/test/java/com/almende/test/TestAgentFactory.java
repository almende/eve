package com.almende.test;

import junit.framework.TestCase;

import org.junit.Test;

import com.almende.eve.agent.AgentFactory;
import com.almende.eve.agent.example.TestAgentInterface;

public class TestAgentFactory extends TestCase {

	@Test
	public void testAgentCall() {
		AgentFactory factory = new AgentFactory();
		
		TestAgentInterface agent = factory.createAgentProxy(null, 
				"http://eveagents.appspot.com/agents/testagent/1/", 
				TestAgentInterface.class);
		
		Double res = agent.add(3.1, 4.2);
		assertEquals(new Double(7.3),res);
		
		res = agent.multiply(3.1, 4.2);
		assertEquals(new Double(13.02),res);
		
	}
}
