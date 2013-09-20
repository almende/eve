package com.almende.eve.monitor;

import java.io.IOException;
import java.io.Serializable;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;
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
import com.almende.eve.state.TypedKey;
import com.almende.util.AnnotationUtil;
import com.almende.util.AnnotationUtil.AnnotatedClass;
import com.almende.util.AnnotationUtil.AnnotatedMethod;
import com.almende.util.NamespaceUtil;
import com.almende.util.NamespaceUtil.CallTuple;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.type.TypeFactory;

public class ResultMonitorFactory implements ResultMonitorFactoryInterface {
	private static final Logger										LOG			= Logger.getLogger(ResultMonitorFactory.class
																						.getCanonicalName());
	private Agent													myAgent		= null;
	
	private static final TypedKey<HashMap<String, ResultMonitor>>	MONITORS	= new TypedKey<HashMap<String, ResultMonitor>>(
																						"_monitors") {
																				};
	
	public ResultMonitorFactory(Agent agent) {
		this.myAgent = agent;
	}
	
	/**
	 * Sets up a monitored RPC call subscription. Conveniency method, which can
	 * also be expressed as:
	 * new ResultMonitor(monitorId, getId(),
	 * url,method,params).add(ResultMonitorConfigType
	 * config).add(ResultMonitorConfigType config).store();
	 * 
	 * @param monitorId
	 * @param url
	 * @param method
	 * @param params
	 * @param callbackMethod
	 * @param confs
	 * @return
	 */
	public String create(String monitorId, URI url, String method,
			ObjectNode params, String callbackMethod,
			ResultMonitorConfigType... confs) {
		
		ResultMonitor old = getMonitorById(monitorId);
		if (old != null) {
			old.cancel();
		}
		
		ResultMonitor monitor = new ResultMonitor(monitorId, myAgent.getId(),
				url, method, params, callbackMethod);
		for (ResultMonitorConfigType config : confs) {
			monitor.add(config);
		}
		return store(monitor);
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
			Class<T> returnType) throws IOException, JSONRPCException {
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
		ResultMonitor monitor = getMonitorById(monitorId);
		if (monitor != null) {
			if (monitor.hasCache() && monitor.getCache() != null
					&& monitor.getCache().filter(filterParms)) {
				result = (T) monitor.getCache().get();
			}
			if (result == null) {
				result = myAgent.send(monitor.getUrl(), monitor.getMethod(),
						JOM.getInstance().readTree(monitor.getParams()),
						returnType);
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
		ResultMonitor monitor = getMonitorById(monitorId);
		if (monitor != null) {
			monitor.cancel();
			delete(monitor.getId());
		} else {
			LOG.warning("Trying to cancel non existing monitor:"
					+ myAgent.getId() + "." + monitorId);
		}
	}
	
	@Access(AccessType.SELF)
	public final void doPoll(@Name("monitorId") String monitorId)
			throws JSONRPCException, IOException {
		ResultMonitor monitor = getMonitorById(monitorId);
		if (monitor != null) {
			Object result = myAgent.send(monitor.getUrl(), monitor.getMethod(),
					JOM.getInstance().readTree(monitor.getParams()),
					TypeFactory.unknownType());
			if (monitor.getCallbackMethod() != null) {
				ObjectNode params = JOM.createObjectNode();
				params.put("result",
						JOM.getInstance().writeValueAsString(result));
				myAgent.send(URI.create("local:" + myAgent.getId()),
						monitor.getCallbackMethod(), params);
			}
			if (monitor.hasCache()) {
				monitor.getCache().store(result);
			}
		}
	}
	
	private JsonNode	lastRes	= null;
	
	@Access(AccessType.SELF)
	public final void doPush(@Name("pushKey") String pushKey,
			@Required(false) @Name("params") ObjectNode triggerParams)
			throws JSONRPCException, IOException {
		
		if (myAgent.getState().containsKey(pushKey)) {
			ObjectNode pushParams = (ObjectNode) JOM.getInstance()
					.readTree(myAgent.getState().get(pushKey, String.class))
					.get("config");
			if (!(pushParams.has("method") && pushParams.has("params"))) {
				throw new JSONRPCException("Missing push configuration fields:"
						+ pushParams);
			}
			String method = pushParams.get("method").textValue();
			ObjectNode params = (ObjectNode) JOM.getInstance().readTree(
					pushParams.get("params").textValue());
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
			parms.put("pushId", pushParams.get("pushId").textValue());
			
			parms.put("callbackParams", triggerParams == null ? pushParams
					: pushParams.putAll(triggerParams));
			
			String callbackMethod = "monitor.callbackPush";
			if (pushParams.has("callback")) {
				callbackMethod = pushParams.get("callback").textValue();
			}
			myAgent.sendAsync(URI.create(pushParams.get("url").textValue()),
					callbackMethod, parms, null, Void.class);
			// TODO: If callback reports "old", unregisterPush();
		}
	}
	
	@Access(AccessType.PUBLIC)
	public final void callbackPush(@Name("result") Object result,
			@Name("pushId") String pushId,
			@Name("callbackParams") ObjectNode callbackParams)
			throws JSONRPCException {
		
		String[] ids = pushId.split("_");
		
		if (ids.length != 2) {
			throw new JSONRPCException("PushId is invalid!");
		}
		String monitorId = ids[0];
		
		try {
			
			ResultMonitor monitor = getMonitorById(monitorId);
			if (monitor != null) {
				if (monitor.getCallbackMethod() != null) {
					
					ObjectNode params = JOM.createObjectNode();
					if (callbackParams != null) {
						params = callbackParams;
					}
					params.put("result",
							JOM.getInstance().writeValueAsString(result));
					myAgent.send(URI.create("local:" + myAgent.getId()),
							monitor.getCallbackMethod(), params);
				}
				if (monitor.hasCache()) {
					monitor.getCache().store(result);
				}
			} else {
				LOG.severe("Couldn't find local monitor by id:" + monitorId);
			}
		} catch (Exception e) {
			LOG.log(Level.WARNING,
					"Couldn't run local callbackMethod for push!" + monitorId,
					e);
		}
	}
	
	@Access(AccessType.PUBLIC)
	public final void registerPush(@Name("pushId") String id,
			@Name("config") ObjectNode pushParams, @Sender String senderUrl) {
		String pushKey = "_push_" + senderUrl + "_" + id;
		pushParams.put("url", senderUrl);
		pushParams.put("pushId", id);
		
		if (myAgent.getState().containsKey(pushKey)) {
			LOG.warning("reregistration of existing push, canceling old version.");
			try {
				unregisterPush(id, senderUrl);
			} catch (Exception e) {
				LOG.warning("Failed to unregister push:" + e);
			}
		}
		ObjectNode result = JOM.createObjectNode();
		result.put("config", pushParams);
		
		ObjectNode params = JOM.createObjectNode();
		params.put("pushKey", pushKey);
		
		LOG.info("Register Push:" + pushKey);
		if (pushParams.has("interval")) {
			int interval = pushParams.get("interval").intValue();
			JSONRequest request = new JSONRequest("monitor.doPush", params);
			result.put(
					"taskId",
					myAgent.getScheduler().createTask(request, interval, true,
							false));
		}
		String event = "";
		if (pushParams.has("event")) {
			// Event param overrules
			event = pushParams.get("event").textValue();
		}
		if (pushParams.has("onChange")
				&& pushParams.get("onChange").booleanValue()) {
			AnnotatedClass ac = null;
			event = "change";
			try {
				CallTuple res = NamespaceUtil.get(myAgent,
						pushParams.get("method").textValue());
				
				ac = AnnotationUtil.get(res.getDestination().getClass());
				for (AnnotatedMethod method : ac
						.getMethods(res.getMethodName())) {
					EventTriggered annotation = method
							.getAnnotation(EventTriggered.class);
					if (annotation != null) {
						// If no Event param, get it from annotation, else
						// use default.
						event = annotation.value();
					}
				}
			} catch (Exception e) {
				LOG.log(Level.WARNING, "", e);
			}
		}
		if (!event.equals("")) {
			try {
				result.put(
						"subscriptionId",
						myAgent.getEventsFactory().subscribe(
								myAgent.getFirstUrl(), event, "monitor.doPush",
								params));
			} catch (Exception e) {
				LOG.log(Level.WARNING, "Failed to register push Event", e);
			}
		}
		
		myAgent.getState().put(pushKey, result.toString());
	}
	
	@Access(AccessType.PUBLIC)
	public final void unregisterPush(@Name("pushId") String id,
			@Sender String senderUrl) throws IOException {
		ObjectNode config = null;
		if (myAgent.getState() != null
				&& myAgent.getState().containsKey(
						"_push_" + senderUrl + "_" + id)) {
			config = (ObjectNode) JOM.getInstance().readTree(
					myAgent.getState().get("_push_" + senderUrl + "_" + id,
							String.class));
		}
		if (config == null) {
			return;
		}
		if (config.has("taskId") && myAgent.getScheduler() != null) {
			String taskId = config.get("taskId").textValue();
			myAgent.getScheduler().cancelTask(taskId);
		}
		if (config.has("subscriptionId")) {
			try {
				myAgent.getEventsFactory().unsubscribe(myAgent.getFirstUrl(),
						config.get("subscriptionId").textValue());
			} catch (Exception e) {
				LOG.severe("Failed to unsubscribe push:" + e);
			}
		}
	}
	
	public String store(ResultMonitor monitor) {
		try {
			Map<String, ResultMonitor> monitors = myAgent.getState().get(
					MONITORS);
			HashMap<String, ResultMonitor> newmonitors = new HashMap<String, ResultMonitor>();
			if (monitors != null) {
				newmonitors.putAll(monitors);
			}
			newmonitors.put(monitor.getId(), monitor);
			if (!myAgent.getState().putIfUnchanged(MONITORS.getKey(),
					newmonitors, monitors)) {
				// recursive retry.
				store(monitor);
			}
		} catch (Exception e) {
			LOG.log(Level.WARNING, "Couldn't find monitors:" + myAgent.getId()
					+ "." + monitor.getId(), e);
		}
		return monitor.getId();
	}
	
	public void delete(String monitorId) {
		
		try {
			Map<String, ResultMonitor> monitors = myAgent.getState().get(
					MONITORS);
			Map<String, ResultMonitor> newmonitors = new HashMap<String, ResultMonitor>();
			if (monitors != null) {
				newmonitors.putAll(monitors);
			}
			newmonitors.remove(monitorId);
			
			if (!myAgent.getState().putIfUnchanged(MONITORS.getKey(),
					(Serializable) newmonitors, (Serializable) monitors)) {
				// recursive retry.
				delete(monitorId);
			}
		} catch (Exception e) {
			LOG.log(Level.WARNING, "Couldn't delete monitor:" + myAgent.getId()
					+ "." + monitorId, e);
		}
	}
	
	public ResultMonitor getMonitorById(String monitorId) {
		try {
			Map<String, ResultMonitor> monitors = myAgent.getState().get(
					MONITORS);
			if (monitors == null) {
				monitors = new HashMap<String, ResultMonitor>();
			}
			ResultMonitor result = monitors.get(monitorId);
			if (result != null) {
				result.init();
			}
			return result;
		} catch (Exception e) {
			LOG.log(Level.WARNING, "Couldn't find monitor:" + myAgent.getId()
					+ "." + monitorId, e);
		}
		return null;
	}
	
	public void cancelAll() {
		for (ResultMonitor monitor : getMonitors().values()) {
			delete(monitor.getId());
		}
	}
	
	@Access(AccessType.PUBLIC)
	public Map<String, ResultMonitor> getMonitors() {
		
		try {
			Map<String, ResultMonitor> monitors = myAgent.getState().get(
					MONITORS);
			if (monitors == null) {
				monitors = new HashMap<String, ResultMonitor>();
			}
			return monitors;
		} catch (Exception e) {
			LOG.log(Level.WARNING, "Couldn't find monitors.", e);
		}
		return null;
	}
}
