/**
 * @file Agent.java
 * 
 * @brief
 *        Agent is the abstract base class for all Eve agents.
 *        It provides basic functionality such as id, url, getting methods,
 *        subscribing to events, etc.
 * 
 * @license
 *          Licensed under the Apache License, Version 2.0 (the "License"); you
 *          may not
 *          use this file except in compliance with the License. You may obtain
 *          a copy
 *          of the License at
 * 
 *          http://www.apache.org/licenses/LICENSE-2.0
 * 
 *          Unless required by applicable law or agreed to in writing, software
 *          distributed under the License is distributed on an "AS IS" BASIS,
 *          WITHOUT
 *          WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See
 *          the
 *          License for the specific language governing permissions and
 *          limitations under
 *          the License.
 * 
 *          Copyright © 2010-2012 Almende B.V.
 * 
 * @author Jos de Jong, <jos@almende.org>
 * @date 2012-12-12
 */

package com.almende.eve.agent;

import java.io.IOException;
import java.lang.reflect.Type;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.almende.eve.agent.annotation.Namespace;
import com.almende.eve.agent.callback.AsyncCallback;
import com.almende.eve.agent.callback.CallbackInterface;
import com.almende.eve.agent.callback.SyncCallback;
import com.almende.eve.agent.proxy.AsyncProxy;
import com.almende.eve.event.EventsFactory;
import com.almende.eve.event.EventsInterface;
import com.almende.eve.monitor.ResultMonitorFactory;
import com.almende.eve.monitor.ResultMonitorFactoryInterface;
import com.almende.eve.rpc.RequestParams;
import com.almende.eve.rpc.annotation.Access;
import com.almende.eve.rpc.annotation.AccessType;
import com.almende.eve.rpc.annotation.Sender;
import com.almende.eve.rpc.jsonrpc.JSONMessage;
import com.almende.eve.rpc.jsonrpc.JSONRPC;
import com.almende.eve.rpc.jsonrpc.JSONRPCException;
import com.almende.eve.rpc.jsonrpc.JSONRPCException.CODE;
import com.almende.eve.rpc.jsonrpc.JSONRequest;
import com.almende.eve.rpc.jsonrpc.JSONResponse;
import com.almende.eve.rpc.jsonrpc.jackson.JOM;
import com.almende.eve.scheduler.Scheduler;
import com.almende.eve.state.State;
import com.almende.eve.state.TypedKey;
import com.almende.eve.transport.TransportService;
import com.almende.util.TypeUtil;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

@Access(AccessType.UNAVAILABLE)
public abstract class Agent implements AgentInterface {
	private static final Logger				LOG				= Logger.getLogger(Agent.class
																	.getCanonicalName());
	private AgentHost						agentHost		= null;
	private State							state			= null;
	private Scheduler						scheduler		= null;
	private ResultMonitorFactoryInterface	monitorFactory	= null;
	private EventsInterface					eventsFactory	= null;
	private CallbackInterface<JSONResponse>	callbacks		= null;
	
	@Access(AccessType.PUBLIC)
	public String getDescription() {
		return "Base agent.";
	}
	
	@Access(AccessType.PUBLIC)
	public String getVersion() {
		return "1.0";
	}
	
	public Agent() {
	}
	
	public void constr(AgentHost agentHost, State state) {
		if (this.state == null) {
			this.agentHost = agentHost;
			this.state = state;
			this.monitorFactory = new ResultMonitorFactory(this);
			this.eventsFactory = new EventsFactory(this);
			this.callbacks = agentHost.getCallbackService(getId(),
					JSONResponse.class);
		}
	}
	
	@Override
	@Access(AccessType.UNAVAILABLE)
	public boolean onAccess(String senderUrl, String functionTag) {
		return true;
	}
	
	@Override
	@Access(AccessType.UNAVAILABLE)
	public boolean onAccess(String senderUrl) {
		return onAccess(senderUrl, null);
	}
	
