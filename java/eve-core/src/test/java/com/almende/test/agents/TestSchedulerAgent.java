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

@Access(AccessType.PUBLIC)
public class TestSchedulerAgent extends Agent {
	static final Logger	log	= Logger.getLogger("testScheduler");
	
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
	
	public void doTest(@Name("time") final String startStr,
			@Name("expected") final String expectedStr,
			@Name("interval") final Boolean interval,
			@Name("sequential") final Boolean sequential,
			@Name("someId") final String id, @Name("delay") final int delay) {
		final DateTime startTime = new DateTime(startStr);
		final DateTime expected = new DateTime(expectedStr);
		
		if (interval) {
			log.info(id + ": Duration since schedule:"
					+ (new Duration(startTime, DateTime.now()).getMillis())
					+ " sequential:" + sequential + " delay:" + delay + "ms.");
		} else {
			log.info(id
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
	
	public int getCount() {
		Integer count = getState().get("runCount", Integer.class);
		if (count == null) {
			count = 0;
		}
		return count;
	}
	
	public void resetCount() {
		getState().put("runCount", 0);
	}
	
	@Override
	public String getDescription() {
		return "Scheduler test agent";
	}
	
	@Override
	public String getVersion() {
		return "0.1";
	}
	
}
