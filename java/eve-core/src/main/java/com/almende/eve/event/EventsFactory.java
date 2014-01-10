/*
 * Copyright: Almende B.V. (2014), Rotterdam, The Netherlands
 * License: The Apache Software License, Version 2.0
 */
package com.almende.eve.event;

import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import com.almende.eve.agent.AgentInterface;
import com.almende.eve.rpc.annotation.Access;
import com.almende.eve.rpc.annotation.AccessType;
import com.almende.eve.rpc.annotation.Name;
import com.almende.eve.rpc.annotation.Optional;
import com.almende.eve.rpc.jsonrpc.JSONRPCException;
import com.almende.eve.rpc.jsonrpc.JSONRequest;
import com.almende.eve.rpc.jsonrpc.jackson.JOM;
import com.almende.eve.state.TypedKey;
import com.almende.util.uuid.UUID;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * A factory for creating Events objects.
 */
public class EventsFactory implements EventsInterface {
	private AgentInterface											myAgent			= null;
	private static final TypedKey<HashMap<String, List<Callback>>>	SUBSCRIPTIONS	= new TypedKey<HashMap<String, List<Callback>>>(
																							"subscriptions") {
																					};
	private static final String										EVENT			= "event";
	
	/**
	 * Instantiates a new events factory.
	 *
	 * @param agent the agent
	 */
	public EventsFactory(final AgentInterface agent) {
		myAgent = agent;
	}
	
	/**
	 * Retrieve the list with subscriptions on given event.
	 * If there are no subscriptions for this event, an empty list is returned
	 *
	 * @param event the event
	 * @return the subscriptions
	 */
	@Override
	public List<Callback> getSubscriptions(final String event) {
		final Map<String, List<Callback>> allSubscriptions = myAgent.getState().get(
				SUBSCRIPTIONS);
		if (allSubscriptions != null) {
			final List<Callback> eventSubscriptions = allSubscriptions.get(event);
			if (eventSubscriptions != null) {
				return eventSubscriptions;
			}
		}
		
		return new ArrayList<Callback>();
	}
	
	/**
	 * Store a list with subscriptions for an event.
	 *
	 * @param event the event
	 * @param subscriptions the subscriptions
	 */
	private void putSubscriptions(final String event, final List<Callback> subscriptions) {
		
		final Map<String, List<Callback>> allSubscriptions = myAgent.getState().get(
				SUBSCRIPTIONS);
		
		final HashMap<String, List<Callback>> newSubscriptions = new HashMap<String, List<Callback>>();
		if (allSubscriptions != null) {
			newSubscriptions.putAll(allSubscriptions);
		}
		newSubscriptions.put(event, subscriptions);
		if (!myAgent.getState().putIfUnchanged(SUBSCRIPTIONS.getKey(),
				newSubscriptions, allSubscriptions)) {
			// Recursive retry.
			putSubscriptions(event, subscriptions);
			return;
		}
	}
	
	/**
	 * Subscribe to an other agents event.
	 *
	 * @param url the url
	 * @param event the event
	 * @param callbackMethod the callback method
	 * @return subscriptionId
	 * @throws IOException Signals that an I/O exception has occurred.
	 * @throws JSONRPCException the jSONRPC exception
	 */
	@Override
	public String subscribe(final URI url, final String event, final String callbackMethod)
			throws IOException, JSONRPCException {
		return subscribe(url, event, callbackMethod, null);
	}
	
	/**
	 * Subscribe to an other agents event.
	 *
	 * @param url the url
	 * @param event the event
	 * @param callbackMethod the callback method
	 * @param callbackParams the callback params
	 * @return subscriptionId
	 * @throws IOException Signals that an I/O exception has occurred.
	 * @throws JSONRPCException the jSONRPC exception
	 */
	@Override
	public String subscribe(final URI url, final String event, final String callbackMethod,
			final ObjectNode callbackParams) throws IOException, JSONRPCException {
		final String method = "event.createSubscription";
		final ObjectNode params = JOM.createObjectNode();
		params.put(EVENT, event);
		params.put("callbackUrl", myAgent.getFirstUrl().toASCIIString());
		params.put("callbackMethod", callbackMethod);
		if (callbackParams != null) {
			params.put("callbackParams", callbackParams);
		}
		
		// TODO: store the agents subscriptions locally
		return myAgent.send(url, method, params, String.class);
	}
	
