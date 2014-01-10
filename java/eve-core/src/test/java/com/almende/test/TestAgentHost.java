/*
 * Copyright: Almende B.V. (2014), Rotterdam, The Netherlands
 * License: The Apache Software License, Version 2.0
 */
package com.almende.test;

import java.net.URI;
import java.util.logging.Logger;

import junit.framework.TestCase;

import org.junit.Test;

import com.almende.eve.agent.AgentHost;
import com.almende.eve.state.FileStateFactory;
import com.almende.test.agents.Test2Agent;
import com.almende.test.agents.Test2AgentInterface;

/**
 * The Class TestAgentHost.
 */
public class TestAgentHost extends TestCase {
	private static final Logger	LOG	= Logger.getLogger("testAgentHost");
	
	/**
	 * Test agent call.
	 * 
	 * @throws Exception
	 *             the exception
	 */
	@Test
	public void testAgentCall() throws Exception {
		final String TESTAGENT = "hostTestAgent";
		
		LOG.warning(this.getClass().getName() + ":"
				+ this.getClass().getClassLoader().hashCode());
		final AgentHost host = AgentHost.getInstance();
		final FileStateFactory stateFactory = new FileStateFactory(".eveagents");
		host.setStateFactory(stateFactory);
		
		if (host.hasAgent(TESTAGENT)) {
			host.deleteAgent(TESTAGENT);
		}
		host.createAgent(Test2Agent.class, TESTAGENT);
		
		LOG.warning("Creating agentProxy");
		Test2AgentInterface agent = host.createAgentProxy(null,
				URI.create("local:" + TESTAGENT), Test2AgentInterface.class);
		LOG.warning("Starting agentProxy");
		Double res = agent.add(3.1, 4.2);
		// result not exact due to intermediate binary representation
		assertEquals(new Double(7.300000000000001), res);
		res = agent.multiply(3.1, 4.2);
		assertEquals(new Double(13.020000000000001), res);
		
		agent = host.createAgentProxy(null,
				URI.create("https://localhost:8443/agents/" + TESTAGENT + "/"),
				Test2AgentInterface.class);
		
		LOG.warning("checking local https call 1:");
		res = agent.add(3.1, 4.2);
		// result not exact due to intermediate binary representation
		assertEquals(new Double(7.300000000000001), res);
		
		LOG.warning("checking local https call 2:");
		res = agent.multiply(3.1, 4.2);
		assertEquals(new Double(13.020000000000001), res);
		
		host.deleteAgent(TESTAGENT);
	}
	
}