	@Override
	@Access(AccessType.UNAVAILABLE)
	public boolean isSelf(String senderUrl) {
		if (senderUrl.startsWith("web://")) {
			return true;
		}
		List<String> urls = getUrls();
		return urls.contains(senderUrl);
	}
	
	@Override
	@Access(AccessType.UNAVAILABLE)
	public void signalAgent(AgentSignal<?> event) throws JSONRPCException,
			IOException {
		if (AgentSignal.INVOKE.equals(event.getEvent())) {
			sigInvoke((Object[]) event.getData());
		} else if (AgentSignal.RESPOND.equals(event.getEvent())) {
			sigRespond((JSONResponse) event.getData());
		} else if (AgentSignal.CREATE.equals(event.getEvent())) {
			sigCreate();
		} else if (AgentSignal.INIT.equals(event.getEvent())) {
			sigInit();
		} else if (AgentSignal.DELETE.equals(event.getEvent())) {
			sigDelete();
		} else if (AgentSignal.DESTROY.equals(event.getEvent())) {
			sigDestroy();
		} else if (AgentSignal.SETSCHEDULERFACTORY.equals(event.getEvent())) {
			// init scheduler tasks
			this.scheduler = agentHost.getScheduler(this);
		} else if (AgentSignal.ADDTRANSPORTSERVICE.equals(event.getEvent())) {
			TransportService service = (TransportService) event.getData();
			service.reconnect(getId());
		}
	}
	
	/**
	 * This method is called once in the life time of an agent, at the moment
	 * the agent is being created by the AgentHost.
	 * It can be overridden and used to perform some action when the agent
	 * is create, in that case super.sigCreate() should be called in
	 * the overridden sigCreate().
	 */
	@Access(AccessType.UNAVAILABLE)
	protected void sigCreate() {
		for (TransportService service : agentHost.getTransportServices()) {
			try {
				service.reconnect(getId());
			} catch (Exception e) {
				LOG.log(Level.WARNING, "Couldn't reconnect transport:"
						+ service + " for agent:" + getId(), e);
			}
		}
	}
	
	/**
	 * This method is called on each incoming RPC call.
	 * It can be overridden and used to perform some action when the agent
	 * is invoked.
	 * 
	 * @param request
	 */
	@Access(AccessType.UNAVAILABLE)
	protected void sigInvoke(Object[] signalData) {
	}
	
	/**
	 * This method is called after handling each incoming RPC call.
	 * It can be overridden and used to perform some action when the agent
	 * has been invoked.
	 */
	@Access(AccessType.UNAVAILABLE)
	protected void sigRespond(JSONResponse response) {
	}
	
	/**
	 * This method is called directly after the agent and its state is
	 * initiated.
	 * It can be overridden and used to perform some action when the agent
	 * is initialized, in that case super.sigInit() should be called in
	 * the overridden sigInit().
	 */
	@Access(AccessType.UNAVAILABLE)
	protected void sigInit() {
	}
	
	/**
	 * This method is called by the finalize method (GC) upon unloading of the
	 * agent from memory.
	 */
	@Access(AccessType.UNAVAILABLE)
	protected void sigDestroy() {
	}
	
	/**
	 * This method is called once in the life time of an agent, at the moment
	 * the agent is being deleted by the AgentHost.
	 * It can be overridden and used to perform some action when the agent
	 * is deleted, in that case super.sigDelete() should be called in
	 * the overridden sigDelete().
	 */
	@Access(AccessType.UNAVAILABLE)
	protected void sigDelete() {
		// TODO: unsubscribe from all subscriptions
		
		// cancel all scheduled tasks.
		if (scheduler == null) {
			this.scheduler = agentHost.getScheduler(this);
		}
		if (scheduler != null) {
			for (String taskId : scheduler.getTasks()) {
				scheduler.cancelTask(taskId);
			}
		}
		// remove all keys from the state
		// Note: the state itself will be deleted by the AgentHost
		state.clear();
		
		// save the agents class again in the state
		state.put(State.KEY_AGENT_TYPE, getClass().getName());
		state = null;
		// forget local reference, as it can keep the State alive
		// even if the AgentHost removes the file.
	}
	
