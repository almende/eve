package com.almende.test.agents;

import java.io.Serializable;
import java.util.logging.Logger;

import org.joda.time.DateTime;
import org.joda.time.Duration;

import com.almende.eve.agent.Agent;
import com.almende.eve.rpc.annotation.Access;
import com.almende.eve.rpc.annotation.AccessType;
import com.almende.eve.rpc.annotation.Name;
import com.almende.eve.rpc.jsonrpc.JSONRequest;
import com.almende.eve.rpc.jsonrpc.jackson.JOM;
import com.fasterxml.jackson.databind.node.ObjectNode;

@Access(AccessType.PUBLIC)
public class TestSchedulerAgent extends Agent {
	static final Logger log = Logger.getLogger("testScheduler");
	
	public void setTest(@Name("agentId") String agentId,
			@Name("delay") int delay, @Name("interval") boolean interval,@Name("sequential") boolean sequential) {
		ObjectNode params = JOM.getInstance().createObjectNode();
		params.put("time", DateTime.now().toString());
		params.put("expected", DateTime.now().plus(delay).toString());
		params.put("interval", interval);
		params.put("sequential", sequential);
		JSONRequest request = new JSONRequest("doTest", params);
		getScheduler().createTask(request, delay, interval, sequential);
	}

	public void doTest(@Name("time") String startStr,
			@Name("expected") String expectedStr,
			@Name("interval") Boolean interval,
			@Name("sequential") Boolean sequential) {
		DateTime startTime = new DateTime(startStr);
		DateTime expected = new DateTime(expectedStr);

		log.info("Duration since schedule:"
				+ (new Duration(startTime, DateTime.now()).getMillis()));
		if (interval){
			log.info("Interval seq:"+sequential);
		} else {
			log.info("Delay after expected runtime:"
				+ (new Duration(expected,DateTime.now()).getMillis()));
		}
		Serializable oldCnt = getState().get("runCount");
		Integer newCnt = 1;
		if (oldCnt != null){
			newCnt = ((Integer)oldCnt)+1;
		}
		while (!getState().putIfUnchanged("runCount", newCnt, oldCnt)){
			oldCnt = (Integer) getState().get("runCount");
			if (oldCnt != null){
				newCnt = ((Integer)oldCnt)+1;
			}
		}
	}
	public int getCount(){
		Integer count =(Integer) getState().get("runCount");
		if (count == null) count = 0;
		return count;
	}
	public void resetCount(){
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
