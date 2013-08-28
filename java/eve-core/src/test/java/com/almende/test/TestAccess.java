package com.almende.test;

import junit.framework.TestCase;

import org.junit.Test;

import com.almende.eve.agent.AgentHost;
import com.almende.eve.config.Config;
import com.almende.eve.state.FileStateFactory;
import com.almende.test.agents.TestAccessAgent;

public class TestAccess extends TestCase {
	static final String TEST1 = "TestAccessAgent";
	static final String TEST2 = "trustedSender";

	@Test
	public void testAccess() throws Exception {
		AgentHost factory = AgentHost.getInstance();
		FileStateFactory stateFactory = new FileStateFactory(".eveagents");
		factory.setStateFactory(stateFactory);
		
		String filename = "eve.yaml";
		String fullname = "src/test/webapp/WEB-INF/" + filename;
		Config config = new Config(fullname);
		factory.loadConfig(config);
		TestAccessAgent testAgent;
		if (factory.hasAgent(TEST1)) {
			testAgent = (TestAccessAgent) factory.getAgent(TEST1);
		} else {
			testAgent = factory.createAgent(TestAccessAgent.class, TEST1);
		}
		System.err.println("testAgent:"+testAgent.getId()+":"+testAgent.getUrls());
		TestAccessAgent agent;
		if (factory.hasAgent(TEST2)) {
			agent = (TestAccessAgent) factory.getAgent(TEST2);
		} else {
			agent = (TestAccessAgent) factory.createAgent(
					TestAccessAgent.class, TEST2);
		}
		boolean[] result = agent.run((String)testAgent.getUrls().get(0));
		assertEquals(8, result.length);
		assertEquals(true, result[0]);// allowed
		assertEquals(false, result[1]);// forbidden
		assertEquals(true, result[2]);// depends
		assertEquals(true, result[3]);// dependsTag
		assertEquals(false, result[4]);// dependsUnTag
		assertEquals(true, result[5]);// unmodified
		assertEquals(false, result[6]);// param
		assertEquals(false, result[7]);// self
		
		// retry though non-local URL (https in this case);
		result = agent
				.run("https://localhost:8443/agents/" + testAgent.getId());
		assertEquals(8, result.length);
		assertEquals(true, result[0]);// allowed
		assertEquals(false, result[1]);// forbidden
		assertEquals(true, result[2]);// depends
		assertEquals(true, result[3]);// dependsTag
		assertEquals(false, result[4]);// dependsUnTag
		assertEquals(true, result[5]);// unmodified
		assertEquals(false, result[6]);// param
		assertEquals(false, result[7]);// self
		
		//retry calling itself
		result = agent.run(agent.getFirstUrl().toString());
		assertEquals(8, result.length);
		assertEquals(true, result[0]);// allowed
		assertEquals(false, result[1]);// forbidden
		assertEquals(true, result[2]);// depends
		assertEquals(true, result[3]);// dependsTag
		assertEquals(false, result[4]);// dependsUnTag
		assertEquals(true, result[5]);// unmodified
		assertEquals(false, result[6]);// param
		assertEquals(true, result[7]);// self
	}
}
