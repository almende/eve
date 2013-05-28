package com.almende.eve.monitor;

import java.util.ArrayList;
import java.util.List;

import com.almende.eve.agent.Agent;
import com.almende.eve.agent.annotation.EventTriggered;
import com.almende.eve.rpc.jsonrpc.JSONRPC;
import com.almende.eve.rpc.jsonrpc.JSONRequest;
import com.almende.eve.rpc.jsonrpc.JSONResponse;
import com.almende.eve.rpc.jsonrpc.jackson.JOM;
import com.almende.util.AnnotationUtil;
import com.almende.util.AnnotationUtil.AnnotatedClass;
import com.almende.util.AnnotationUtil.AnnotatedMethod;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

public class ResultMonitorFactory {
	
	Agent	myAgent	= null;
	
	public ResultMonitorFactory(Agent agent) {
		this.myAgent = agent;
	}
	
	/**
	 * Sets up a monitored RPC call subscription. Conveniency method, which can
	 * also be expressed as:
	 * new ResultMonitor(getId(), url,method,params).add(ResultMonitorConfigType
	 * config).add(ResultMonitorConfigType config).store();
	 * 
	 * @param url
	 * @param method
	 * @param params
	 * @param callbackMethod
	 * @param confs
	 * @return
	 */
	public String create(String url, String method, ObjectNode params,
			String callbackMethod, ResultMonitorConfigType... confs) {
		ResultMonitor monitor = new ResultMonitor(myAgent.getId(), url, method,
				params, callbackMethod);
		for (ResultMonitorConfigType config : confs) {
			monitor.add(config);
		}
		return monitor.store();
	}
	
	/**
	 * Gets an actual return value of this monitor subscription. If a cache is
	 * available,
	 * this will return the cached value if the maxAge filter allows this.
	 * Otherwise it will run the actual RPC call (similar to "send");
	 * 
	 * @param monitorId
	 * @param filter_parms
	 * @param returnType
	 * @return
	 * @throws Exception
	 */
	@SuppressWarnings("unchecked")
	public <T> T getResult(String monitorId, ObjectNode filter_parms,
			Class<T> returnType) throws Exception {
		T result = null;
		ResultMonitor monitor = ResultMonitor.getMonitorById(myAgent.getId(),
				monitorId);
		if (monitor != null) {
			if (monitor.hasCache()) {
				if (monitor.getCache() != null
						&& monitor.getCache().filter(filter_parms)) {
					result = (T) monitor.getCache().get();
				}
			}
			if (result == null) {
				result = myAgent.send(monitor.url, monitor.method,
						monitor.params, returnType);
				if (monitor.hasCache()) {
					monitor.getCache().store(result);
				}
			}
		} else {
			System.err.println("Failed to find monitor!" + monitorId);
		}
		return result;
		
	}
	
	/**
	 * Cancels a running monitor subscription.
	 * 
	 * @param monitorId
	 */
	public void cancel(String monitorId) {
		ResultMonitor monitor = ResultMonitor.getMonitorById(myAgent.getId(),
				monitorId);
		// TODO: Let the cancelation be managed by the original objects
		// (Pushes/Polls/Caches, etc.)
		if (monitor != null) {
			for (String task : monitor.schedulerIds) {
				myAgent.getScheduler().cancelTask(task);
			}
			for (String remote : monitor.remoteIds) {
				ObjectNode params = JOM.createObjectNode();
				params.put("pushId", remote);
				try {
					myAgent.send(monitor.url, "unregisterPush", params);
				} catch (Exception e) {
					System.err.println("Failed to unregister Push");
					e.printStackTrace();
				}
			}
		}
		monitor.delete();
	}
	
