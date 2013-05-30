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

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.almende.eve.agent.annotation.Namespace;
import com.almende.eve.agent.proxy.AsyncProxy;
import com.almende.eve.event.EventsFactory;
import com.almende.eve.event.EventsInterface;
import com.almende.eve.monitor.ResultMonitorFactory;
import com.almende.eve.monitor.ResultMonitorInterface;
import com.almende.eve.rpc.annotation.Access;
import com.almende.eve.rpc.annotation.AccessType;
import com.almende.eve.rpc.jsonrpc.JSONRPCException;
import com.almende.eve.rpc.jsonrpc.JSONRequest;
import com.almende.eve.rpc.jsonrpc.JSONResponse;
import com.almende.eve.rpc.jsonrpc.jackson.JOM;
import com.almende.eve.scheduler.Scheduler;
import com.almende.eve.state.State;
import com.almende.eve.transport.AsyncCallback;
import com.almende.eve.transport.TransportService;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.node.ObjectNode;

@Access(AccessType.UNAVAILABLE)
abstract public class Agent implements AgentInterface {
	protected AgentFactory			agentFactory	= null;
	protected State					state			= null;
	protected Scheduler				scheduler		= null;
	protected ResultMonitorInterface monitorFactory	= null;
	protected EventsInterface		eventsFactory	= null;
	
	@Access(AccessType.PUBLIC)
	public abstract String getDescription();
	
	@Access(AccessType.PUBLIC)
	public abstract String getVersion();
	
	public Agent() {
	}

	public void constr(AgentFactory factory, State state) {
		if (this.state == null) {
			this.agentFactory = factory;
			this.state = state;
			this.scheduler = factory.getScheduler(this);
			this.monitorFactory = new ResultMonitorFactory(this);
			this.eventsFactory = new EventsFactory(this);
		}
	}
	
	@Override
	@Access(AccessType.UNAVAILABLE)
	public boolean onAccess(String senderId, String functionTag) {
		return true;
	}
	
	@Override
	@Access(AccessType.UNAVAILABLE)
	public boolean onAccess(String senderId) {
		return onAccess(senderId, null);
	}
	
	@Override
	@Access(AccessType.UNAVAILABLE)
	public void create() {
	}
	
	@Override
	@Access(AccessType.UNAVAILABLE)
	public void delete() {
		// TODO: unsubscribe from all subscriptions
		
		// cancel all scheduled tasks.
		Scheduler scheduler = getScheduler();
		if (scheduler != null) {
			for (String taskId : scheduler.getTasks()) {
				scheduler.cancelTask(taskId);
			}
		}
		// remove all keys from the state
		// Note: the state itself will be deleted by the AgentFactory
		state.clear();
		
		// save the agents class again in the state
		state.put(State.KEY_AGENT_TYPE, getClass().getName());
		state = null; // forget local reference, as it can keep the State alive
						// even if the agentFactory removes the file.
	}
	
	@Override
	@Access(AccessType.UNAVAILABLE)
	public void boot() throws Exception {
		// init scheduler tasks
		getScheduler();
		// if applicable reconnect existing connections.
		List<TransportService> services = agentFactory.getTransportServices();
		if (services != null) {
			for (TransportService service : services) {
				service.reconnect(getId());
			}
		}
	}
	
	@Override
	@Access(AccessType.UNAVAILABLE)
	public void init() {
	}
	
	@Override
	@Access(AccessType.UNAVAILABLE)
	public void destroy() {
		getState().destroy();
	}
	
	@Override
	@Access(AccessType.UNAVAILABLE)
	protected void finalize() {
		// ensure the state is cleanup when the agent's method destroy is not
		// called.
		getState().destroy();
	}
	
	@Override
	@Access(AccessType.UNAVAILABLE)
	final public State getState() {
		return state;
	}
	
	@Override
	@Namespace("scheduler")
	final public Scheduler getScheduler() {
		return scheduler;
	}
	
	@Override
	@Access(AccessType.UNAVAILABLE)
	final public AgentFactory getAgentFactory() {
		return agentFactory;
	}
	
	@Override
	@Namespace("monitor")
	final public ResultMonitorInterface getResultMonitorFactory() {
		return monitorFactory;
	}
	
	@Override
	@Namespace("event")
	final public EventsInterface getEventsFactory() {
		return eventsFactory;
	}
	
	@Override
	@Access(AccessType.PUBLIC)
	public String getFirstUrl() {
		List<String> urls = getUrls();
		if (urls.size() > 0) {
			return urls.get(0);
		}
		return "local://" + getId();
	}
	
	@Override
	@Access(AccessType.PUBLIC)
	public List<Object> getMethods() {
		return getAgentFactory().getMethods(this);
	}
	
	@Override
	@Access(AccessType.UNAVAILABLE)
	@Deprecated
	final public <T> T send(String url, String method, Object params,
			Class<T> type) throws Exception {
		return send(url,method,params,JOM.getTypeFactory().uncheckedSimpleType(type));
	}

	@Override
	@SuppressWarnings("unchecked")
	@Access(AccessType.UNAVAILABLE)
	final public <T> T send(String url, String method, Object params,
			Type type) throws Exception {
		return (T)send(url,method,params,JOM.getTypeFactory().constructType(type));
	}

