/**
 * @file Agent.java
 * 
 * @brief 
 * Agent is the abstract base class for all Eve agents.
 * It provides basic functionality such as id, url, getting methods,
 * subscribing to events, etc. 
 *
 * @license
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy 
 * of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 *
 * Copyright © 2010-2012 Almende B.V.
 *
 * @author 	Jos de Jong, <jos@almende.org>
 * @date	  2012-12-12
 */

package com.almende.eve.agent;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.UUID;

import com.almende.eve.agent.annotation.Access;
import com.almende.eve.agent.annotation.AccessType;
import com.almende.eve.agent.annotation.Name;
import com.almende.eve.agent.annotation.Required;
import com.almende.eve.context.Context;
import com.almende.eve.entity.Callback;
import com.almende.eve.rpc.jsonrpc.JSONRPCException;
import com.almende.eve.rpc.jsonrpc.JSONRequest;
import com.almende.eve.rpc.jsonrpc.JSONResponse;
import com.almende.eve.rpc.jsonrpc.jackson.JOM;
import com.almende.eve.scheduler.Scheduler;
import com.almende.eve.transport.AsyncCallback;
import com.almende.eve.transport.TransportService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;


abstract public class Agent implements AgentInterface {
	private AgentFactory agentFactory = null;
	private Context context = null;
	private Scheduler scheduler = null;
	
	public abstract String getDescription();
	public abstract String getVersion();

	public Agent() {}

	/**
	 * This method is called once in the life time of an agent, at the moment
	 * the agent is being created by the AgentFactory.
	 * It can be overridden and used to perform some action when the agent
	 * is create, in that case super.create() should be called in 
	 * the overridden create().
	 */
	@Access(AccessType.UNAVAILABLE)
	public void create() {}

	/**
	 * This method is called once in the life time of an agent, at the moment
	 * the agent is being deleted by the AgentFactory.
	 * It can be overridden and used to perform some action when the agent
	 * is deleted, in that case super.delete() should be called in 
	 * the overridden delete().
	 */
	@Access(AccessType.UNAVAILABLE)
	public void delete() {
		// TODO: unsubscribe from all subscriptions 
		
		// cancel all scheduled tasks.
		Scheduler scheduler = getScheduler();
		for (String taskId : scheduler.getTasks()) {
			scheduler.cancelTask(taskId);
		}
		
		// remove all keys from the context
		context.clear();
		
		// save the agents class again in the context
		context.put("class", getClass().getName());		
	}
	
	/**
	 * This method is called directly after the agent and its context is 
	 * initiated. 
	 * It can be overridden and used to perform some action when the agent
	 * is initialized, in that case super.init() should be called in 
	 * the overridden init().
	 */
	@Access(AccessType.UNAVAILABLE)
	public void init() {}

	/**
	 * This method can is called when the agent is uninitialized, and is 
	 * needed finalize the context of the agent.
	 * It can be overridden and used to perform some action when the agent
	 * is uninitialized, in that case super.destroy() should be called in 
	 * the overridden destroy().
	 */
	@Access(AccessType.UNAVAILABLE)
	public void destroy() {
		getContext().destroy();
	}
	
	@Override
	@Access(AccessType.UNAVAILABLE)
	protected void finalize() {
		// ensure the context is cleanup when the agent's method destroy is not
		// called.
		getContext().destroy();
	}
	
	/**
	 * Set the context of the agent instance. This method is used by the 
	 * AgentFactory.
	 * @param context
	 */
	@Access(AccessType.UNAVAILABLE)
	final public void setContext(Context context) {
		this.context = context;
	}

	/**
	 * Get the agents context. The context contains methods get, put, etc. to
	 * write properties into a persistent context.
	 * @param context
	 */
	@Access(AccessType.UNAVAILABLE)
	final public Context getContext() {
		return context;
	}