	@Override
	@Access(AccessType.UNAVAILABLE)
	protected void finalize() throws Throwable {
		// ensure the state is cleanup when the agent's method destroy is not
		// called.
		sigDestroy();
		getState().destroy();
		super.finalize();
	}
	
	@Override
	@Access(AccessType.UNAVAILABLE)
	public final State getState() {
		return state;
	}
	
	@Override
	@Namespace("scheduler")
	public final Scheduler getScheduler() {
		if (scheduler == null) {
			this.scheduler = agentHost.getScheduler(this);
		}
		return scheduler;
	}
	
	@Override
	@Access(AccessType.UNAVAILABLE)
	public final AgentHost getAgentHost() {
		return agentHost;
		
	}
	
	@Override
	@Deprecated
	@Access(AccessType.UNAVAILABLE)
	public final AgentHost getAgentFactory() {
		return getAgentHost();
		
	}
	
	@Override
	@Namespace("monitor")
	public final ResultMonitorFactoryInterface getResultMonitorFactory() {
		return monitorFactory;
	}
	
	@Override
	@Namespace("event")
	public final EventsInterface getEventsFactory() {
		return eventsFactory;
	}
	
	@Override
	@Access(AccessType.PUBLIC)
	public URI getFirstUrl() {
		List<String> urls = getUrls();
		if (urls.size() > 0) {
			return URI.create(urls.get(0));
		}
		return URI.create("local:" + getId());
	}
	
	@Override
	@Access(AccessType.PUBLIC)
	public List<Object> getMethods() {
		return getAgentHost().getMethods(this);
	}
	
	// TODO: only allow ObjectNode as params?
	private JSONResponse locSend(URI url, String method, Object params)
			throws IOException, JSONRPCException {
		// TODO: implement support for adding custom http headers (for
		// authorization for example)
		
		ObjectNode jsonParams;
		if (params instanceof ObjectNode) {
			jsonParams = (ObjectNode) params;
		} else {
			jsonParams = JOM.getInstance().valueToTree(params);
		}
		
		// invoke the other agent via the AgentHost, allowing the factory
		// to route the request internally or externally
		JSONRequest request = new JSONRequest(method, jsonParams);
		SyncCallback<JSONResponse> callback = new SyncCallback<JSONResponse>();
		send(request, url, callback);
		JSONResponse response;
		try {
			response = callback.get();
		} catch (Exception e) {
			throw new JSONRPCException(CODE.REMOTE_EXCEPTION, "", e);
		}
		JSONRPCException err = response.getError();
		if (err != null) {
			throw err;
		}
		return response;
	}
	
	@Override
	@Access(AccessType.UNAVAILABLE)
	public final <T> T send(URI url, String method, Object params, Class<T> type)
			throws IOException, JSONRPCException {
		return TypeUtil.inject(locSend(url, method, params).getResult(), type);
	}
	
	@Override
	@Access(AccessType.UNAVAILABLE)
	public final <T> T send(URI url, String method, Object params, Type type)
			throws IOException, JSONRPCException {
		return TypeUtil.inject(locSend(url, method, params).getResult(), type);
	}
	
	@Override
	@Access(AccessType.UNAVAILABLE)
	public final <T> T send(URI url, String method, Object params,
			TypeUtil<T> type) throws IOException, JSONRPCException {
		return type.inject(locSend(url, method, params).getResult());
	}
	
	@Override
	@Access(AccessType.UNAVAILABLE)
	public final <T> T send(URI url, String method, Object params, JavaType type)
			throws IOException, JSONRPCException {
		return TypeUtil.inject(locSend(url, method, params).getResult(), type);
	}
	