	@Override
	@SuppressWarnings("unchecked")
	@Access(AccessType.UNAVAILABLE)
	final public <T> T send(String url, String method, Object params,
			JavaType type) throws Exception {
		// TODO: implement support for adding custom http headers (for
		// authorization for example)
		
		ObjectNode jsonParams;
		if (params instanceof ObjectNode) {
			jsonParams = (ObjectNode) params;
		} else {
			jsonParams = JOM.getInstance().convertValue(params,
					ObjectNode.class);
		}
		
		// invoke the other agent via the agentFactory, allowing the factory
		// to route the request internally or externally
		String id = UUID.randomUUID().toString();
		JSONRequest request = new JSONRequest(id, method, jsonParams);
		JSONResponse response = getAgentFactory().send(this, url, request);
		JSONRPCException err = response.getError();
		if (err != null) {
			throw err;
		}
		if (type != null && !type.hasRawClass(Void.class)) {
			return (T)response.getResult(type);
		}
		
		return (T)null;
	}

	@Override
	@SuppressWarnings("unchecked")
	@Access(AccessType.UNAVAILABLE)
	final public <T> T send(String url, String method, Type type)
			throws Exception {
		return (T)send(url, method, null, type);
	}	

	@Override
	@SuppressWarnings("unchecked")
	@Access(AccessType.UNAVAILABLE)
	final public <T> T send(String url, String method, JavaType type)
			throws Exception {
		return (T)send(url, method, null, type);
	}
	

	@Override
	@Access(AccessType.UNAVAILABLE)
	@Deprecated
	final public <T> T send(String url, String method, Class<T> type)
			throws Exception {
		
		return send(url, method, null, JOM.getTypeFactory().uncheckedSimpleType(type));
	}
	
	@Override
	@Access(AccessType.UNAVAILABLE)
	final public void send(String url, String method, Object params)
			throws Exception {
		send(url, method, params, JOM.getVoid());
	}
	
	@Override
	@Access(AccessType.UNAVAILABLE)
	final public void send(String url, String method) throws Exception {
		send(url, method, null, JOM.getVoid());
	}
	
	@Override
	@Access(AccessType.UNAVAILABLE)
	final public <T> T createAgentProxy(String url, Class<T> agentInterface) {
		return getAgentFactory().createAgentProxy(this, url, agentInterface);
	}
	
	@Override
	@Access(AccessType.UNAVAILABLE)
	final public <T> AsyncProxy<T> createAsyncAgentProxy(String url,
			Class<T> agentInterface) {
		return getAgentFactory().createAsyncAgentProxy(this, url,
				agentInterface);
	}
	
	@Override
	@Access(AccessType.UNAVAILABLE)
	@Deprecated
	final public <T> void sendAsync(String url, String method,
			ObjectNode params, final AsyncCallback<T> callback,
			Class<T> type) throws Exception {
		String id = UUID.randomUUID().toString();
		JSONRequest request = new JSONRequest(id, method, params);
		sendAsync(url, request, callback, JOM.getTypeFactory().uncheckedSimpleType(type));
	}
	
	@Override
	@Access(AccessType.UNAVAILABLE)
	final public <T> void sendAsync(String url, String method,
			ObjectNode params, final AsyncCallback<T> callback,
			Type type) throws Exception {
		String id = UUID.randomUUID().toString();
		JSONRequest request = new JSONRequest(id, method, params);
		sendAsync(url, request, callback, JOM.getTypeFactory().constructType(type));
	}

	@Override
	@Access(AccessType.UNAVAILABLE)
	final public <T> void sendAsync(String url, String method,
			ObjectNode params, final AsyncCallback<T> callback,
			final JavaType type) throws Exception {
		String id = UUID.randomUUID().toString();
		JSONRequest request = new JSONRequest(id, method, params);
		sendAsync(url, request, callback, type);
	}
	
	@Override
	@Access(AccessType.UNAVAILABLE)
	@Deprecated
	final public <T> void sendAsync(final String url,
			final JSONRequest request, final AsyncCallback<T> callback,
			Class<T> type) throws Exception {
		sendAsync(url, request, callback, JOM.getTypeFactory().uncheckedSimpleType(type));
	}
	
	@Override
	@Access(AccessType.UNAVAILABLE)
	final public <T> void sendAsync(final String url,
			final JSONRequest request, final AsyncCallback<T> callback,
			Type type) throws Exception {
		sendAsync(url, request, callback, JOM.getTypeFactory().constructType(type));
	}

	@Override
	@Access(AccessType.UNAVAILABLE)
	final public <T> void sendAsync(final String url,
			final JSONRequest request, final AsyncCallback<T> callback,
			final JavaType type) throws Exception {
		
		// Create a callback to retrieve a JSONResponse and extract the result
		// or error from this.
		final AsyncCallback<JSONResponse> responseCallback = new AsyncCallback<JSONResponse>() {
			@SuppressWarnings("unchecked")
			@Override
			public void onSuccess(JSONResponse response) {
				Exception err;
				try {
					err = response.getError();
				} catch (JSONRPCException e) {
					err = e;
				}
				if (err != null) {
					callback.onFailure(err);
				}
				if (type != null && !type.hasRawClass(Void.class)) {
					callback.onSuccess((T) response.getResult(type));
				} else {
					callback.onSuccess(null);
				}
			}
			
			@Override
			public void onFailure(Exception exception) {
				callback.onFailure(exception);
			}
		};
		
		getAgentFactory().sendAsync(this, url, request, responseCallback);
	}
	
	@Override
	@Access(AccessType.PUBLIC)
	public List<String> getUrls() {
		List<String> urls = new ArrayList<String>();
		if (agentFactory != null) {
			String agentId = getId();
			for (TransportService service : agentFactory.getTransportServices()) {
				String url = service.getAgentUrl(agentId);
				if (url != null) {
					urls.add(url);
				}
			}
		} else {
			System.err.println("AgentFactory not initialized?!?");
		}
		return urls;
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
