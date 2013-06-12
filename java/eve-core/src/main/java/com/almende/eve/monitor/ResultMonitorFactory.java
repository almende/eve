package com.almende.eve.monitor;

import java.io.IOException;
import java.net.ProtocolException;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.almende.eve.agent.Agent;
import com.almende.eve.agent.annotation.EventTriggered;
import com.almende.eve.rpc.annotation.Access;
import com.almende.eve.rpc.annotation.AccessType;
import com.almende.eve.rpc.annotation.Name;
import com.almende.eve.rpc.annotation.Required;
import com.almende.eve.rpc.annotation.Sender;
import com.almende.eve.rpc.jsonrpc.JSONRPC;
import com.almende.eve.rpc.jsonrpc.JSONRPCException;
import com.almende.eve.rpc.jsonrpc.JSONRequest;
import com.almende.eve.rpc.jsonrpc.JSONResponse;
import com.almende.eve.rpc.jsonrpc.jackson.JOM;
import com.almende.util.AnnotationUtil;
import com.almende.util.AnnotationUtil.AnnotatedClass;
import com.almende.util.AnnotationUtil.AnnotatedMethod;
import com.almende.util.NamespaceUtil;
import com.almende.util.NamespaceUtil.CallTuple;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.type.TypeFactory;

public class ResultMonitorFactory implements ResultMonitorInterface {
	private static final Logger LOG = Logger.getLogger(ResultMonitorFactory.class.getCanonicalName());
	private Agent	myAgent	= null;
	
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
	public String create(URI url, String method, ObjectNode params,
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
	 * @param filterParms
	 * @param returnType
	 * @return
	 * @throws JSONRPCException 
	 * @throws IOException 
	 */
	public <T> T getResult(String monitorId, ObjectNode filterParms,
			Class<T> returnType) throws IOException, JSONRPCException{
		return getResult(monitorId, filterParms, JOM.getTypeFactory()
				.constructSimpleType(returnType, new JavaType[0]));
	}
	
	/**
	 * Gets an actual return value of this monitor subscription. If a cache is
	 * available,
	 * this will return the cached value if the maxAge filter allows this.
	 * Otherwise it will run the actual RPC call (similar to "send");
	 * 
	 * @param monitorId
	 * @param filterParms
	 * @param returnType
	 * @return
	 * @throws JSONRPCException 
	 * @throws IOException 
	 */
	@SuppressWarnings("unchecked")
	public <T> T getResult(String monitorId, ObjectNode filterParms,
			JavaType returnType) throws JSONRPCException, IOException {
		T result = null;
		ResultMonitor monitor = ResultMonitor.getMonitorById(myAgent.getId(),
				monitorId);
		if (monitor != null) {
			if (monitor.hasCache() && monitor.getCache() != null
					&& monitor.getCache().filter(filterParms)) {
				result = (T) monitor.getCache().get();
			}
			if (result == null) {
				result = myAgent.send(monitor.getUrl(), monitor.getMethod(),
						monitor.getParams(), returnType);
				if (monitor.hasCache()) {
					monitor.getCache().store(result);
				}
			}
		} else {
			LOG.severe("Failed to find monitor!" + monitorId);
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
			for (String task : monitor.getSchedulerIds()) {
				myAgent.getScheduler().cancelTask(task);
			}
			for (String remote : monitor.getRemoteIds()) {
				ObjectNode params = JOM.createObjectNode();
				params.put("pushId", remote);
				try {
					myAgent.send(monitor.getUrl(), "monitor.unregisterPush", params);
				} catch (Exception e) {
					LOG.log(Level.WARNING,"Failed to unregister Push",e);
				}
			}
		}
		monitor.delete();
	}
	
	@Access(AccessType.PUBLIC)
	public final void doPoll(@Name("monitorId") String monitorId) throws JSONRPCException, IOException
			{
		ResultMonitor monitor = ResultMonitor.getMonitorById(myAgent.getId(),
				monitorId);
		if (monitor != null) {
			Object result = myAgent.send(monitor.getUrl(), monitor.getMethod(),
					monitor.getParams(), TypeFactory.unknownType());
			if (monitor.getCallbackMethod() != null) {
				ObjectNode params = JOM.createObjectNode();
				params.put("result",
						JOM.getInstance().writeValueAsString(result));
				myAgent.send(URI.create("local://" + myAgent.getId()),
						monitor.getCallbackMethod(), params);
			}
			if (monitor.hasCache()) {
				monitor.getCache().store(result);
			}
		}
	}
	
	private JsonNode	lastRes	= null;
	
	@Access(AccessType.PUBLIC)
	public final void doPush(@Name("pushParams") ObjectNode pushParams,
			@Required(false) @Name("triggerParams") ObjectNode triggerParams) throws ProtocolException, JSONRPCException
			{
		String method = pushParams.get("method").textValue();
		ObjectNode params = (ObjectNode) pushParams.get("params");
		JSONResponse res = JSONRPC.invoke(myAgent, new JSONRequest(method,
				params), myAgent);
		
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
		
		parms.put("callbackParams", triggerParams == null ? pushParams
				: pushParams.putAll(triggerParams));
		
		myAgent.send(URI.create(pushParams.get("url").textValue()),
				"monitor.callbackPush", parms);
		//TODO: If callback reports "old", unregisterPush();
	}
	
	@Access(AccessType.PUBLIC)
	public final void callbackPush(@Name("result") Object result,
			@Name("monitorId") String monitorId,
			@Name("callbackParams") ObjectNode callbackParams) {
		try {
			ResultMonitor monitor = ResultMonitor.getMonitorById(
					myAgent.getId(), monitorId);
			if (monitor != null) {
				if (monitor.getCallbackMethod() != null) {
					
					ObjectNode params = JOM.createObjectNode();
					if (callbackParams != null) {
						params = callbackParams;
					}
					params.put("result",
							JOM.getInstance().writeValueAsString(result));
					myAgent.send(URI.create("local://" + myAgent.getId()),
							monitor.getCallbackMethod(), params);
				}
				if (monitor.hasCache()) {
					monitor.getCache().store(result);
				}
			} else {
				LOG.severe("Couldn't find local monitor by id:"
						+ monitorId);
			}
		} catch (Exception e) {
			LOG.log(Level.WARNING,"Couldn't run local callbackMethod for push!"
					+ monitorId,e);
		}
	}
	
	@Access(AccessType.PUBLIC)
	public final List<String> registerPush(
			@Name("pushParams") ObjectNode pushParams, @Sender String senderUrl) {
		List<String> result = new ArrayList<String>();
		pushParams.put("url", senderUrl);
		ObjectNode wrapper = JOM.createObjectNode();
		wrapper.put("pushParams", pushParams);
		
		if (pushParams.has("interval")) {
			int interval = pushParams.get("interval").intValue();
			JSONRequest request = new JSONRequest("monitor.doPush", wrapper);
			result.add(myAgent.getScheduler().createTask(request, interval,
					true, false));
		}
		if (pushParams.has("onEvent") && pushParams.get("onEvent").asBoolean()) {
			// default
			String event = "change"; 
			if (pushParams.has("event")) {
				// Event param overrules
				event = pushParams.get("event").textValue(); 
			} else {
				AnnotatedClass ac = null;
				try {
					CallTuple res = NamespaceUtil.get(myAgent,
							pushParams.get("method").textValue());
					
					ac = AnnotationUtil.get(res.destination.getClass());
					for (AnnotatedMethod method : ac.getMethods(res.methodName)) {
						EventTriggered annotation = method
								.getAnnotation(EventTriggered.class);
						if (annotation != null) {
							// If no Event param, get it from annotation, else use default.
							event = annotation.value(); 
						}
					}
				} catch (Exception e) {
					LOG.log(Level.WARNING,"",e);
				}
			}
			
			try {
				result.add(myAgent.getEventsFactory()
						.subscribe(myAgent.getFirstUrl(), event,
								"monitor.doPush", wrapper));
			} catch (Exception e) {
				LOG.log(Level.WARNING,"Failed to register push Event",e);
			}
		}
		return result;
	}
	
	@Access(AccessType.PUBLIC)
	public final void unregisterPush(@Name("pushId") String id) {
		// Just assume that id is either a taskId or an Event subscription Id.
		// Both allow unknown ids, Postel's law rules!
		myAgent.getScheduler().cancelTask(id);
		try {
			myAgent.getEventsFactory().unsubscribe(myAgent.getFirstUrl(), id);
		} catch (Exception e) {
			LOG.severe("Failed to unsubscribe push:" + e);
		}
	}
}
