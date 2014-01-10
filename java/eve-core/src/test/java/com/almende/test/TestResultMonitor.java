/*
 * Copyright: Almende B.V. (2014), Rotterdam, The Netherlands
 * License: The Apache Software License, Version 2.0
 */
package com.almende.test;

import java.util.List;

import junit.framework.TestCase;

import org.junit.Test;

import com.almende.eve.agent.AgentHost;
import com.almende.eve.rpc.jsonrpc.JSONRequest;
import com.almende.eve.rpc.jsonrpc.jackson.JOM;
import com.almende.eve.scheduler.RunnableSchedulerFactory;
import com.almende.eve.state.FileStateFactory;
import com.almende.test.agents.TestResultMonitorAgent;

/**
 * The Class TestResultMonitor.
 */
public class TestResultMonitor extends TestCase {
	
	/**
	 * Test.
	 *
	 * @throws Exception the exception
	 */
	@Test
	public void test() throws Exception {
		final AgentHost host = AgentHost.getInstance();
		final FileStateFactory stateFactory = new FileStateFactory(
				".eveagents_resultmonitor", true);
		host.setStateFactory(stateFactory);
		// factory.setSchedulerFactory(new ClockSchedulerFactory(factory, ""));
		host.setSchedulerFactory(new RunnableSchedulerFactory(host, ""));
		
		if (host.hasAgent("alice")) {
			host.deleteAgent("alice");
		}
		if (host.hasAgent("bob")) {
			host.deleteAgent("bob");
		}
		
		final TestResultMonitorAgent alice = host.createAgent(
				TestResultMonitorAgent.class, "alice");
		final TestResultMonitorAgent bob = host.createAgent(
				TestResultMonitorAgent.class, "bob");
		
		alice.prepare();
		bob.getScheduler().createTask(
				new JSONRequest("bobEvent", JOM.createObjectNode()), 1000,
				true, false);
		
		int count = 0;
		while (count++ < 20) {
			Thread.sleep(1000);
			final List<Integer> res = alice.get_result();
			
			System.err.println(count + " Alice, from Bob:" + res.get(0) + ":"
					+ res.get(1) + ":" + res.get(2) + ":" + res.get(3));
			
		}
		
		alice.tear_down();
		
		host.deleteAgent("alice");
		host.deleteAgent("bob");
	}
	
}
