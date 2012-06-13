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
 * @date	  2012-04-04
 */

package com.almende.eve.agent;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import com.almende.eve.agent.annotation.Access;
import com.almende.eve.agent.annotation.AccessType;
import com.almende.eve.context.Context;
import com.almende.eve.session.Session;
import com.almende.eve.json.JSONRPC;
import com.almende.eve.json.JSONRPCException;
import com.almende.eve.json.JSONRequest;
import com.almende.eve.json.JSONResponse;
import com.almende.eve.json.annotation.Name;
import com.almende.eve.json.annotation.Required;
import com.almende.eve.json.jackson.JOM;
import com.fasterxml.jackson.databind.node.ObjectNode;


abstract public class Agent {
	private Context context = null;
	private Session session = null;
	
	@SuppressWarnings("rawtypes")
	private static Class<? extends ArrayList> LIST_CALLBACK_CLASS = (new ArrayList<Callback>()).getClass();

	public abstract String getDescription();
	public abstract String getVersion();

	public Agent() {}
	
	/**
	 * This method is called directly after the agent and its context is 
	 * initiated. 
	 * It can be used overridden and used to initialize variables for the agent. 
	 */
	@Access(AccessType.UNAVAILABLE)
	public void init() {}
	
	@Access(AccessType.UNAVAILABLE)
	final public void setContext(Context context) {
		if (context != null) {
			this.context = context;
		}
	}

	@Access(AccessType.UNAVAILABLE)
	final public Context getContext() {
		return context;
	}
	
	@Access(AccessType.UNAVAILABLE)
	final public void setSession(Session session) {
		if (session != null) {
			this.session = session;
		}
	}

	@Access(AccessType.UNAVAILABLE)
	final public Session getSession() {
		return session;
	}

	/**
	 * Clear the agents context, unsubscribe from all subscribed events, 
	 * cancel all running tasks
	 */
	public void clear() throws Exception {
	// final public void clear() {
		// TODO: unsubscribe from all subscriptions 
		
		// TODO: cancel all scheduled tasks.

		// TODO: loop through the context keys and remove everything.
	}
	
	/**
	 * Retrieve the type name of this agent, its class
	 * @return classname
	 */
	final public String getType() {
		return this.getClass().getSimpleName();
	}

	/**
	 * Create the subscription key for a given event.
	 * the resulting key will be "subscriptions.event"
	 * @param event
	 * @return
	 */
	private String getSubscriptionsKey(String event) {
		String key = "subscriptions." + event;
		return key;
	}
	
	/**
	 * Subscribe to an event.
	 * When the event is triggered, a callback will be send to the provided
	 * callbackUrl.
	 * @param event
	 * @param callbackUrl
	 * @param callbackMethod
	 */
	//@Access(AccessType.PRIVATE) // TODO
	@SuppressWarnings("unchecked")
	final public void subscribe(
			@Name("event") String event, 
			@Name("callbackUrl") String callbackUrl, 
			@Name("callbackMethod") String callbackMethod) {
		
		String key = getSubscriptionsKey(event);
		List<Callback> subscriptions = context.get(key, LIST_CALLBACK_CLASS);		
		if (subscriptions == null) {
			subscriptions = new ArrayList<Callback>(); 
		}
		
		for (Callback s : subscriptions) {
			if (s.callbackUrl.equals(callbackUrl) && 
					s.callbackMethod.equals(callbackMethod)) {
				// The callback already exists. do not duplicate it
				return;
			}
		}
		
		// the callback does not yet exist. create it and store it
		Callback callback = new Callback(callbackUrl, callbackMethod);
		subscriptions.add(callback);
		context.put(key, subscriptions);
	}
	
	/**
	 * Unsubscribe from an event
	 * @param event
	 * @param callbackUrl
	 */
	//@Access(AccessType.PRIVATE) // TODO
	@SuppressWarnings("unchecked")
	final public void unsubscribe(
			@Name("event") String event, 
			@Name("callbackUrl") String callbackUrl,
			@Name("callbackMethod") String callbackMethod) {
		String key = getSubscriptionsKey(event);
		List<Callback> subscriptions = context.get(key, LIST_CALLBACK_CLASS);
		if (subscriptions != null) {
			for (Callback s : subscriptions) {
				if (s.callbackUrl.equals(callbackUrl) && 
						s.callbackMethod.equals(callbackMethod)) {
					// callback is found. remove it and store the subscriptions 
					//again
					subscriptions.remove(s);
					context.put(key, subscriptions);
					return;
				}
			}
		}
	}