	/**
	 * Unsubscribe from an other agents event.
	 *
	 * @param url the url
	 * @param subscriptionId the subscription id
	 * @throws IOException Signals that an I/O exception has occurred.
	 * @throws JSONRPCException the jSONRPC exception
	 */
	@Override
	public void unsubscribe(final URI url, final String subscriptionId) throws IOException,
			JSONRPCException {
		final String method = "event.deleteSubscription";
		final ObjectNode params = JOM.createObjectNode();
		params.put("subscriptionId", subscriptionId);
		myAgent.sendAsync(url, method, params);
	}
	
	/**
	 * Unsubscribe from an other agents event.
	 *
	 * @param url the url
	 * @param event the event
	 * @param callbackMethod the callback method
	 * @throws IOException Signals that an I/O exception has occurred.
	 * @throws JSONRPCException the jSONRPC exception
	 */
	@Override
	public void unsubscribe(final URI url, final String event, final String callbackMethod)
			throws IOException, JSONRPCException {
		final String method = "event.deleteSubscription";
		final ObjectNode params = JOM.createObjectNode();
		params.put(EVENT, event);
		params.put("callbackUrl", myAgent.getFirstUrl().toASCIIString());
		params.put("callbackMethod", callbackMethod);
		myAgent.sendAsync(url, method, params);
	}
	
	/**
	 * Trigger an event.
	 *
	 * @param event the event
	 * @throws IOException Signals that an I/O exception has occurred.
	 */
	@Override
	@Access(AccessType.UNAVAILABLE)
	public final void trigger(@Name(EVENT) final String event) throws IOException {
		trigger(event, null);
	}
	
	/**
	 * Trigger an event.
	 *
	 * @param event the event
	 * @param params An ObjectNode, Map, or POJO
	 * @throws IOException Signals that an I/O exception has occurred.
	 */
	@Override
	@Access(AccessType.UNAVAILABLE)
	public final void trigger(@Name(EVENT) final String event,
			@Name("params") final Object params) throws IOException {
		// TODO: user first url is very dangerous! can cause a mismatch
		final String url = myAgent.getFirstUrl().toASCIIString();
		final List<Callback> subscriptions = new ArrayList<Callback>();
		
		if (event.equals("*")) {
			throw new IllegalArgumentException("Cannot trigger * event");
		}
		
		// retrieve subscriptions from the event
		final List<Callback> valueEvent = getSubscriptions(event);
		subscriptions.addAll(valueEvent);
		
		// retrieve subscriptions from the all event "*"
		final List<Callback> valueAll = getSubscriptions("*");
		subscriptions.addAll(valueAll);
		
		final ObjectNode baseParams = JOM.createObjectNode();
		if (params != null) {
			if (params instanceof JsonNode) {
				baseParams.put("params", (ObjectNode) params);
			} else {
				final ObjectNode jsonParams = JOM.getInstance().convertValue(params,
						ObjectNode.class);
				baseParams.put("params", jsonParams);
			}
		}
		baseParams.put("agent", url);
		baseParams.put(EVENT, event);
		
		for (final Callback subscription : subscriptions) {
			// create a task to send this trigger.
			// This way, it is sent asynchronously and cannot block this
			// trigger method
			ObjectNode triggerParams = baseParams.deepCopy();
			
			triggerParams.put("subscriptionId", subscription.getId());
			
			final ObjectNode taskParams = JOM.createObjectNode();
			taskParams.put("url", subscription.getUrl());
			taskParams.put("method", subscription.getMethod());
			
			if (subscription.getParams() != null
					&& !subscription.getParams().equals("null")) {
				final ObjectNode parms = (ObjectNode) JOM.getInstance().readTree(
						subscription.getParams());
				triggerParams = (ObjectNode) parms.putAll(triggerParams);
			}
			taskParams.put("params", triggerParams);
			final JSONRequest request = new JSONRequest("event.doTrigger", taskParams);
			final long delay = 0;
			myAgent.getScheduler().createTask(request, delay);
		}
	}
	
