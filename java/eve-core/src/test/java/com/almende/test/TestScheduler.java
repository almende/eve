package com.almende.test;

import junit.framework.TestCase;

import org.joda.time.DateTime;
import org.junit.Test;

import com.almende.eve.agent.AgentFactory;
import com.almende.eve.scheduler.ClockSchedulerFactory;
import com.almende.eve.state.FileStateFactory;
import com.almende.eve.transport.http.HttpService;
import com.almende.test.agents.TestSchedulerAgent;

public class TestScheduler extends TestCase {

	@Test
	public void testScheduler() throws Exception {
		AgentFactory af = AgentFactory.getInstance();
		if (af == null) {
			af = AgentFactory.createInstance();
			af.setStateFactory(new FileStateFactory(".eveagents"));
			af.addTransportService(new HttpService());
		}
		af.setSchedulerFactory(new ClockSchedulerFactory(af, ""));
		// af.setSchedulerFactory(new
		// RunnableSchedulerFactory(af,"_runnableScheduler"));
		String agentIds[] = { "myTest1", "myTest2", "myTest3" };

		for (String agentId : agentIds) {
			if (af.hasAgent(agentId)) {
				System.err
						.println("Agent:" + agentId + " exists, removing....");
				af.deleteAgent(agentId);
			}
			System.err.println("Setup agent:" + agentId);
			TestSchedulerAgent agent = (TestSchedulerAgent) af.createAgent(TestSchedulerAgent.class, agentId);
			agent.resetCount();
		}
		for (String agentId : agentIds) {
			TestSchedulerAgent agent = (TestSchedulerAgent) af
					.getAgent(agentId);
			agent.setTest(agentId, 1000);
			agent.setTest(agentIds[0], 1500);
			agent.setTest(agentIds[1], 3000);
			agent.setTest(agentIds[2], 4500);
			agent.setTest(agentId, 500);
			agent.setTest(agentIds[0], 2500);
			agent.setTest(agentIds[1], 3500);
			agent.setTest(agentIds[2], 4000);
			agent.setTest(agentId, 2000);
			agent.setTest(agentIds[0], 900);
			agent.setTest(agentIds[1], 3300);
			agent.setTest(agentIds[2], 2300);
			
			System.err.println(agent.getScheduler().toString());
		}
		DateTime start = DateTime.now();
		System.err.println("Start:" + start);
		while (start.plus(5000).isAfterNow()) {
			Thread.sleep(500);
		}
		/*
		for (String agentId : agentIds) {
			if (af.hasAgent(agentId)) {
				System.err
						.println("Drop Agent:" + agentId + ".");
				af.deleteAgent(agentId);
			}
		}
		for (String agentId : agentIds) {
			System.err.println("Setup agent:" + agentId);
			TestSchedulerAgent agent = (TestSchedulerAgent) af.createAgent(TestSchedulerAgent.class, agentId);
			agent.getScheduler();
		}
		for (String agentId : agentIds) {
			System.err.println("Agent " + agentId + " ran "
					+ ((TestSchedulerAgent) af.getAgent(agentId)).getCount()
					+ " tasks.");
			System.err.println("Tasks left:"
					+ af.getAgent(agentId).getScheduler().getTasks());
		}*/
	}
}
