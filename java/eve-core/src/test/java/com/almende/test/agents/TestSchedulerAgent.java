/*
 * Copyright: Almende B.V. (2014), Rotterdam, The Netherlands
 * License: The Apache Software License, Version 2.0
 */
package com.almende.test.agents;

import java.util.logging.Logger;

import org.joda.time.DateTime;
import org.joda.time.Duration;

import com.almende.eve.agent.Agent;
import com.almende.eve.rpc.annotation.Access;
import com.almende.eve.rpc.annotation.AccessType;
import com.almende.eve.rpc.annotation.Name;
import com.almende.eve.rpc.jsonrpc.JSONRequest;
import com.almende.eve.rpc.jsonrpc.jackson.JOM;
import com.almende.util.uuid.UUID;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * The Class TestSchedulerAgent.
 */
@Access(AccessType.PUBLIC)
public class TestSchedulerAgent extends Agent {
	private static final Logger	LOG	= Logger.getLogger("testScheduler");
	
	/**
	 * Sets the test.
	 * 
	 * @param agentId
	 *            the agent id
	 * @param delay
	 *            the delay
	 * @param interval
	 *            the interval
	 * @param sequential
	 *            the sequential
	 */
	public void setTest(@Name("agentId") final String agentId,
			@Name("delay") final int delay,
			@Name("interval") final boolean interval,
			@Name("sequential") final boolean sequential) {
		final ObjectNode params = JOM.getInstance().createObjectNode();
		params.put("time", DateTime.now().toString());
		params.put("expected", DateTime.now().plus(delay).toString());
		params.put("interval", interval);
		params.put("sequential", sequential);
		params.put("someId", new UUID().toString());
		params.put("delay", delay);
		final JSONRequest request = new JSONRequest("doTest", params);
		getScheduler().createTask(request, delay, interval, sequential);
	}
	
	/**
	 * Do test.
	 * 
	 * @param startStr
	 *            the start str
	 * @param expectedStr
	 *            the expected str
	 * @param interval
	 *            the interval
	 * @param sequential
	 *            the sequential
	 * @param id
	 *            the id
	 * @param delay
	 *            the delay
	 */
	public void doTest(@Name("time") final String startStr,
			@Name("expected") final String expectedStr,
			@Name("interval") final Boolean interval,
			@Name("sequential") final Boolean sequential,
			@Name("someId") final String id, @Name("delay") final int delay) {
		final DateTime startTime = new DateTime(startStr);
		final DateTime expected = new DateTime(expectedStr);
		
		if (interval) {
			LOG.info(id + ": Duration since schedule:"
					+ (new Duration(startTime, DateTime.now()).getMillis())
					+ " sequential:" + sequential + " delay:" + delay + "ms.");
		} else {
			LOG.info(id
					+ ": Delay after expected runtime:"
					+ (new Duration(expected, DateTime.now()).getMillis()
							+ "ms of planned delay:" + delay + "ms "));
		}
		Integer oldCnt = getState().get("runCount", Integer.class);
		Integer newCnt = 1;
		if (oldCnt != null) {
			newCnt = oldCnt + 1;
		}
		while (!getState().putIfUnchanged("runCount", newCnt, oldCnt)) {
			oldCnt = getState().get("runCount", Integer.class);
			if (oldCnt != null) {
				newCnt = oldCnt + 1;
			}
		}
	}
	
	/**
	 * Gets the count.
	 * 
	 * @return the count
	 */
	public int getCount() {
		Integer count = getState().get("runCount", Integer.class);
		if (count == null) {
			count = 0;
		}
		return count;
	}
	
	/**
	 * Reset count.
	 */
	public void resetCount() {
		getState().put("runCount", 0);
	}
	
	/*
	 * (non-Javadoc)
	 * 
	 * @see com.almende.eve.agent.Agent#getDescription()
	 */
	@Override
	public String getDescription() {
		return "Scheduler test agent";
	}
	
	/*
	 * (non-Javadoc)
	 * 
	 * @see com.almende.eve.agent.Agent#getVersion()
	 */
	@Override
	public String getVersion() {
		return "0.1";
	}
	
}