	@Override
	@Access(AccessType.UNAVAILABLE)
	public final <T> T send(URI url, String method, Type type)
			throws IOException, JSONRPCException {
		return TypeUtil.inject(locSend(url, method, null).getResult(), type);
	}
	
	@Override
	@Access(AccessType.UNAVAILABLE)
	public final <T> T send(URI url, String method, JavaType type)
			throws IOException, JSONRPCException {
		return TypeUtil.inject(locSend(url, method, null).getResult(), type);
	}
	
	@Override
	@Access(AccessType.UNAVAILABLE)
	public final <T> T send(URI url, String method, Class<T> type)
			throws IOException, JSONRPCException {
		
		return TypeUtil.inject(locSend(url, method, null).getResult(), type);
	}
	
	@Override
	@Access(AccessType.UNAVAILABLE)
	public final <T> T send(URI url, String method, TypeUtil<T> type)
			throws IOException, JSONRPCException {
		
		return type.inject(locSend(url, method, null).getResult());
	}
	
	@Override
	@Access(AccessType.UNAVAILABLE)
	public final void send(URI url, String method, Object params)
			throws IOException, JSONRPCException {
		locSend(url, method, params);
	}
	
	@Override
	@Access(AccessType.UNAVAILABLE)
	public final void send(URI url, String method) throws IOException,
			JSONRPCException {
		locSend(url, method, null);
	}
	
	@Override
	@Access(AccessType.UNAVAILABLE)
	public final <T extends AgentInterface> T createAgentProxy(URI url,
			Class<T> agentInterface) {
		return getAgentHost().createAgentProxy(this, url, agentInterface);
	}
	
	@Override
	@Access(AccessType.UNAVAILABLE)
	public final <T extends AgentInterface> AsyncProxy<T> createAsyncAgentProxy(
			URI url, Class<T> agentInterface) {
		return getAgentHost().createAsyncAgentProxy(this, url, agentInterface);
	}
	
	@Override
	@Access(AccessType.UNAVAILABLE)
	public final void sendAsync(final URI url, final String method,
			final ObjectNode params) throws IOException {
		sendAsync(url, method, params, null, Void.class);
	}
	
	@Override
	@Access(AccessType.UNAVAILABLE)
	public final void sendAsync(final URI url, final String method)
			throws IOException {
		sendAsync(url, method, null, null, Void.class);
	}
	
	@Override
	@Access(AccessType.UNAVAILABLE)
	public final <T> void sendAsync(final URI url, final String method,
			final ObjectNode params, final AsyncCallback<T> callback,
			final Class<T> type) throws IOException {
		JSONRequest request = new JSONRequest(method, params);
		sendAsync(url, request, callback, JOM.getTypeFactory()
				.uncheckedSimpleType(type));
	}
	
	@Override
	@Access(AccessType.UNAVAILABLE)
	public final <T> void sendAsync(final URI url, final String method,
			final ObjectNode params, final AsyncCallback<T> callback,
			final Type type) throws IOException {
		JSONRequest request = new JSONRequest(method, params);
		sendAsync(url, request, callback,
				JOM.getTypeFactory().constructType(type));
	}
	
	@Override
	@Access(AccessType.UNAVAILABLE)
	public final <T> void sendAsync(final URI url, final String method,
			final ObjectNode params, final AsyncCallback<T> callback,
			final JavaType type) throws IOException {
		JSONRequest request = new JSONRequest(method, params);
		sendAsync(url, request, callback, type);
	}
	
	@Override
	@Access(AccessType.UNAVAILABLE)
	public final <T> void sendAsync(final URI url, final JSONRequest request,
			final AsyncCallback<T> callback, final Class<T> type)
			throws IOException {
		sendAsync(url, request, callback, JOM.getTypeFactory()
				.uncheckedSimpleType(type));
	}
	
