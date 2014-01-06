package com.almende.test;

import junit.framework.TestCase;

import org.junit.Test;

import com.almende.eve.agent.AgentHostDefImpl;
import com.almende.eve.state.FileStateFactory;
import com.almende.eve.transport.http.HttpService;
import com.almende.test.agents.TestAccessAgent;

public class TestAccess extends TestCase {
	static final String TEST1 = "TestAccessAgent";
	static final String TEST2 = "trustedSender";

	@Test
	public void testAccess() throws Exception {
		AgentHostDefImpl host = AgentHostDefImpl.getInstance();
		FileStateFactory stateFactory = new FileStateFactory(".eveagents");
		host.setStateFactory(stateFactory);
		host.addTransportService(new HttpService(host,"http://localhost:8080/agents"));
		host.addTransportService(new HttpService(host,"https://localhost:8443/agents"));

		TestAccessAgent testAgent;
		if (host.hasAgent(TEST1)) {
			testAgent = (TestAccessAgent) host.getAgent(TEST1);
		} else {
			testAgent = host.createAgent(TestAccessAgent.class, TEST1);
		}
		System.err.println("testAgent:"+testAgent.getId()+":"+testAgent.getUrls());
		TestAccessAgent agent;
		if (host.hasAgent(TEST2)) {
			agent = (TestAccessAgent) host.getAgent(TEST2);
		} else {
			agent = (TestAccessAgent) host.createAgent(
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
