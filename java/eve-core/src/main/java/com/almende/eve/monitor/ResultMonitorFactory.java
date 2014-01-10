package com.almende.eve.monitor;

import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.almende.eve.agent.AgentInterface;
import com.almende.eve.agent.annotation.EventTriggered;
import com.almende.eve.rpc.annotation.Access;
import com.almende.eve.rpc.annotation.AccessType;
import com.almende.eve.rpc.annotation.Name;
import com.almende.eve.rpc.annotation.Optional;
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
	private AgentInterface											myAgent		= null;
	
	private static final TypedKey<HashMap<String, ResultMonitor>>	MONITORS	= new TypedKey<HashMap<String, ResultMonitor>>(
																						"_monitors") {
																				};
	
	public ResultMonitorFactory(final AgentInterface agent) {
		myAgent = agent;
	}
	
	@Override
	public String create(final String monitorId, final URI url, final String method,
			final ObjectNode params, final String callbackMethod,
			final ResultMonitorConfigType... confs) {
		
		final ResultMonitor old = getMonitorById(monitorId);
		if (old != null) {
			old.cancel();
		}
		
		final ResultMonitor monitor = new ResultMonitor(monitorId, myAgent.getId(),
				url, method, params, callbackMethod);
		for (final ResultMonitorConfigType config : confs) {
			monitor.add(config);
		}
		return store(monitor);
	}
	
	@Override
	public <T> T getResult(final String monitorId, final ObjectNode filterParms,
			final Class<T> returnType) throws IOException, JSONRPCException {
		return getResult(monitorId, filterParms, JOM.getTypeFactory()
				.constructSimpleType(returnType, new JavaType[0]));
	}
	
	@Override
	@SuppressWarnings("unchecked")
	public <T> T getResult(final String monitorId, final ObjectNode filterParms,
			final JavaType returnType) throws JSONRPCException, IOException {
		T result = null;
		final ResultMonitor monitor = getMonitorById(monitorId);
		if (monitor != null) {
			if (monitor.hasCache() && monitor.getCache() != null
					&& monitor.getCache().filter(filterParms)) {
				result = (T) monitor.getCache().getValue();
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
	
	@Override
	public void cancel(final String monitorId) {
		final ResultMonitor monitor = getMonitorById(monitorId);
		if (monitor != null) {
			monitor.cancel();
			delete(monitor.getId());
		} else {
			LOG.warning("Trying to cancel non existing monitor:"
					+ myAgent.getId() + "." + monitorId);
		}
	}
	
	@Access(AccessType.SELF)
	@Override
	public final void doPoll(@Name("monitorId") final String monitorId)
			throws JSONRPCException, IOException {
		final ResultMonitor monitor = getMonitorById(monitorId);
		if (monitor != null) {
			if (monitor.getUrl() == null || monitor.getMethod() == null) {
				LOG.warning("Monitor data invalid:" + monitor);
			}
			final Object result = myAgent.send(monitor.getUrl(), monitor.getMethod(),
					JOM.getInstance().readTree(monitor.getParams()),
					TypeFactory.unknownType());
			if (monitor.getCallbackMethod() != null) {
				final ObjectNode params = JOM.createObjectNode();
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
	
	// TODO: doesn't work!
	private JsonNode	lastRes	= null;
	
	@Access(AccessType.SELF)
	@Override
	public final void doPush(@Name("pushKey") final String pushKey,
			@Optional @Name("params") final ObjectNode triggerParams)
			throws JSONRPCException, IOException {
		
		if (myAgent.getState().containsKey(pushKey)) {
			final ObjectNode pushParams = (ObjectNode) JOM.getInstance()
					.readTree(myAgent.getState().get(pushKey, String.class))
					.get("config");
			if (!(pushParams.has("method") && pushParams.has("params"))) {
				throw new JSONRPCException("Missing push configuration fields:"
						+ pushParams);
			}
			final String method = pushParams.get("method").textValue();
			final ObjectNode params = (ObjectNode) JOM.getInstance().readTree(
					pushParams.get("params").textValue());
			final JSONResponse res = JSONRPC.invoke(myAgent, new JSONRequest(method,
					params), myAgent);
			
			final JsonNode result = res.getResult();
			if (pushParams.has("onChange")
					&& pushParams.get("onChange").asBoolean()) {
				if (lastRes != null && lastRes.equals(result)) {
					return;
				}
				lastRes = result;
			}
			
			final ObjectNode parms = JOM.createObjectNode();
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
	@Override
	public final void callbackPush(@Name("result") final Object result,
			@Name("pushId") final String pushId,
			@Name("callbackParams") final ObjectNode callbackParams)
			throws JSONRPCException {
		
		// TODO: THis is unclean!
		final String[] ids = pushId.split("_");
		
		if (ids.length != 2) {
			throw new JSONRPCException("PushId is invalid!");
		}
		final String monitorId = ids[0];
		
		try {
			
			final ResultMonitor monitor = getMonitorById(monitorId);
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
		} catch (final Exception e) {
			LOG.log(Level.WARNING,
					"Couldn't run local callbackMethod for push!" + monitorId,
					e);
		}
	}
	
	@Access(AccessType.PUBLIC)
	@Override
	public final void registerPush(@Name("pushId") final String id,
			@Name("config") final ObjectNode pushParams, @Sender final String senderUrl) {
		final String pushKey = "_push_" + senderUrl + "_" + id;
		pushParams.put("url", senderUrl);
		pushParams.put("pushId", id);
		
		if (myAgent.getState().containsKey(pushKey)) {
			LOG.warning("reregistration of existing push, canceling old version.");
			try {
				unregisterPush(id, senderUrl);
			} catch (final Exception e) {
				LOG.warning("Failed to unregister push:" + e);
			}
		}
		final ObjectNode result = JOM.createObjectNode();
		result.put("config", pushParams);
		
		final ObjectNode params = JOM.createObjectNode();
		params.put("pushKey", pushKey);
		
		LOG.info("Register Push:" + pushKey);
		if (pushParams.has("interval")) {
			final int interval = pushParams.get("interval").intValue();
			final JSONRequest request = new JSONRequest("monitor.doPush", params);
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
				final CallTuple res = NamespaceUtil.get(myAgent,
						pushParams.get("method").textValue());
				
				ac = AnnotationUtil.get(res.getDestination().getClass());
				for (final AnnotatedMethod method : ac
						.getMethods(res.getMethodName())) {
					final EventTriggered annotation = method
							.getAnnotation(EventTriggered.class);
					if (annotation != null) {
						// If no Event param, get it from annotation, else
						// use default.
						event = annotation.value();
					}
				}
			} catch (final Exception e) {
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
			} catch (final Exception e) {
				LOG.log(Level.WARNING, "Failed to register push Event", e);
			}
		}
		
		myAgent.getState().put(pushKey, result.toString());
	}
	
	@Access(AccessType.PUBLIC)
	@Override
	public final void unregisterPush(@Name("pushId") final String id,
			@Sender final String senderUrl) throws IOException {
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
			final String taskId = config.get("taskId").textValue();
			myAgent.getScheduler().cancelTask(taskId);
		}
		if (config.has("subscriptionId")) {
			try {
				myAgent.getEventsFactory().unsubscribe(myAgent.getFirstUrl(),
						config.get("subscriptionId").textValue());
			} catch (final Exception e) {
				LOG.severe("Failed to unsubscribe push:" + e);
			}
		}
	}
	
	@Override
	public String store(final ResultMonitor monitor) {
		try {
			final Map<String, ResultMonitor> monitors = myAgent.getState().get(
					MONITORS);
			final HashMap<String, ResultMonitor> newmonitors = new HashMap<String, ResultMonitor>();
			if (monitors != null) {
				newmonitors.putAll(monitors);
			}
			newmonitors.put(monitor.getId(), monitor);
			if (!myAgent.getState().putIfUnchanged(MONITORS.getKey(),
					newmonitors, monitors)) {
				// recursive retry.
				store(monitor);
			}
		} catch (final Exception e) {
			LOG.log(Level.WARNING, "Couldn't find monitors:" + myAgent.getId()
					+ "." + monitor.getId(), e);
		}
		return monitor.getId();
	}
	
	@Override
	public void delete(final String monitorId) {
		
		try {
			final Map<String, ResultMonitor> monitors = myAgent.getState().get(
					MONITORS);
			final Map<String, ResultMonitor> newmonitors = new HashMap<String, ResultMonitor>();
			if (monitors != null) {
				newmonitors.putAll(monitors);
			}
			newmonitors.remove(monitorId);
			
			if (!myAgent.getState().putIfUnchanged(MONITORS.getKey(),
					newmonitors, monitors)) {
				// recursive retry.
				delete(monitorId);
			}
		} catch (final Exception e) {
			LOG.log(Level.WARNING, "Couldn't delete monitor:" + myAgent.getId()
					+ "." + monitorId, e);
		}
	}
	
	@Override
	public ResultMonitor getMonitorById(final String monitorId) {
		try {
			Map<String, ResultMonitor> monitors = myAgent.getState().get(
					MONITORS);
			if (monitors == null) {
				monitors = new HashMap<String, ResultMonitor>();
			}
			final ResultMonitor result = monitors.get(monitorId);
			if (result != null) {
				result.init();
			}
			return result;
		} catch (final Exception e) {
			LOG.log(Level.WARNING, "Couldn't find monitor:" + myAgent.getId()
					+ "." + monitorId, e);
		}
		return null;
	}
	
	@Override
	public void cancelAll() {
		for (final ResultMonitor monitor : getMonitors()) {
			delete(monitor.getId());
		}
	}
	
	@Access(AccessType.PUBLIC)
	@Override
	public List<ResultMonitor> getMonitors() {
		
		try {
			Map<String, ResultMonitor> monitors = myAgent.getState().get(
					MONITORS);
			if (monitors == null) {
				monitors = new HashMap<String, ResultMonitor>();
			}
			final List<ResultMonitor> result = new ArrayList<ResultMonitor>(
					monitors.size());
			result.addAll(monitors.values());
			return result;
		} catch (final Exception e) {
			LOG.log(Level.WARNING, "Couldn't find monitors.", e);
		}
		return null;
	}
}