	@Override
	@Access(AccessType.UNAVAILABLE)
	public final <T> void sendAsync(final URI url, final JSONRequest request,
			final AsyncCallback<T> callback, final Type type)
			throws IOException {
		sendAsync(url, request, callback,
				JOM.getTypeFactory().constructType(type));
	}
	
	@Override
	@Access(AccessType.UNAVAILABLE)
	public final <T> void sendAsync(final URI url, final JSONRequest request,
			final AsyncCallback<T> callback, final JavaType type)
			throws IOException {
		
		// Create a callback to retrieve a JSONResponse and extract the result
		// or error from this. This is double nested, mostly because of the type
		// conversions required on the result.
		final AsyncCallback<JSONResponse> responseCallback = new AsyncCallback<JSONResponse>() {
			@SuppressWarnings("unchecked")
			@Override
			public void onSuccess(JSONResponse response) {
				if (callback == null) {
					Exception err = response.getError();
					if (err != null) {
						LOG.warning("async RPC call failed, and no callback handler available:"
								+ err.getLocalizedMessage());
					}
				} else {
					Exception err = response.getError();
					if (err != null) {
						callback.onFailure(err);
					}
					if (type != null && !type.hasRawClass(Void.class)) {
						try {
							T res = (T) TypeUtil.inject(response.getResult(),
									type);
							callback.onSuccess(res);
						} catch (ClassCastException cce) {
							callback.onFailure(new JSONRPCException(
									"Incorrect return type received for JSON-RPC call:"
											+ request.getMethod() + "@" + url,
									cce));
						}
						
					} else {
						callback.onSuccess(null);
					}
				}
			}
			
			@Override
			public void onFailure(Exception exception) {
				if (callback == null) {
					LOG.warning("async RPC call failed and no callback handler available:"
							+ exception.getLocalizedMessage());
				} else {
					callback.onFailure(exception);
				}
			}
		};
		
		send(request, url, responseCallback);
	}
	
	@Override
	@Access(AccessType.PUBLIC)
	public List<String> getUrls() {
		List<String> urls = new ArrayList<String>();
		if (agentHost != null) {
			String agentId = getId();
			for (TransportService service : agentHost.getTransportServices()) {
				String url = service.getAgentUrl(agentId);
				if (url != null) {
					urls.add(url);
				}
			}
			urls.add("local:" + agentId);
		} else {
			LOG.severe("AgentHost not initialized?!?");
		}
		return urls;
	}
	
	@Override
	public <T> T getRef(TypedKey<T> key) {
		return agentHost.getRef(getId(), key);
	}
	
	@Override
	public <T> void putRef(TypedKey<T> key, T value) {
		agentHost.putRef(getId(), key, value);
	}
	
	@Override
	@Access(AccessType.PUBLIC)
	public String getId() {
		return state.getAgentId();
	}
	
	@Override
	@Access(AccessType.PUBLIC)
	public String getType() {
		return getClass().getSimpleName();
	}
	
	@Override
	@Access(AccessType.PUBLIC)
	public String toString() {
		Map<String, Object> data = new HashMap<String, Object>();
		data.put("class", this.getClass().getName());
		data.put("id", getId());
		return data.toString();
	}
	
	// TODO: This should be abstracted to a generic "Translation service"?
	@Override
	public void receive(Object msg, URI senderUrl) {
		receive(msg, senderUrl, null);
	}
	
