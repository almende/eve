/*
 * Copyright: Almende B.V. (2014), Rotterdam, The Netherlands
 * License: The Apache Software License, Version 2.0
 */
package com.almende.test;

import junit.framework.TestCase;

import org.junit.Test;

import com.almende.eve.agent.AgentHost;
import com.almende.eve.agent.AspectAgent;
import com.almende.eve.rpc.annotation.Access;
import com.almende.eve.rpc.annotation.AccessType;
import com.almende.eve.state.FileStateFactory;

/**
 * The Class TestAspects.
 */
public class TestAspects extends TestCase {
	
	/**
	 * Test aspect.
	 *
	 * @throws Exception the exception
	 */
	@Test
	public void testAspect() throws Exception {
		final String TEST_AGENT = "AspectAgent";
		final AgentHost factory = AgentHost.getInstance();
		final FileStateFactory stateFactory = new FileStateFactory(".eveagents");
		factory.setStateFactory(stateFactory);
		
		if (factory.hasAgent(TEST_AGENT)) {
			factory.deleteAgent(TEST_AGENT);
		}
		final AspectAgent<TestAspects> agent = factory.createAspectAgent(
				this.getClass(), TEST_AGENT);
		
		final String result = agent.send(agent.getFirstUrl(), "aspect.callMe",
				String.class);
		assertEquals("Hello World", result);
	}
	
	/**
	 * Call me.
	 *
	 * @return the string
	 */
	@Access(AccessType.PUBLIC)
	public String callMe() {
		return "Hello World";
	}
}