	public void doPoll(String monitorId) throws Exception {
		ResultMonitor monitor = ResultMonitor.getMonitorById(myAgent.getId(),
				monitorId);
		if (monitor != null) {
			Object result = myAgent.send(monitor.url, monitor.method,
					monitor.params, Object.class);
			if (monitor.callbackMethod != null) {
				ObjectNode params = JOM.createObjectNode();
				params.put("result",
						JOM.getInstance().writeValueAsString(result));
				myAgent.send("local://" + myAgent.getId(),
						monitor.callbackMethod, params);
			}
			if (monitor.hasCache()) {
				monitor.getCache().store(result);
			}
		}
	}
	
	private JsonNode	lastRes	= null;
	
	public void doPush(ObjectNode pushParams) throws Exception {
		String method = pushParams.get("method").textValue();
		ObjectNode params = (ObjectNode) pushParams.get("params");
		JSONResponse res = JSONRPC.invoke(myAgent, new JSONRequest(method,
				params),myAgent);
		
		JsonNode result = res.getResult();
		if (pushParams.has("onChange")
				&& pushParams.get("onChange").asBoolean()) {
			if (lastRes != null && lastRes.equals(result)) {
				return;
			}
			lastRes = result;
		}
		
		ObjectNode parms = JOM.createObjectNode();
		parms.put("result", result);
		parms.put("monitorId", pushParams.get("monitorId").textValue());
		
		myAgent.send(pushParams.get("url").textValue(), "callbackPush", parms);
		// If callback reports "old", unregisterPush();
	}
	
	public void callbackPush(Object result, String monitorId) {
		try {
			ResultMonitor monitor = ResultMonitor.getMonitorById(
					myAgent.getId(), monitorId);
			if (monitor != null) {
				if (monitor.callbackMethod != null) {
					ObjectNode params = JOM.createObjectNode();
					params.put("result",
							JOM.getInstance().writeValueAsString(result));
					JSONRPC.invoke(myAgent, new JSONRequest(
							monitor.callbackMethod, params),myAgent);
				}
				if (monitor.hasCache()) {
					monitor.getCache().store(result);
				}
			}
		} catch (Exception e) {
			System.err.println("Couldn't run local callbackMethod for push!"
					+ monitorId);
			e.printStackTrace();
		}
	}
	
	public List<String> registerPush(ObjectNode pushParams, String senderUrl) {
		List<String> result = new ArrayList<String>();
		pushParams.put("url", senderUrl);
		ObjectNode parms = JOM.createObjectNode();
		parms.put("params", pushParams);
		
		if (pushParams.has("interval")) {
			int interval = pushParams.get("interval").intValue();
			JSONRequest request = new JSONRequest("doPush", parms);
			result.add(myAgent.getScheduler().createTask(request, interval,
					true, false));
		}
		if (pushParams.has("onEvent") && pushParams.get("onEvent").asBoolean()) {
			String event = "change"; // default
			if (pushParams.has("event")) {
				event = pushParams.get("event").textValue(); // Event param
																// overrules
			} else {
				AnnotatedClass ac = null;
				try {
					ac = AnnotationUtil.get(getClass());
					for (AnnotatedMethod method : ac.getMethods(pushParams.get(
							"method").textValue())) {
						EventTriggered annotation = method
								.getAnnotation(EventTriggered.class);
						if (annotation != null) {
							event = annotation.value(); // If no Event param,
														// get it from
														// annotation, else
														// default.
						}
					}
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
			try {
				result.add(myAgent.getEventsFactory().subscribe(
						myAgent.getFirstUrl(), event, "doPush", parms));
			} catch (Exception e) {
				System.err.println("Failed to register push Event");
				e.printStackTrace();
			}
		}
		return result;
	}
	
	public void unregisterPush(String id) {
		// Just assume that id is either a taskId or an Event subscription Id.
		// Both allow unknown ids, Postel's law rules!
		myAgent.getScheduler().cancelTask(id);
		try {
			myAgent.getEventsFactory().unsubscribe(myAgent.getFirstUrl(), id);
		} catch (Exception e) {
			System.err.println("Failed to unsubscribe push:" + e);
		}
	}
}