	/**
	 * Get a scheduler to schedule tasks for the agent to be executed later on.
	 * @param context
	 */
	@Access(AccessType.UNAVAILABLE)
	final public Scheduler getScheduler() {
		if (scheduler == null && agentFactory != null) {
			scheduler = agentFactory.getScheduler(getId());
		}
		return scheduler;
	}

	@Access(AccessType.UNAVAILABLE)
	final public void setAgentFactory(AgentFactory agentFactory) {
		this.agentFactory = agentFactory;
	}

	/**
	 * Get the agent factory. The agent factory can create/delete agents. 
	 * @return
	 */
	@Access(AccessType.UNAVAILABLE)
	final public AgentFactory getAgentFactory() {
		return agentFactory;
	}
	
	/**
	 * Clear the agents context, unsubscribe from all subscribed events, 
	 * cancel all running tasks
	 * @Deprecated use delete() instead.
	 */
	@Deprecated
	public void clear() throws Exception {
		delete();
	}

	/**
	 * Retrieve the list with subscriptions on given event.
	 * If there are no subscriptions for this event, an empty list is returned
	 * @param event
	 * @return
	 */
	@SuppressWarnings("unchecked")
	private List<Callback> getSubscriptions(String event) {
		Map<String, List<Callback> > allSubscriptions = 
			(Map<String, List<Callback> >) context.get("subscriptions");
		if (allSubscriptions != null) {
			List<Callback> eventSubscriptions = allSubscriptions.get(event);
			if (eventSubscriptions != null) {
				return eventSubscriptions;
			}
		}
		
		return new ArrayList<Callback>();
	}
	
	/**
	 * Store a list with subscriptions for an event
	 * @param event
	 * @param subscriptions
	 */
	@SuppressWarnings("unchecked")
	private void putSubscriptions(String event, List<Callback> subscriptions) {
		Map<String, List<Callback> > allSubscriptions = 
			(Map<String, List<Callback> >) context.get("subscriptions");
		if (allSubscriptions == null) {
			allSubscriptions = new HashMap<String, List<Callback>> ();
		}
		allSubscriptions.put(event, subscriptions);
		context.put("subscriptions", allSubscriptions);
	}
	
	/**
	 * Let an other agent subscribe to one of this agents events
	 * When the event is triggered, a callback will be send to the provided
	 * callbackUrl.
	 * @param event
	 * @param callbackUrl
	 * @param callbackMethod
	 * @return subscriptionId
	 */
	final public String onSubscribe (
			@Name("event") String event, 
			@Name("callbackUrl") String callbackUrl, 
			@Name("callbackMethod") String callbackMethod) {
		List<Callback> subscriptions = getSubscriptions(event);
		for (Callback subscription : subscriptions) {
			if (subscription.url == null || subscription.method == null){
				continue;
			}
			if (subscription.url.equals(callbackUrl) && 
					subscription.method.equals(callbackMethod)) {
				// The callback already exists. do not duplicate it
				return subscription.id;
			}
		}
		
		// the callback does not yet exist. create it and store it
		String subscriptionId = UUID.randomUUID().toString();
		Callback callback = new Callback(subscriptionId, callbackUrl, callbackMethod);
		subscriptions.add(callback);
		
		// store the subscriptions
		putSubscriptions(event, subscriptions);
		
		return subscriptionId;
	}
	
