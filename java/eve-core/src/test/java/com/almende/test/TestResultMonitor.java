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

public class TestResultMonitor extends TestCase {
	
	@Test
	public void test() throws Exception {
		AgentHost factory = AgentHost.getInstance();
		FileStateFactory stateFactory = new FileStateFactory(".eveagents_resultmonitor", true);
		factory.setStateFactory(stateFactory);
		//factory.setSchedulerFactory(new ClockSchedulerFactory(factory, ""));
		factory.setSchedulerFactory(new RunnableSchedulerFactory(factory, ""));
		
		if (factory.hasAgent("alice")) factory.deleteAgent("alice");
		if (factory.hasAgent("bob")) factory.deleteAgent("bob");
		
		TestResultMonitorAgent alice = factory.createAgent(
				TestResultMonitorAgent.class, "alice");
		TestResultMonitorAgent bob = factory.createAgent(TestResultMonitorAgent.class,
				"bob");
		
		alice.prepare();
		bob.getScheduler().createTask(
				new JSONRequest("bobEvent", JOM.createObjectNode()), 1000,
				true, false);
		
		int count = 0;
		while (count++ < 20) {
			Thread.sleep(1000);
			List<Integer> res = alice.get_result();
			
			System.err.println(count + " Alice, from Bob:" + res.get(0) + ":"
					+ res.get(1) + ":" + res.get(2) + ":" + res.get(3));
			
		}
		
		alice.tear_down();
		
		factory.deleteAgent("alice");
		factory.deleteAgent("bob");
	}
	
}