	/**
	 * Trigger an event
	 * @param event
	 * @param params
	 * @throws Exception 
	 * @throws JSONRPCException 
	 */
	@Access(AccessType.UNAVAILABLE)
	@SuppressWarnings("unchecked")
	final public void trigger(@Name("event") String event, 
			@Name("params") ObjectNode params) throws JSONRPCException, Exception {
		String url = getUrl();
		List<Callback> subscriptions = new ArrayList<Callback>();

		if (event.equals("*")) {
			throw new Exception("Cannot trigger * event");
		}

		// retrieve subscriptions from the event
		String keyEvent = getSubscriptionsKey(event);
		List<Callback> valueEvent = context.get(keyEvent, LIST_CALLBACK_CLASS);
		if (valueEvent != null) {
			subscriptions.addAll(valueEvent);
		}
		
		// retrieve subscriptions from the all event "*"
		String keyAll = getSubscriptionsKey("*");
		List<Callback> valueAll = context.get(keyAll, LIST_CALLBACK_CLASS);
		if (valueAll != null) {
			subscriptions.addAll(valueAll);
		}
		
		// TODO: smartly remove double entries?
		ObjectNode callbackParams = JOM.createObjectNode();
		callbackParams.put("agent", url);
		callbackParams.put("event", event);
		callbackParams.put("params", params);
		
		for (Callback s : subscriptions) {
			try {
				send(s.callbackUrl, s.callbackMethod, callbackParams);
			} catch (Exception e) {
				e.printStackTrace();
				// TODO: how to handle exceptions in trigger?
			}
		}
	}

	/**
	 * Get all available methods of this agent
	 * @return
	 */
	final public List<Object> getMethods(@Name("asJSON") 
			@Required(false) Boolean asJSON) {
		return JSONRPC.describe(this.getClass(), asJSON);
	}
	
	/**
	 * Send a request to an agent in JSON-RPC format
	 * @param url    The url of the agent
	 * @param method The name of the method
	 * @param params A ObjectNode containing the parameter values of the method
	 * @return       
	 * @throws JSONRPCException 
	 * @throws IOException 
	 */
	@Access(AccessType.UNAVAILABLE)
	final public <T> T send(String url, String method, ObjectNode params, Class<T> type) 
			throws IOException, JSONRPCException {
		JSONResponse response = JSONRPC.send(url, new JSONRequest(method, params));
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
	 * @throws JSONRPCException 
	 * @throws IOException 
	 */
	@Access(AccessType.UNAVAILABLE)
	final public <T> T send(String url, String method, Class<T> type) 
			throws JSONRPCException, IOException {
		return send(url, method, null, type);
	}
	
	/**
	 * Send a request to an agent in JSON-RPC format
	 * @param url    The url of the agent
	 * @param method The name of the method
	 * @param params A ObjectNode containing the parameter values of the method
	 * @return 
	 * @throws JSONRPCException 
	 * @throws IOException 
	 */
	@Access(AccessType.UNAVAILABLE)
	final public void send(String url, String method, ObjectNode params) 
			throws JSONRPCException, IOException {
		send(url, method, params, void.class);
	}

	/**
	 * Send a request to an agent in JSON-RPC format
	 * @param url    The url of the agent
	 * @param method The name of the method
	 * @return 
	 * @throws JSONRPCException 
	 * @throws IOException 
	 */
	@Access(AccessType.UNAVAILABLE)
	final public void send(String url, String method) 
			throws JSONRPCException, IOException {
		send(url, method, null, void.class);
	}


	/**
	 * Send a request to an agent in JSON-RPC 1.0 format (array with parameters)
	 * @param callbackMethod  The method to be executed on callback
	 * @param url             The url of the agent to be called
	 * @param method          The name of the method
	 * @param params          A JSONObject containing the parameter 
	 *                        values of the method
	 * @return response       A Confirmation message or error message in JSON 
	 *                        format
	 * @throws Exception 
	 * @throws JSONException 
	 */
	@Access(AccessType.UNAVAILABLE)
	final public void sendAsync(String url, String method, ObjectNode params,
			String callbackMethod) throws Exception {
		JSONRequest req = new JSONRequest(method, params);
		String callbackUrl = getUrl();
		req.setCallback(callbackUrl, callbackMethod);
		JSONRPC.send(url, req);
	}

	/**
	 * Get the full url of this agent, for example "http://mysite.com/agents/key"
	 * @return
	 * @throws Exception 
	 */
	@Access(AccessType.UNAVAILABLE)
	final public String getUrl() throws Exception {
		return context.getAgentUrl();
	}

	/**
	 * Get the Id of this agent
	 * @return
	 */
	@Access(AccessType.UNAVAILABLE)
	final public String getId() {
		return context.getAgentId();
	}
	
	/**
	 * Get a UUID from an url
	 * @param url
	 * @return
	 */
	@Access(AccessType.UNAVAILABLE)
	final public static String getUuid(String url) {
		return url.substring(url.lastIndexOf('/') + 1);
	}
	
	/**
	 * Get the classname of an agent from its url
	 * @param url
	 * @return
	 */
	final protected static String getClassName(String url) {
		String[] parts = url.split("/");
		if (parts.length > 1) {
			return parts[parts.length - 2];
		}
		
		return "";
	}
}
