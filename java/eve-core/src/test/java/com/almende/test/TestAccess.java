package com.almende.test;

import junit.framework.TestCase;

import org.junit.Test;

import com.almende.eve.agent.Agent;
import com.almende.eve.agent.AgentFactory;
import com.almende.eve.transport.http.HttpService;
import com.almende.test.agents.TestAccessAgent;

public class TestAccess extends TestCase {

	@Test
	public void testAccess() throws Exception {
		AgentFactory factory = new AgentFactory();
		factory.addTransportService(new HttpService("http://localhost:8080/agents/"));
		Agent testAgent = factory.createAgent(TestAccessAgent.class, "TestAccessAgent");
		
		TestAccessAgent agent = (TestAccessAgent) factory.createAgent(TestAccessAgent.class, "trustedSender");
		boolean[] result = agent.run(testAgent.getUrls().get(0));
		assertEquals(7,result.length);
		assertEquals(true,result[0]);//allowed
		assertEquals(false,result[1]);//forbidden
		assertEquals(true,result[2]);//depends
		assertEquals(true,result[3]);//dependsTag
		assertEquals(false,result[4]);//dependsUnTag
		assertEquals(true,result[5]);//unmodified
		assertEquals(false,result[6]);//param
	}

}
