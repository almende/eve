package com.almende.eve.event;

import java.io.IOException;
import java.net.ProtocolException;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import com.almende.eve.agent.Agent;
import com.almende.eve.rpc.annotation.Access;
import com.almende.eve.rpc.annotation.AccessType;
import com.almende.eve.rpc.annotation.Name;
import com.almende.eve.rpc.annotation.Optional;
import com.almende.eve.rpc.jsonrpc.JSONRPCException;
import com.almende.eve.rpc.jsonrpc.JSONRequest;
import com.almende.eve.rpc.jsonrpc.jackson.JOM;
import com.almende.eve.state.TypedKey;
import com.almende.util.uuid.UUID;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

public class EventsFactory implements EventsInterface {
	private Agent													myAgent			= null;
	private static final TypedKey<HashMap<String, List<Callback>>>	SUBSCRIPTIONS	= new TypedKey<HashMap<String, List<Callback>>>(
																							"subscriptions") {
																					};
	private static final String										EVENT			= "event";
	
	public EventsFactory(Agent agent) {
		this.myAgent = agent;
	}
	
	/**
	 * Retrieve the list with subscriptions on given event.
	 * If there are no subscriptions for this event, an empty list is returned
	 * 
	 * @param event
	 * @return
	 */
	public List<Callback> getSubscriptions(String event) {
		Map<String, List<Callback>> allSubscriptions = myAgent.getState().get(
				SUBSCRIPTIONS);
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
	 * 
	 * @param event
	 * @param subscriptions
	 */
	private void putSubscriptions(String event, List<Callback> subscriptions) {
		
		Map<String, List<Callback>> allSubscriptions = myAgent.getState().get(
				SUBSCRIPTIONS);
		
		HashMap<String, List<Callback>> newSubscriptions = new HashMap<String, List<Callback>>();
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
	 * Subscribe to an other agents event
	 * 
	 * @param url
	 * @param event
	 * @param callbackMethod
	 * @return subscriptionId
	 * @throws JSONRPCException
	 * @throws ProtocolException
	 * @throws Exception
	 */
	public String subscribe(URI url, String event, String callbackMethod)
			throws IOException, JSONRPCException {
		return subscribe(url, event, callbackMethod, null);
	}
	
	/**
	 * Subscribe to an other agents event
	 * 
	 * @param url
	 * @param event
	 * @param callbackMethod
	 * @return subscriptionId
	 * @throws JSONRPCException
	 * @throws ProtocolException
	 * @throws Exception
	 */
	public String subscribe(URI url, String event, String callbackMethod,
			ObjectNode callbackParams) throws IOException,
			JSONRPCException {
		String method = "event.createSubscription";
		ObjectNode params = JOM.createObjectNode();
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
	 * Unsubscribe from an other agents event
	 * 
	 * @param url
	 * @param subscriptionId
	 * @throws JSONRPCException
	 * @throws ProtocolException
	 */
	public void unsubscribe(URI url, String subscriptionId)
			throws IOException, JSONRPCException {
		String method = "event.deleteSubscription";
		ObjectNode params = JOM.createObjectNode();
		params.put("subscriptionId", subscriptionId);
		myAgent.sendAsync(url, method, params);
	}
	
	/**
	 * Unsubscribe from an other agents event
	 * 
	 * @param url
	 * @param event
	 * @param callbackMethod
	 * @throws JSONRPCException
	 * @throws ProtocolException
	 * @throws Exception
	 */
	public void unsubscribe(URI url, String event, String callbackMethod)
			throws IOException, JSONRPCException {
		String method = "event.deleteSubscription";
		ObjectNode params = JOM.createObjectNode();
		params.put(EVENT, event);
		params.put("callbackUrl", myAgent.getFirstUrl().toASCIIString());
		params.put("callbackMethod", callbackMethod);
		myAgent.sendAsync(url, method, params);
	}
	
	/**
	 * Trigger an event
	 * 
	 * @param event
	 * @throws IOException
	 * @throws JsonProcessingException
	 */
	@Access(AccessType.UNAVAILABLE)
	public final void trigger(@Name(EVENT) String event) throws IOException {
		trigger(event, null);
	}
	
	/**
	 * Trigger an event
	 * 
	 * @param event
	 * @param params
	 *            An ObjectNode, Map, or POJO
	 * @throws IOException
	 * @throws JsonProcessingException
	 */
	@Access(AccessType.UNAVAILABLE)
	public final void trigger(@Name(EVENT) String event,
			@Name("params") Object params) throws IOException {
		// TODO: user first url is very dangerous! can cause a mismatch
		String url = myAgent.getFirstUrl().toASCIIString();
		List<Callback> subscriptions = new ArrayList<Callback>();
		
		if (event.equals("*")) {
			throw new IllegalArgumentException("Cannot trigger * event");
		}

		// retrieve subscriptions from the event
		List<Callback> valueEvent = getSubscriptions(event);
		subscriptions.addAll(valueEvent);
		
		// retrieve subscriptions from the all event "*"
		List<Callback> valueAll = getSubscriptions("*");
		subscriptions.addAll(valueAll);
		
		ObjectNode baseParams = JOM.createObjectNode();
		if (params != null) {
			if (params instanceof JsonNode) {
				baseParams.put("params", (ObjectNode) params);
			} else {
				ObjectNode jsonParams = JOM.getInstance().convertValue(params,
						ObjectNode.class);
				baseParams.put("params", jsonParams);
			}
		}
		baseParams.put("agent", url);
		baseParams.put(EVENT, event);
		
		for (Callback subscription : subscriptions) {
			// create a task to send this trigger.
			// This way, it is sent asynchronously and cannot block this
			// trigger method
			ObjectNode triggerParams = baseParams.deepCopy();
			
			triggerParams.put("subscriptionId", subscription.getId());
			
			ObjectNode taskParams = JOM.createObjectNode();
			taskParams.put("url", subscription.getUrl());
			taskParams.put("method", subscription.getMethod());
			
			if (subscription.getParams() != null
					&& !subscription.getParams().equals("null")) {
				ObjectNode parms = (ObjectNode) JOM.getInstance().readTree(
						subscription.getParams());
				triggerParams = (ObjectNode) parms.putAll(triggerParams);
			}
			taskParams.put("params", triggerParams);
			JSONRequest request = new JSONRequest("event.doTrigger", taskParams);
			long delay = 0;
			myAgent.getScheduler().createTask(request, delay);
		}
	}
	
	@Access(AccessType.PUBLIC)
	public final String createSubscription(@Name(EVENT) String event,
			@Name("callbackUrl") String callbackUrl,
			@Name("callbackMethod") String callbackMethod,
			@Optional @Name("callbackParams") ObjectNode params) {
		
		// check if callback already existed, returning existing instead
		List<Callback> subscriptions = getSubscriptions(event);
		for (Callback subscription : subscriptions) {
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
		String subscriptionId = new UUID().toString();
		Callback callback = new Callback(subscriptionId, callbackUrl,
				callbackMethod, params);
		
		// Callback didn't exist, store new callback.
		subscriptions.add(callback);
		
		// store the subscriptions
		// FIXME: Race condition on subscriptions!
		putSubscriptions(event, subscriptions);
		
		return subscriptionId;
	}
	
	@Access(AccessType.PUBLIC)
	public final void deleteSubscription(
			@Optional @Name("subscriptionId") String subscriptionId,
			@Optional @Name(EVENT) String event,
			@Optional @Name("callbackUrl") String callbackUrl,
			@Optional @Name("callbackMethod") String callbackMethod) {
		Map<String, List<Callback>> allSubscriptions = myAgent.getState().get(
				SUBSCRIPTIONS);
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
	 * delayed behaviour
	 * 
	 * @param url
	 * @param method
	 * @param params
	 * @throws JSONRPCException
	 * @throws ProtocolException
	 */
	@Access(AccessType.SELF)
	public final void doTrigger(@Name("url") String url,
			@Name("method") String method, @Name("params") ObjectNode params)
			throws IOException, JSONRPCException {
		// TODO: send the trigger as a JSON-RPC 2.0 Notification
		myAgent.sendAsync(URI.create(url), method, params);
	}
	
	@Access(AccessType.SELF)
	public ObjectNode getSubscriptionStats() {
		ObjectNode result = JOM.createObjectNode();
		Map<String, List<Callback>> allSubscriptions = myAgent.getState().get(
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
