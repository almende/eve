package com.almende.test.agents;

import java.util.ArrayList;
import java.util.List;

import org.joda.time.DateTime;

import com.almende.eve.agent.Agent;
import com.almende.eve.agent.annotation.EventTriggered;
import com.almende.eve.agent.annotation.Name;
import com.almende.eve.entity.Cache;
import com.almende.eve.entity.Poll;
import com.almende.eve.entity.Push;
import com.almende.eve.rpc.jsonrpc.jackson.JOM;
import com.fasterxml.jackson.databind.node.ObjectNode;

public class TestResultMonitorAgent extends Agent {
	
	@EventTriggered("Go")
	public Integer getData() {
		return DateTime.now().getSecondOfDay();
	}
	
	public void bobEvent() throws Exception {
		trigger("Go", JOM.createObjectNode());
	}
	
	public void prepare() {
		String monitorID = initResultMonitor("local://bob", "getData",
				JOM.createObjectNode(), null, new Poll(1000), new Cache());
		
		if (monitorID != null) getState().put("pollKey", monitorID);
		
		monitorID = initResultMonitor("local://bob", "getData", JOM.createObjectNode(),
				null, new Push(1000, false), new Cache());
		if (monitorID != null) getState().put("pushKey", monitorID);
		
		monitorID = initResultMonitor("local://bob", "getData", JOM.createObjectNode(),
				null, new Push(-1, true), new Cache());
		if (monitorID != null) getState().put("LazyPushKey", monitorID);
		
		monitorID = initResultMonitor("local://bob", "getData", JOM.createObjectNode(),
				"returnRes", new Poll(800), new Poll(1500));
		
		if (monitorID != null) getState().put("LazyPollKey", monitorID);
		
	}
	
	public void returnRes(@Name("result") int result) {
		System.err.println("Received callback result:" + result);
	}
	
	public List<Integer> get_result() {
		try {
			List<Integer> result = new ArrayList<Integer>();
			ObjectNode params = JOM.createObjectNode();
			params.put("maxAge", 3000);
			String monitorID = (String) getState().get("pushKey");
			Object res = getResult(monitorID, params, Integer.class);
			result.add((Integer) res);
			
			monitorID = (String) getState().get("pollKey");
			res = getResult(monitorID, params, Integer.class);
			result.add((Integer) res);
			
			monitorID = (String) getState().get("LazyPushKey");
			res = getResult(monitorID, params, Integer.class);
			result.add((Integer) res);
			
			monitorID = (String) getState().get("LazyPollKey");
			res = getResult(monitorID, params, Integer.class);
			result.add((Integer) res);
			
			return result;
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}
	
	public void tear_down() {
		cancelResultMonitor((String) getState().get("pushKey"));
	}
	
	@Override
	public String getDescription() {
		return "test agent to work on MemoQuery development";
	}
	
	@Override
	public String getVersion() {
		return "1.0";
	}
	
}