	/**
	 * Let an other agent unsubscribe from one of this agents events
	 * - If subscriptionId is provided, the subscription with this id will be
	 *   deleted
	 * - If the parameter callbackUrl and optionally event and/or callbackMethod,
	 *   all subscriptions with matching parameters will be deleted. 
	 *   (if only  callbackUrl is provided, all subscriptions from this agent 
	 *   will be deleted).
	 * @param subscriptionId
	 * @param event
	 * @param callbackUrl
	 * @param callbackMethod
	 */
	final public void onUnsubscribe(
			@Required(false) @Name("subscriptionId") String subscriptionId,
			@Required(false) @Name("event") String event, 
			@Required(false) @Name("callbackUrl") String callbackUrl,
			@Required(false) @Name("callbackMethod") String callbackMethod) {
		@SuppressWarnings("unchecked")
		Map<String, List<Callback> > allSubscriptions = 
				(Map<String, List<Callback> >) context.get("subscriptions");
		if (allSubscriptions == null) {
			return;
		}
		
		for (Entry<String, List<Callback>> entry : allSubscriptions.entrySet()) {
			String subscriptionEvent = entry.getKey();
			List<Callback> subscriptions = entry.getValue();
			if (subscriptions != null) {
				int i = 0;
				while (i < subscriptions.size()) {
					Callback subscription = subscriptions.get(i);
					boolean matched = false;
					if (subscriptionId != null && subscriptionId.equals(subscription.id)) {
						// callback with given subscriptionId is found
						matched = true;
					}
					else if (callbackUrl != null && callbackUrl.equals(subscription.url)){
						if ((callbackMethod == null || callbackMethod.equals(subscription.method)) &&
								(event == null || event.equals(subscriptionEvent))) {
							// callback with matching properties is found
							matched = true;
						}
					}
					
					if (matched) {
						subscriptions.remove(i);
					}
					else {
						i++;
					}
				}
			}
			// TODO: cleanup event list when empty
		}
		
		// store context again
		context.put("subscriptions", allSubscriptions);
	}
	
	/**
	 * Asynchronously trigger an event.
	 * the onTrigger method is called from a scheduled task, initiated in the 
	 * method trigger
	 * @param url
	 * @param method
	 * @param params
	 * @throws Exception 
	 */
	final public void onTrigger (
			@Name("url") String url, 
			@Name("method") String method, 
			@Name("params") ObjectNode params) throws Exception {
		// TODO: send the trigger as a JSON-RPC 2.0 Notification
		// TODO: catch exceptions and log them here?
		send(url, method, params);
	}
	
	/**
	 * Subscribe to an other agents event
	 * @param url
	 * @param event
	 * @param callbackMethod
	 * @return subscriptionId
	 * @throws Exception
	 */
	protected String subscribe(String url, String event, String callbackMethod) 
			throws Exception {
		String method = "onSubscribe";
		ObjectNode params = JOM.createObjectNode();
		params.put("event", event);
		params.put("callbackUrl", getFirstUrl());
		params.put("callbackMethod", callbackMethod);
		// TODO: store the agents subscriptions locally
		return send(url, method, params, String.class);
	}

	/**
	 * Unsubscribe from an other agents event
	 * @param url
	 * @param subscriptionId
	 * @throws Exception
	 */
	protected void unsubscribe(String url, String subscriptionId) throws Exception {
		String method = "onUnsubscribe";
		ObjectNode params = JOM.createObjectNode();
		params.put("subscriptionId", subscriptionId);
		send(url, method, params);
	}
	
	/**
	 * Unsubscribe from an other agents event
	 * @param url
	 * @param event
	 * @param callbackMethod
	 * @throws Exception
	 */
	protected void unsubscribe(String url, String event, String callbackMethod) 
			throws Exception {
		String method = "onUnsubscribe";
		ObjectNode params = JOM.createObjectNode();
		params.put("event", event);
		params.put("callbackUrl", getFirstUrl());
		params.put("callbackMethod", callbackMethod);
		send(url, method, params);
	}

