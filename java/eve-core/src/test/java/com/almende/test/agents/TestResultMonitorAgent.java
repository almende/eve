package com.almende.test.agents;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;

import org.joda.time.DateTime;

import com.almende.eve.agent.Agent;
import com.almende.eve.agent.annotation.EventTriggered;
import com.almende.eve.monitor.Cache;
import com.almende.eve.monitor.impl.DefaultCache;
import com.almende.eve.monitor.impl.DefaultPoll;
import com.almende.eve.monitor.impl.DefaultPush;
import com.almende.eve.monitor.impl.ResultMonitorImpl;
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
		System.err.println("BobEvent triggered!");
		ObjectNode params = JOM.createObjectNode();
		params.put("hello", "world");
		getEventsFactory().trigger("Go", params);
		
	}
	
	public void prepare() {
		String monitorID = getResultMonitorFactory().create("Poll",
				URI.create("local:bob"), "getData", JOM.createObjectNode(),
				null, new DefaultPoll(1000), new DefaultCache());
		
		if (monitorID != null) getState().put("PollKey", monitorID);
		
		Cache testCache = new DefaultCache();
		monitorID = getResultMonitorFactory().create("Push",
				URI.create("local:bob"), "getData", JOM.createObjectNode(),
				null, new DefaultPush().onInterval(1000).onChange(), testCache);
		if (monitorID != null) getState().put("PushKey", monitorID);
		
		monitorID = new ResultMonitorImpl("LazyPush", getId(),
				URI.create("local:bob"), "getData", JOM.createObjectNode())
				.add(new DefaultPush(-1, true)).add(testCache).store();
		if (monitorID != null) getState().put("LazyPushKey", monitorID);
		
		monitorID = getResultMonitorFactory().create("LazyPoll",
				URI.create("local:bob"), "getData", JOM.createObjectNode(),
				"returnRes", new DefaultPoll(800), new DefaultPoll(1500));
		if (monitorID != null) getState().put("LazyPollKey", monitorID);
		
		monitorID = getResultMonitorFactory().create("EventPush",
				URI.create("local:bob"), "getData", JOM.createObjectNode(),
				"returnResParm", new DefaultPush().onEvent("Go"));
		if (monitorID != null) getState().put("EventPushKey", monitorID);
		
	}
	
	public void returnRes(@Name("result") int result) {
		System.err.println("Received callback result:" + result);
	}
	
	public void returnResParm(@Name("result") int result,
			@Name("hello") String world) {
		System.err
				.println("Received callback result:" + result + " : " + world);
	}
	
	public List<Integer> get_result() {
		try {
			List<Integer> result = new ArrayList<Integer>();
			ObjectNode params = JOM.createObjectNode();
			params.put("maxAge", 3000);
			String monitorID = getState().get("PushKey", String.class);
			result.add(getResultMonitorFactory().getResult(monitorID, params,
					Integer.class));
			
			monitorID = getState().get("PollKey", String.class);
			result.add(getResultMonitorFactory().getResult(monitorID, params,
					Integer.class));
			
			monitorID = getState().get("LazyPushKey", String.class);
			result.add(getResultMonitorFactory().getResult(monitorID, params,
					Integer.class));
			
			monitorID = getState().get("LazyPollKey", String.class);
			result.add(getResultMonitorFactory().getResult(monitorID, params,
					Integer.class));
			
			return result;
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}
	
	public void tear_down() {
		getResultMonitorFactory().cancel(
				getState().get("PushKey", String.class));
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
