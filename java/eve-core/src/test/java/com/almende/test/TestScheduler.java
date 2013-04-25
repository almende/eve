package com.almende.test;

import java.util.logging.Logger;

import junit.framework.TestCase;

import org.joda.time.DateTime;
import org.junit.Test;

import com.almende.eve.agent.AgentFactory;
import com.almende.eve.scheduler.ClockSchedulerFactory;
import com.almende.eve.scheduler.RunnableSchedulerFactory;
import com.almende.eve.scheduler.Scheduler;
import com.almende.eve.state.FileStateFactory;
import com.almende.eve.transport.http.HttpService;
import com.almende.test.agents.TestSchedulerAgent;

public class TestScheduler extends TestCase {
	static final Logger	log	= Logger.getLogger("testScheduler");
	
	@Test
	public void testScheduler() throws Exception {
		schedule(false);
		
		Thread.sleep(2000);
		
		schedule(true);
		
	}
	
	public void schedule(boolean clock) throws Exception {
		AgentFactory af = AgentFactory.getInstance();
		if (af == null) {
			af = AgentFactory.createInstance();
			af.setStateFactory(new FileStateFactory(".eveagents"));
			af.addTransportService(new HttpService());
		}
		if (clock) {
			af.setSchedulerFactory(new ClockSchedulerFactory(af, ""));
		} else {
			af.setSchedulerFactory(new RunnableSchedulerFactory(af,
					"_runnableScheduler"));
		}
		
		String agentIds[] = { "myTest1", "myTest2", "myTest3" };
		
		for (String agentId : agentIds) {
			if (af.hasAgent(agentId)) {
				log.severe("Agent:" + agentId + " exists, removing....");
				af.deleteAgent(agentId);
			}
			log.info("Setup agent:" + agentId);
			TestSchedulerAgent agent = (TestSchedulerAgent) af.createAgent(
					TestSchedulerAgent.class, agentId);
			agent.resetCount();
		}
		DateTime start = DateTime.now();
		log.info("Start:" + start);
		for (String agentId : agentIds) {
			TestSchedulerAgent agent = (TestSchedulerAgent) af
					.getAgent(agentId);
			agent.setTest(agentId, 500, true, false);
			agent.setTest(agentId, 1000, true, false);
			agent.setTest(agentId, 2000, false, false);
			
			agent.setTest(agentIds[0], 900, false, false);
			agent.setTest(agentIds[0], 1500, false, false);
			agent.setTest(agentIds[0], 2500, false, false);
			
			agent.setTest(agentIds[1], 1100, true, true);
			agent.setTest(agentIds[1], 3300, false, false);
			agent.setTest(agentIds[1], 3500, false, false);
			
			agent.setTest(agentIds[2], 2300, false, false);
			agent.setTest(agentIds[2], 4000, false, false);
			agent.setTest(agentIds[2], 4500, false, false);
			
			// System.err.println(agent.getScheduler().toString());
		}
		while (start.plus(3000).isAfterNow()) {
			Thread.sleep(500);
		}
		for (String agentId : agentIds) {
			TestSchedulerAgent agent = (TestSchedulerAgent) af
					.getAgent(agentId);
			Scheduler scheduler = agent.getScheduler();
			
			log.info("Agent " + agentId + " ran " + agent.getCount()
					+ " tasks.");
			log.info("Tasks left:" + scheduler.getTasks());
			
			for (String taskId : scheduler.getTasks()) {
				scheduler.cancelTask(taskId);
			}
			agent.resetCount();
		}
		Thread.sleep(1000);
		for (String agentId : agentIds) {
			TestSchedulerAgent agent = (TestSchedulerAgent) af
					.getAgent(agentId);
			Scheduler scheduler = agent.getScheduler();
			log.info("Agent " + agentId + " ran still: " + agent.getCount()
					+ " tasks after cancel.");
			log.info("Tasks left:" + scheduler.getTasks());
		}
	}
}