	/**
	 * Trigger an event
	 * @param event
	 * @param params   An ObjectNode, Map, or POJO
	 * @throws Exception 
	 * @throws JSONRPCException 
	 */
	@Access(AccessType.UNAVAILABLE)
	final public void trigger(@Name("event") String event, 
			@Name("params") Object params) throws Exception {
		// TODO: user first url is very dangerous! can cause a mismatch
		String url = getFirstUrl();
		List<Callback> subscriptions = new ArrayList<Callback>();

		if (event.equals("*")) {
			throw new Exception("Cannot trigger * event");
		}

		// send a trigger to the agent factory
		getAgentFactory().getEventLogger().log(getId(), event, params);
		
		// retrieve subscriptions from the event
		List<Callback> valueEvent = getSubscriptions(event);
		subscriptions.addAll(valueEvent);
		
		// retrieve subscriptions from the all event "*"
		List<Callback> valueAll = getSubscriptions("*");
		subscriptions.addAll(valueAll);
		
		// TODO: smartly remove double entries?
		ObjectNode callbackParams = JOM.createObjectNode();
		callbackParams.put("agent", url);
		callbackParams.put("event", event);
		if (params instanceof JsonNode) {
			callbackParams.put("params", (ObjectNode) params);
		}
		else {
			ObjectNode jsonParams = JOM.getInstance().convertValue(params, ObjectNode.class);
			callbackParams.put("params", jsonParams);
		}
		
		for (Callback subscription : subscriptions) {
			// create a task to send this trigger. 
			// This way, it is sent asynchronously and cannot block this
			// trigger method
			callbackParams.put("subscriptionId", subscription.id); // TODO: test if changing subscriptionId works with multiple tasks

			ObjectNode taskParams = JOM.createObjectNode();
			taskParams.put("url", subscription.url);
			taskParams.put("method", subscription.method);
			taskParams.put("params", callbackParams);
			JSONRequest request = new JSONRequest("onTrigger", taskParams);
			long delay = 0;
			getScheduler().createTask(request, delay);
		}
	}

	/**
	 * Get the first url of the agents urls. Returns null if the agent does not
	 * have any urls.
	 * @return firstUrl
	 */
	private String getFirstUrl() {
		List<String> urls = getUrls();
		if (urls.size() > 0) {
			return urls.get(0);
		}
		return null;
	}
	
	/**
	 * Get all available methods of this agent
	 * @return
	 */
	final public List<Object> getMethods(
			@Name("asJSON") @Required(false) Boolean asJSON) {
		return getAgentFactory().getMethods(this, asJSON);
	}
	
	/**
	 * Send a request to an agent in JSON-RPC format
	 * @param url    The url of the agent
	 * @param method The name of the method
	 * @param params A Object containing the parameter values of the method.
	 *               This can be an ObjectNode, Map, or POJO.
	 * @param type   The return type of the method
	 * @return       
	 * @throws Exception 
	 */
	@Access(AccessType.UNAVAILABLE)
	final public <T> T send(String url, String method, Object params, 
			Class<T> type) throws Exception {
		// TODO: implement support for adding custom http headers (for authorization for example)
	
		ObjectNode jsonParams;
		if (params instanceof ObjectNode) {
			jsonParams = (ObjectNode) params;
		}
		else {
			jsonParams = JOM.getInstance().convertValue(params, ObjectNode.class);
		}
		
		// invoke the other agent via the context, allowing the context
		// to route the request internally or externally
		String id = UUID.randomUUID().toString();
		JSONRequest request = new JSONRequest(id, method, jsonParams);
		JSONResponse response = getAgentFactory().send(getId(), url, request);
		JSONRPCException err = response.getError();
		if (err != null) {
			throw err;
		}
		if (type != null && type != void.class) {
			return response.getResult(type);
		}
		
		return null;
	}

	/**
	 * Send a request to an agent in JSON-RPC format
	 * @param url    The url of the agent
	 * @param method The name of the method
	 * @return       
	 * @throws Exception 
	 */
	@Access(AccessType.UNAVAILABLE)
	final public <T> T send(String url, String method, Class<T> type) 
			throws Exception {
		return send(url, method, null, type);
	}
	
	/**
	 * Send a request to an agent in JSON-RPC format
	 * @param url    The url of the agent
	 * @param method The name of the method
	 * @param params A Object containing the parameter values of the method.
	 *               This can be an ObjectNode, Map, or POJO.
	 * @return 
	 * @throws Exception 
	 */
	@Access(AccessType.UNAVAILABLE)
	final public void send(String url, String method, Object params) 
			throws Exception {
		send(url, method, params, void.class);
	}
	
