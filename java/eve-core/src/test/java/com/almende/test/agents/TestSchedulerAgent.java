package com.almende.test.agents;

import java.util.logging.Logger;

import org.joda.time.DateTime;
import org.joda.time.Duration;

import com.almende.eve.agent.Agent;
import com.almende.eve.agent.annotation.Name;
import com.almende.eve.rpc.jsonrpc.JSONRequest;
import com.almende.eve.rpc.jsonrpc.jackson.JOM;
import com.almende.test.TestScheduler;
import com.fasterxml.jackson.databind.node.ObjectNode;

public class TestSchedulerAgent extends Agent {
	
	public void setTest(@Name("agentId") String agentId,
			@Name("delay") int delay) {
		ObjectNode params = JOM.getInstance().createObjectNode();
		params.put("time", DateTime.now().toString());
		params.put("expected", DateTime.now().plus(delay).toString());
		JSONRequest request = new JSONRequest("doTest", params);

		getScheduler().createTask(request, delay);
	}

	public void doTest(@Name("time") String startStr,
			@Name("expected") String expectedStr) {
		DateTime startTime = new DateTime(startStr);
		DateTime expected = new DateTime(expectedStr);

		Logger log = Logger.getLogger("testScheduler");
		log.info("Duration since schedule:"
				+ (new Duration(startTime, DateTime.now()).getMillis()));
		log.info("Delay after expected runtime:"
				+ (new Duration(expected,DateTime.now()).getMillis()));
		synchronized (TestScheduler.count) {
			TestScheduler.count--;
		}
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
