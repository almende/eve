/*
 * Copyright: Almende B.V. (2014), Rotterdam, The Netherlands
 * License: The Apache Software License, Version 2.0
 */
package com.almende.test;

import junit.framework.TestCase;

import org.junit.Test;

import com.almende.eve.agent.AgentHost;
import com.almende.eve.state.FileStateFactory;
import com.almende.eve.state.TypedKey;
import com.almende.test.agents.Test2Agent;

/**
 * The Class TestReference.
 */
public class TestReference extends TestCase {
	
	/**
	 * Test reference.
	 *
	 * @throws Exception the exception
	 */
	@Test
	public void testReference() throws Exception {
		final Object testObject = new Object();
		
		final String TEST_AGENT = "ReferenceAgent";
		final AgentHost host = AgentHost.getInstance();
		final FileStateFactory stateFactory = new FileStateFactory(".eveagents");
		host.setStateFactory(stateFactory);
		
		if (host.hasAgent(TEST_AGENT)) {
			host.deleteAgent(TEST_AGENT);
		}
		Test2Agent test = host.createAgent(Test2Agent.class, TEST_AGENT);
		
		test.putRef(new TypedKey<Object>("test") {
		}, testObject);
		
		test = (Test2Agent) host.getAgent(TEST_AGENT);
		
		assertEquals(test.getRef(new TypedKey<Object>("test") {
		}), testObject);
	}
	
}