	/* (non-Javadoc)
	 * @see com.almende.eve.event.EventsInterface#createSubscription(java.lang.String, java.lang.String, java.lang.String, com.fasterxml.jackson.databind.node.ObjectNode)
	 */
	@Override
	@Access(AccessType.PUBLIC)
	public final String createSubscription(@Name(EVENT) final String event,
			@Name("callbackUrl") final String callbackUrl,
			@Name("callbackMethod") final String callbackMethod,
			@Optional @Name("callbackParams") final ObjectNode params) {
		
		// check if callback already existed, returning existing instead
		final List<Callback> subscriptions = getSubscriptions(event);
		for (final Callback subscription : subscriptions) {
			if (subscription == null || subscription.getUrl() == null
					|| subscription.getMethod() == null) {
				continue;
			}
			if (!subscription.getUrl().equals(callbackUrl)
					|| !subscription.getMethod().equals(callbackMethod)) {
				continue;
			}
			if (subscription.getParams() == null) {
				if (params != null) {
					continue;
				}
			} else {
				if (!subscription.getParams().equals(params)) {
					continue;
				}
			}
			// Callback already exists, returning existing callbackId.
			return subscription.getId();
		}
		// create new callback
		final String subscriptionId = new UUID().toString();
		final Callback callback = new Callback(subscriptionId, callbackUrl,
				callbackMethod, params);
		
		// Callback didn't exist, store new callback.
		subscriptions.add(callback);
		
		// store the subscriptions
		// FIXME: Race condition on subscriptions!
		putSubscriptions(event, subscriptions);
		
		return subscriptionId;
	}
	
	/* (non-Javadoc)
	 * @see com.almende.eve.event.EventsInterface#deleteSubscription(java.lang.String, java.lang.String, java.lang.String, java.lang.String)
	 */
	@Override
	@Access(AccessType.PUBLIC)
	public final void deleteSubscription(
			@Optional @Name("subscriptionId") final String subscriptionId,
			@Optional @Name(EVENT) final String event,
			@Optional @Name("callbackUrl") final String callbackUrl,
			@Optional @Name("callbackMethod") final String callbackMethod) {
		final Map<String, List<Callback>> allSubscriptions = myAgent.getState().get(
				SUBSCRIPTIONS);
		if (allSubscriptions == null) {
			return;
		}
		
		for (final Entry<String, List<Callback>> entry : allSubscriptions.entrySet()) {
			final String subscriptionEvent = entry.getKey();
			final List<Callback> subscriptions = entry.getValue();
			if (subscriptions != null) {
				int i = 0;
				while (i < subscriptions.size()) {
					final Callback subscription = subscriptions.get(i);
					boolean matched = false;
					if (subscriptionId != null
							&& subscriptionId.equals(subscription.getId())) {
						// callback with given subscriptionId is found
						matched = true;
					} else if (callbackUrl != null
							&& callbackUrl.equals(subscription.getUrl())
							&& (callbackMethod == null || callbackMethod
									.equals(subscription.getMethod()))
							&& (event == null || event
									.equals(subscriptionEvent))) {
						// callback with matching properties is found
						matched = true;
					}
					
					if (matched) {
						subscriptions.remove(i);
					} else {
						i++;
					}
				}
			}
			// TODO: cleanup event list when empty
		}
		
		// store state again
		// TODO: Race condition on state
		myAgent.getState().put(SUBSCRIPTIONS.getKey(), allSubscriptions);
	}
	
	/**
	 * Work-method for trigger: called by scheduler for asynchronous and/or
	 * delayed behaviour.
	 *
	 * @param url the url
	 * @param method the method
	 * @param params the params
	 * @throws IOException Signals that an I/O exception has occurred.
	 * @throws JSONRPCException the jSONRPC exception
	 */
	@Override
	@Access(AccessType.SELF)
	public final void doTrigger(@Name("url") final String url,
			@Name("method") final String method, @Name("params") final ObjectNode params)
			throws IOException, JSONRPCException {
		// TODO: send the trigger as a JSON-RPC 2.0 Notification
		myAgent.sendAsync(URI.create(url), method, params);
	}
	
	/**
	 * Gets the subscription stats.
	 *
	 * @return the subscription stats
	 */
	@Access(AccessType.SELF)
	public ObjectNode getSubscriptionStats() {
		final ObjectNode result = JOM.createObjectNode();
		final Map<String, List<Callback>> allSubscriptions = myAgent.getState().get(
				SUBSCRIPTIONS);
		if (allSubscriptions != null) {
			result.put("nofSubscriptions", allSubscriptions.values().size());
			result.put("nofEvents", allSubscriptions.keySet().size());
		} else {
			result.put("nofSubscriptions", 0);
			result.put("nofEvents", 0);
		}
		return result;
	}
}
