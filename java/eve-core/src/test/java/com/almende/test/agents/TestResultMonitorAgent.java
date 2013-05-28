package com.almende.test.agents;

import java.util.ArrayList;
import java.util.List;

import org.joda.time.DateTime;

import com.almende.eve.agent.Agent;
import com.almende.eve.agent.annotation.EventTriggered;
import com.almende.eve.monitor.Cache;
import com.almende.eve.monitor.Poll;
import com.almende.eve.monitor.Push;
import com.almende.eve.monitor.ResultMonitor;
import com.almende.eve.rpc.annotation.Access;
import com.almende.eve.rpc.annotation.AccessType;
import com.almende.eve.rpc.annotation.Name;
import com.almende.eve.rpc.jsonrpc.jackson.JOM;
import com.fasterxml.jackson.databind.node.ObjectNode;

@Access(AccessType.PUBLIC)
public class TestResultMonitorAgent extends Agent {
	
	@EventTriggered("Go")
	public Integer getData() {
		return DateTime.now().getSecondOfDay();
	}
	
	public void bobEvent() throws Exception {
		eventsFactory.trigger("Go", JOM.createObjectNode());
	}
	
	public void prepare() {
		String monitorID = getResultMonitorFactory().create("local://bob", "getData",
				JOM.createObjectNode(), null, new Poll(1000), new Cache());
		
		if (monitorID != null) getState().put("pollKey", monitorID);
		
		Cache testCache = new Cache();
		monitorID = getResultMonitorFactory().create("local://bob", "getData", JOM.createObjectNode(),
				null, new Push().onInterval(1000).onChange(), testCache);
		if (monitorID != null) getState().put("pushKey", monitorID);
		
		monitorID = new ResultMonitor(getId(),"local://bob", "getData", JOM.createObjectNode()).add(new Push(-1, true)).add(testCache).store();
		if (monitorID != null) getState().put("LazyPushKey", monitorID);
		
		monitorID = getResultMonitorFactory().create("local://bob", "getData", JOM.createObjectNode(),
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
			result.add(getResultMonitorFactory().getResult(monitorID, params, Integer.class));
			
			monitorID = (String) getState().get("pollKey");
			result.add(getResultMonitorFactory().getResult(monitorID, params, Integer.class));
			
			monitorID = (String) getState().get("LazyPushKey");
			result.add(getResultMonitorFactory().getResult(monitorID, params, Integer.class));
			
			monitorID = (String) getState().get("LazyPollKey");
			result.add(getResultMonitorFactory().getResult(monitorID, params, Integer.class));
			
			return result;
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}
	
	public void tear_down() {
		getResultMonitorFactory().cancel((String) getState().get("pushKey"));
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
