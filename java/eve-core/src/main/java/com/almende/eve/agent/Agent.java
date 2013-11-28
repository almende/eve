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
import java.net.ProtocolException;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.almende.eve.agent.annotation.Namespace;
import com.almende.eve.agent.proxy.AsyncProxy;
import com.almende.eve.event.EventsFactory;
import com.almende.eve.event.EventsInterface;
import com.almende.eve.monitor.ResultMonitorFactory;
import com.almende.eve.monitor.ResultMonitorFactoryInterface;
import com.almende.eve.rpc.annotation.Access;
import com.almende.eve.rpc.annotation.AccessType;
import com.almende.eve.rpc.jsonrpc.JSONRPCException;
import com.almende.eve.rpc.jsonrpc.JSONRequest;
import com.almende.eve.rpc.jsonrpc.JSONResponse;
import com.almende.eve.rpc.jsonrpc.jackson.JOM;
import com.almende.eve.scheduler.Scheduler;
import com.almende.eve.state.State;
import com.almende.eve.state.TypedKey;
import com.almende.eve.transport.AsyncCallback;
import com.almende.eve.transport.TransportService;
import com.almende.util.TypeUtil;
import com.almende.util.uuid.UUID;
import com.fasterxml.jackson.databind.JavaType;
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
		if ("create".equals(event.getEvent())) {
			create();
		} else if ("init".equals(event.getEvent())) {
			init();
		} else if ("delete".equals(event.getEvent())) {
			delete();
		} else if ("setSchedulerFactory".equals(event.getEvent())) {
			// init scheduler tasks
			this.scheduler = agentHost.getScheduler(this);
		} else if ("addTransportService".equals(event.getEvent())) {
			TransportService service = (TransportService) event.getService();
			service.reconnect(getId());
		}
	}
	
	/**
	 * This method is called once in the life time of an agent, at the moment
	 * the agent is being created by the AgentHost.
	 * It can be overridden and used to perform some action when the agent
	 * is create, in that case super.create() should be called in
	 * the overridden create().
	 */
	@Access(AccessType.UNAVAILABLE)
	protected void create() {
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
	 * This method is called directly after the agent and its state is
	 * initiated.
	 * It can be overridden and used to perform some action when the agent
	 * is initialized, in that case super.init() should be called in
	 * the overridden init().
	 */
	@Access(AccessType.UNAVAILABLE)
	protected void init() {
	}
	
	/**
	 * This method is called by the finalize method (GC) upon unloading of the
	 * agent from memory.
	 */
	@Access(AccessType.UNAVAILABLE)
	protected void destroy() {
	}
	
	/**
	 * This method is called once in the life time of an agent, at the moment
	 * the agent is being deleted by the AgentHost.
	 * It can be overridden and used to perform some action when the agent
	 * is deleted, in that case super.delete() should be called in
	 * the overridden delete().
	 */
	@Access(AccessType.UNAVAILABLE)
	protected void delete() {
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
		destroy();
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
			throws ProtocolException, JSONRPCException {
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
		String id = new UUID().toString();
		// String id = "hi there!";
		JSONRequest request = new JSONRequest(id, method, jsonParams);
		JSONResponse response = getAgentHost().send(this, url, request);
		JSONRPCException err = response.getError();
		if (err != null) {
			throw err;
		}
		return response;
	}
	
	@Override
	@Access(AccessType.UNAVAILABLE)
	public final <T> T send(URI url, String method, Object params, Class<T> type)
			throws ProtocolException, JSONRPCException {
		return TypeUtil.inject(locSend(url, method, params).getResult(), type);
	}
	
	@Override
	@Access(AccessType.UNAVAILABLE)
	public final <T> T send(URI url, String method, Object params, Type type)
			throws ProtocolException, JSONRPCException {
		return TypeUtil.inject(locSend(url, method, params).getResult(), type);
	}
	
	@Override
	@Access(AccessType.UNAVAILABLE)
	public final <T> T send(URI url, String method, Object params,
			TypeUtil<T> type) throws ProtocolException, JSONRPCException {
		return type.inject(locSend(url, method, params).getResult());
	}
	
	@Override
	@Access(AccessType.UNAVAILABLE)
	public final <T> T send(URI url, String method, Object params, JavaType type)
			throws ProtocolException, JSONRPCException {
		return TypeUtil.inject(locSend(url, method, params).getResult(), type);
	}
	
	@Override
	@Access(AccessType.UNAVAILABLE)
	public final <T> T send(URI url, String method, Type type)
			throws ProtocolException, JSONRPCException {
		return TypeUtil.inject(locSend(url, method, null).getResult(), type);
	}
	
	@Override
	@Access(AccessType.UNAVAILABLE)
	public final <T> T send(URI url, String method, JavaType type)
			throws ProtocolException, JSONRPCException {
		return TypeUtil.inject(locSend(url, method, null).getResult(), type);
	}
	
	@Override
	@Access(AccessType.UNAVAILABLE)
	public final <T> T send(URI url, String method, Class<T> type)
			throws ProtocolException, JSONRPCException {
		
		return TypeUtil.inject(locSend(url, method, null).getResult(), type);
	}
	
	@Override
	@Access(AccessType.UNAVAILABLE)
	public final <T> T send(URI url, String method, TypeUtil<T> type)
			throws ProtocolException, JSONRPCException {
		
		return type.inject(locSend(url, method, null).getResult());
	}
	
	@Override
	@Access(AccessType.UNAVAILABLE)
	public final void send(URI url, String method, Object params)
			throws ProtocolException, JSONRPCException {
		locSend(url, method, params);
	}
	
	@Override
	@Access(AccessType.UNAVAILABLE)
	public final void send(URI url, String method) throws ProtocolException,
			JSONRPCException {
		locSend(url, method, null);
	}
	
	@Override
	@Access(AccessType.UNAVAILABLE)
	public final <T> T createAgentProxy(URI url, Class<T> agentInterface) {
		return getAgentHost().createAgentProxy(this, url, agentInterface);
	}
	
	@Override
	@Access(AccessType.UNAVAILABLE)
	public final <T> AsyncProxy<T> createAsyncAgentProxy(URI url,
			Class<T> agentInterface) {
		return getAgentHost().createAsyncAgentProxy(this, url, agentInterface);
	}
	
	@Override
	@Access(AccessType.UNAVAILABLE)
	public final void sendAsync(final URI url, final String method,
			final ObjectNode params) throws ProtocolException, JSONRPCException {
		sendAsync(url, method, params, null, Void.class);
	}
	
	@Override
	@Access(AccessType.UNAVAILABLE)
	public final void sendAsync(final URI url, final String method)
			throws ProtocolException, JSONRPCException {
		sendAsync(url, method, null, null, Void.class);
	}
	
	@Override
	@Access(AccessType.UNAVAILABLE)
	public final <T> void sendAsync(final URI url, final String method,
			final ObjectNode params, final AsyncCallback<T> callback,
			final Class<T> type) throws ProtocolException, JSONRPCException {
		String id = new UUID().toString();
		JSONRequest request = new JSONRequest(id, method, params);
		sendAsync(url, request, callback, JOM.getTypeFactory()
				.uncheckedSimpleType(type));
	}
	
	@Override
	@Access(AccessType.UNAVAILABLE)
	public final <T> void sendAsync(final URI url, final String method,
			final ObjectNode params, final AsyncCallback<T> callback,
			final Type type) throws ProtocolException, JSONRPCException {
		String id = new UUID().toString();
		JSONRequest request = new JSONRequest(id, method, params);
		sendAsync(url, request, callback,
				JOM.getTypeFactory().constructType(type));
	}
	
	@Override
	@Access(AccessType.UNAVAILABLE)
	public final <T> void sendAsync(final URI url, final String method,
			final ObjectNode params, final AsyncCallback<T> callback,
			final JavaType type) throws ProtocolException, JSONRPCException {
		String id = new UUID().toString();
		JSONRequest request = new JSONRequest(id, method, params);
		sendAsync(url, request, callback, type);
	}
	
	@Override
	@Access(AccessType.UNAVAILABLE)
	public final <T> void sendAsync(final URI url, final JSONRequest request,
			final AsyncCallback<T> callback, final Class<T> type)
			throws ProtocolException, JSONRPCException {
		sendAsync(url, request, callback, JOM.getTypeFactory()
				.uncheckedSimpleType(type));
	}
	
	@Override
	@Access(AccessType.UNAVAILABLE)
	public final <T> void sendAsync(final URI url, final JSONRequest request,
			final AsyncCallback<T> callback, final Type type)
			throws ProtocolException, JSONRPCException {
		sendAsync(url, request, callback,
				JOM.getTypeFactory().constructType(type));
	}
	
	@Override
	@Access(AccessType.UNAVAILABLE)
	public final <T> void sendAsync(final URI url, final JSONRequest request,
			final AsyncCallback<T> callback, final JavaType type)
			throws ProtocolException, JSONRPCException {
		
		// Create a callback to retrieve a JSONResponse and extract the result
		// or error from this.
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
											+ request.getMethod() + "@" + url, cce));
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
		
		getAgentHost().sendAsync(this, url, request, responseCallback);
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
}
