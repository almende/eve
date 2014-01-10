/*
 * Copyright: Almende B.V. (2014), Rotterdam, The Netherlands
 * License: The Apache Software License, Version 2.0
 */
package com.almende.test;

import java.util.logging.Logger;

import junit.framework.TestCase;

import org.joda.time.DateTime;
import org.junit.Test;

import com.almende.eve.agent.AgentHost;
import com.almende.eve.scheduler.ClockSchedulerFactory;
import com.almende.eve.scheduler.RunnableSchedulerFactory;
import com.almende.eve.scheduler.Scheduler;
import com.almende.eve.state.FileStateFactory;
import com.almende.eve.transport.http.HttpService;
import com.almende.test.agents.TestSchedulerAgent;

/**
 * The Class TestScheduler.
 */
public class TestScheduler extends TestCase {
	private static final Logger	LOG	= Logger.getLogger("testScheduler");
	
	/**
	 * Test single shot.
	 * 
	 * @throws Exception
	 *             the exception
	 */
	@Test
	public void testSingleShot() throws Exception {
		final AgentHost host = AgentHost.getInstance();
		host.setStateFactory(new FileStateFactory(".eveagents_schedulerTest"));
		host.addTransportService(new HttpService(host));
		host.setSchedulerFactory(new ClockSchedulerFactory(host, ""));
		
		if (host.hasAgent("SingleShot")) {
			LOG.severe("Removing old agent");
			host.deleteAgent("SingleShot");
			LOG.severe("Removed old agent");
		}
		LOG.severe("Setup new Agent");
		final TestSchedulerAgent test = host.createAgent(
				TestSchedulerAgent.class, "SingleShot");
		LOG.severe("Running test");
		
		test.setTest("SingleShot", 5000, false, false);
		LOG.severe("Sleep");
		
		Thread.sleep(2000);
		test.setTest("SingleShot", 5000, false, false);
		LOG.severe("More Sleep");
		
		Thread.sleep(10000);
		host.deleteAgent("SingleShot");
	}
	
	/**
	 * Test scheduler.
	 * 
	 * @throws Exception
	 *             the exception
	 */
	@Test
	public void testScheduler() throws Exception {
		schedule(false);
		
		Thread.sleep(2000);
		
		schedule(true);
		
	}
	
	/**
	 * Schedule.
	 * 
	 * @param clock
	 *            the clock
	 * @throws Exception
	 *             the exception
	 */
	public void schedule(final boolean clock) throws Exception {
		final AgentHost host = AgentHost.getInstance();
		host.setStateFactory(new FileStateFactory(".eveagents_schedulerTest"));
		host.addTransportService(new HttpService(host));
		
		if (clock) {
			host.setSchedulerFactory(new ClockSchedulerFactory(host, ""));
		} else {
			host.setSchedulerFactory(new RunnableSchedulerFactory(host,
					"_runnableScheduler"));
		}
		
		final String agentIds[] = { "myTest1", "myTest2", "myTest3" };
		
		for (final String agentId : agentIds) {
			if (host.hasAgent(agentId)) {
				LOG.severe("Agent:" + agentId + " exists, removing....");
				host.deleteAgent(agentId);
			}
			LOG.info("Setup agent:" + agentId);
			final TestSchedulerAgent agent = host.createAgent(
					TestSchedulerAgent.class, agentId);
			agent.resetCount();
		}
		final DateTime start = DateTime.now();
		LOG.info("Start:" + start);
		for (final String agentId : agentIds) {
			final TestSchedulerAgent agent = (TestSchedulerAgent) host
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
		for (final String agentId : agentIds) {
			final TestSchedulerAgent agent = (TestSchedulerAgent) host
					.getAgent(agentId);
			final Scheduler scheduler = agent.getScheduler();
			
			LOG.info("Agent " + agentId + " ran " + agent.getCount()
					+ " tasks.");
			LOG.info("Tasks left:" + scheduler.getTasks());
			
			scheduler.cancelAllTasks();
			agent.resetCount();
		}
		Thread.sleep(1000);
		for (final String agentId : agentIds) {
			final TestSchedulerAgent agent = (TestSchedulerAgent) host
					.getAgent(agentId);
			final Scheduler scheduler = agent.getScheduler();
			LOG.info("Agent " + agentId + " ran still: " + agent.getCount()
					+ " tasks after cancel.");
			LOG.info("Tasks left:" + scheduler.getTasks());
			host.deleteAgent(agentId);
		}
	}
}
