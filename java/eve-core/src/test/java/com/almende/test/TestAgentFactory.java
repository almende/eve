package com.almende.test;

import java.net.URI;

import junit.framework.TestCase;

import org.junit.Test;

import com.almende.eve.agent.AgentHost;
import com.almende.eve.state.FileStateFactory;
import com.almende.test.agents.Test2AgentInterface;

public class TestAgentFactory extends TestCase {

	@Test
	public void testAgentCall() throws Exception {

		System.err.println(this.getClass().getName() + ":"+this.getClass().getClassLoader().hashCode());
		AgentHost factory = AgentHost.getInstance();
		FileStateFactory stateFactory = new FileStateFactory(".eveagents");
		factory.setStateFactory(stateFactory);
		
		Test2AgentInterface agent = factory.createAgentProxy(null, 
				URI.create("local:test"), 
				Test2AgentInterface.class);
		
		Double res = agent.add(3.1, 4.2);
		//result not exact due to intermediate binary representation
		assertEquals(new Double(7.300000000000001),res); 
		res = agent.multiply(3.1, 4.2);
		assertEquals(new Double(13.020000000000001),res);

		agent = factory.createAgentProxy(null, 
				URI.create("https://localhost:8443/agents/test/"), 
				Test2AgentInterface.class);
		
		System.err.println("checking local https call 1:");
		res = agent.add(3.1, 4.2);
		//result not exact due to intermediate binary representation
		assertEquals(new Double(7.300000000000001),res); 
		
		System.err.println("checking local https call 2:");
		res = agent.multiply(3.1, 4.2);
		assertEquals(new Double(13.020000000000001),res);
		System.err.println("Done");
		
	}
	
}