	private JSONMessage jsonConvert(final Object msg) throws IOException,
			JSONRPCException {
		JSONMessage jsonMsg = null;
		if (msg instanceof JSONMessage) {
			jsonMsg = (JSONMessage) msg;
		} else {
			ObjectNode json = null;
			if (msg instanceof String) {
				String message = (String) msg;
				if (message.startsWith("{") || message.trim().startsWith("{")) {
					
					json = JOM.getInstance().readValue(message,
							ObjectNode.class);
				}
			} else if (msg instanceof ObjectNode) {
				json = (ObjectNode) msg;
			} else if (msg == null) {
				LOG.warning("Message null!");
			} else {
				LOG.warning("Message unknown type:" + msg.getClass());
			}
			if (json != null) {
				if (JSONRPC.isResponse(json)) {
					JSONResponse response = new JSONResponse(json);
					jsonMsg = response;
				} else if (JSONRPC.isRequest(json)) {
					JSONRequest request = new JSONRequest(json);
					jsonMsg = request;
				} else {
					throw new IllegalArgumentException(
							getId()
									+ ": Request does not contain a valid JSON-RPC request or response");
				}
			}
		}
		return jsonMsg;
	}
	
	@Override
	public void receive(final Object msg, final URI senderUrl, final String tag) {
		JsonNode id = null;
		try {
			JSONMessage jsonMsg = jsonConvert(msg);
			if (jsonMsg != null) {
				if (jsonMsg.getId() != null) {
					id = jsonMsg.getId();
				}
				if (jsonMsg instanceof JSONRequest) {
					final RequestParams params = new RequestParams();
					params.put(Sender.class, senderUrl.toASCIIString());
					
					final JSONRequest request = (JSONRequest) jsonMsg;
					final AgentInterface me = this;
					AgentHost.getPool().execute(new Runnable() {
						@Override
						public void run() {
							JSONResponse response = JSONRPC.invoke(me, request,
									params, me);
							try {
								send(response, senderUrl, null, tag);
							} catch (IOException e) {
								LOG.log(Level.WARNING, getId()
										+ ": Failed to send response.", e);
							}
						}
					});
					
				} else if (jsonMsg instanceof JSONResponse && callbacks != null
						&& id != null && !id.isNull()) {
					final JSONResponse response = (JSONResponse) jsonMsg;
					final AsyncCallback<JSONResponse> callback = callbacks
							.get(id);
					if (callback != null) {
						AgentHost.getPool().execute(new Runnable() {
							@Override
							public void run() {
								if (response.getError() != null) {
									callback.onFailure(response.getError());
								} else {
									callback.onSuccess(response);
								}
							}
						});
					}
				}
			} else {
				LOG.log(Level.WARNING, getId()
						+ ": Received non-JSON message:'" + msg + "'");
			}
		} catch (Exception e) {
			LOG.log(Level.WARNING, "Exception in receiving message", e);
			// generate JSON error response, skipped if it was an incoming
			// notification i.s.o. request.
			JSONRPCException jsonError = new JSONRPCException(
					JSONRPCException.CODE.INTERNAL_ERROR, e.getMessage(), e);
			JSONResponse response = new JSONResponse(jsonError);
			response.setId(id);
			try {
				send(response, senderUrl, null, tag);
			} catch (Exception e1) {
				LOG.log(Level.WARNING,
						getId() + ": failed to send '"
								+ e.getLocalizedMessage()
								+ "' error to remote agent.", e1);
			}
		}
	}
	
	@Override
	public void send(Object msg, URI receiverUrl,
			AsyncCallback<JSONResponse> callback) throws IOException {
		send(msg, receiverUrl, callback, null);
	}
	
	@Override
	public void send(Object msg, URI receiverUrl,
			AsyncCallback<JSONResponse> callback, String tag)
			throws IOException {
		if (msg instanceof JSONRequest) {
			JSONRequest request = (JSONRequest) msg;
			if (callback != null && callbacks != null) {
				callbacks.store(request.getId(), callback);
			}
			agentHost.sendAsync(receiverUrl, msg, this, tag);
		} else if (msg instanceof JSONRPCException) {
			JSONResponse response = new JSONResponse((JSONRPCException) msg);
			agentHost.sendAsync(receiverUrl, response, this, tag);
		} else {
			agentHost.sendAsync(receiverUrl, msg, this, tag);
		}
	}
}