	/**
	 * Create a proxy to an other agent. Invoked methods will be send to the 
	 * actual agent via the AgentFactory.
	 * @param url
	 * @param agentInterface  A Java Interface, extending AgentInterface
	 * @return agentProxy
	 */
	@Access(AccessType.UNAVAILABLE)
	final public <T> T createAgentProxy(String url, Class<T> agentInterface) {
		return getAgentFactory().createAgentProxy(getId(), url, agentInterface);
	}
	
	/**
	 * Send a request to an agent in JSON-RPC format
	 * @param url    The url of the agent
	 * @param method The name of the method
	 * @return 
	 * @throws Exception 
	 */
	@Access(AccessType.UNAVAILABLE)
	final public void send(String url, String method) 
			throws Exception {
		send(url, method, null, void.class);
	}

	/**
	 * Send an asynchronous JSON-RPC request to an agent
	 * sendAsync is not supported on Google App Engine
	 * @param url             The url of the agent to be called
	 * @param method          The name of the method
	 * @param params          A JSONObject containing the parameter 
	 *                         values of the method
	 * @param callback        An AsyncCallback of which the onSuccess or
	 *                         onFailure method will be executed on callback.
	 * @param type            The type of result coming from the callback.                        
	 * @throws Exception 
	 * @throws JSONException 
	 */
	@Access(AccessType.UNAVAILABLE)
	final public <T> void sendAsync(String url, String method, ObjectNode params,
			final AsyncCallback<T> callback, final Class<T> type) throws Exception {
		String id = UUID.randomUUID().toString();
		JSONRequest request = new JSONRequest(id, method, params);
		sendAsync(url, request, callback, type);
	}
	
	/**
	 * Send an asynchronous JSON-RPC request to an agent
	 * sendAsync is not supported on Google App Engine
	 * @param url             The url of the agent to be called
	 * @param request         JSON-RPC request containing method and params
	 * @param callback        An AsyncCallback of which the onSuccess or
	 *                         onFailure method will be executed on callback.
	 * @param type            The type of result coming from the callback.                        
	 * @throws Exception 
	 * @throws JSONException 
	 */
	@Access(AccessType.UNAVAILABLE)
	final public <T> void sendAsync(final String url, final JSONRequest request,
			final AsyncCallback<T> callback, final Class<T> type) throws Exception {

		// Create a callback to retrieve a JSONResponse and extract the result
		// or error from this.
		final AsyncCallback<JSONResponse> responseCallback = 
				new AsyncCallback<JSONResponse>() {
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
				if (type != null && type != void.class) {
					callback.onSuccess(response.getResult(type));
				}
				else {
					callback.onSuccess(null);
				}
			}

			@Override
			public void onFailure(Exception exception) {
				callback.onFailure(exception);
			}
		};
		
		getAgentFactory().sendAsync(getId(), url, request, responseCallback);
	}

	/**
	 * Get the urls of this agent, for example "http://mysite.com/agents/key".
	 * An agent can have multiple urls for different configured communication
	 * services, such as HTTP and XMPP.
	 * @return urls
	 */
	final public List<String> getUrls() {
		List<String> urls = new ArrayList<String>();
		if (agentFactory != null) {
			String agentId = getId();
			for (TransportService service : agentFactory.getTransportServices()) {
				String url = service.getAgentUrl(agentId);
				if (url != null) {
					urls.add(url);
				}
			}
		}
		return urls;
	}
	
	/**
	 * Get the Id of this agent
	 * @return
	 */
	final public String getId() {
		return context.getAgentId();
	}
	
	/**
	 * Retrieve the type name of this agent, its class
	 * @return classname
	 */
	final public String getType() {
		return getClass().getSimpleName();
	}

	@Override
	public String toString() {
		Map<String, Object> data = new HashMap<String, Object>();
		data.put("class", this.getClass().getName());
		data.put("id", getId());
		return data.toString();
	}
}
