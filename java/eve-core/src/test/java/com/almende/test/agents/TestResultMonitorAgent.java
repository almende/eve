package com.almende.test.agents;

import java.net.URI;
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
		System.err.println("BobEvent triggered!");
		final ObjectNode params = JOM.createObjectNode();
		params.put("hello", "world");
		getEventsFactory().trigger("Go", params);
		
	}
	
	public void prepare() {
		String monitorID = getResultMonitorFactory().create("Poll",
				URI.create("local:bob"), "getData", JOM.createObjectNode(),
				null, new Poll(1000), new Cache());
		
		if (monitorID != null) {
			getState().put("PollKey", monitorID);
		}
		
		final Cache testCache = new Cache();
		monitorID = getResultMonitorFactory().create("Push",
				URI.create("local:bob"), "getData", JOM.createObjectNode(),
				null, new Push().onInterval(1000).onChange(), testCache);
		if (monitorID != null) {
			getState().put("PushKey", monitorID);
		}
		
		monitorID = new ResultMonitor("LazyPush", getId(),
				URI.create("local:bob"), "getData", JOM.createObjectNode())
				.add(new Push(-1, true)).add(testCache).store();
		if (monitorID != null) {
			getState().put("LazyPushKey", monitorID);
		}
		
		monitorID = getResultMonitorFactory().create("LazyPoll",
				URI.create("local:bob"), "getData", JOM.createObjectNode(),
				"returnRes", new Poll(800), new Poll(1500));
		if (monitorID != null) {
			getState().put("LazyPollKey", monitorID);
		}
		
		monitorID = getResultMonitorFactory().create("EventPush",
				URI.create("local:bob"), "getData", JOM.createObjectNode(),
				"returnResParm", new Push().onEvent("Go"));
		if (monitorID != null) {
			getState().put("EventPushKey", monitorID);
		}
		
	}
	
	public void returnRes(@Name("result") final int result) {
		System.err.println("Received callback result:" + result);
	}
	
	public void returnResParm(@Name("result") final int result,
			@Name("hello") final String world) {
		System.err
				.println("Received callback result:" + result + " : " + world);
	}
	
	public List<Integer> get_result() {
		try {
			final List<Integer> result = new ArrayList<Integer>();
			final ObjectNode params = JOM.createObjectNode();
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
		} catch (final Exception e) {
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
