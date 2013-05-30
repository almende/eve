package com.almende.eve.event;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.UUID;

import com.almende.eve.agent.Agent;
import com.almende.eve.rpc.annotation.Access;
import com.almende.eve.rpc.annotation.AccessType;
import com.almende.eve.rpc.annotation.Name;
import com.almende.eve.rpc.annotation.Required;
import com.almende.eve.rpc.jsonrpc.JSONRPCException;
import com.almende.eve.rpc.jsonrpc.JSONRequest;
import com.almende.eve.rpc.jsonrpc.jackson.JOM;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

public class EventsFactory implements EventsInterface {
	Agent	myAgent	= null;
	
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
	@SuppressWarnings("unchecked")
	private List<Callback> getSubscriptions(String event) {
		Map<String, List<Callback>> allSubscriptions = (Map<String, List<Callback>>) myAgent
				.getState().get("subscriptions");
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
	@SuppressWarnings("unchecked")
	private void putSubscriptions(String event, List<Callback> subscriptions) {
		
		HashMap<String, List<Callback>> allSubscriptions = (HashMap<String, List<Callback>>) myAgent
				.getState().get("subscriptions");
		
		HashMap<String, List<Callback>> newSubscriptions = new HashMap<String, List<Callback>>();
		if (allSubscriptions != null){
			newSubscriptions.putAll(allSubscriptions);
		}
		newSubscriptions.put(event, subscriptions);
		if (!myAgent.getState().putIfUnchanged("subscriptions", newSubscriptions, allSubscriptions)){
			//Recursive retry.
			putSubscriptions(event,subscriptions);
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
	 * @throws Exception
	 */
	public String subscribe(String url, String event, String callbackMethod)
			throws Exception {
		return subscribe(url, event, callbackMethod, null);
	}
	
	/**
	 * Subscribe to an other agents event
	 * 
	 * @param url
	 * @param event
	 * @param callbackMethod
	 * @return subscriptionId
	 * @throws Exception
	 */
	public String subscribe(String url, String event, String callbackMethod,
			ObjectNode callbackParams) throws Exception {
		String method = "event.createSubscription";
		ObjectNode params = JOM.createObjectNode();
		params.put("event", event);
		params.put("callbackUrl", myAgent.getFirstUrl());
		params.put("callbackMethod", callbackMethod);
		if (callbackParams != null) {
			params.put("callbackParams", callbackParams);
		}
		
		// TODO: store the agents subscriptions locally
		return myAgent.send(url, method, params, JOM.getTypeFactory().constructSimpleType(String.class, new JavaType[0]));
	}
	
	/**
	 * Unsubscribe from an other agents event
	 * 
	 * @param url
	 * @param subscriptionId
	 * @throws Exception
	 */
	public void unsubscribe(String url, String subscriptionId) throws Exception {
		String method = "event.deleteSubscription";
		ObjectNode params = JOM.createObjectNode();
		params.put("subscriptionId", subscriptionId);
		myAgent.send(url, method, params);
	}
	
	/**
	 * Unsubscribe from an other agents event
	 * 
	 * @param url
	 * @param event
	 * @param callbackMethod
	 * @throws Exception
	 */
	public void unsubscribe(String url, String event, String callbackMethod)
			throws Exception {
		String method = "event.deleteSubscription";
		ObjectNode params = JOM.createObjectNode();
		params.put("event", event);
		params.put("callbackUrl", myAgent.getFirstUrl());
		params.put("callbackMethod", callbackMethod);
		myAgent.send(url, method, params);
	}
	
	/**
	 * Trigger an event
	 * 
	 * @param event
	 
	 * @throws Exception
	 * @throws JSONRPCException
	 */
	@Access(AccessType.UNAVAILABLE)
	final public void trigger(@Name("event") String event) throws Exception {
		trigger(event,null);
	}
	
	/**
	 * Trigger an event
	 * 
	 * @param event
	 * @param params
	 *            An ObjectNode, Map, or POJO
	 * @throws Exception
	 * @throws JSONRPCException
	 */
	@Access(AccessType.UNAVAILABLE)
	final public void trigger(@Name("event") String event,
			@Name("params") Object params) throws Exception {
		// TODO: user first url is very dangerous! can cause a mismatch
		String url = myAgent.getFirstUrl();
		List<Callback> subscriptions = new ArrayList<Callback>();
		
		if (event.equals("*")) {
			throw new Exception("Cannot trigger * event");
		}
		
		// send a trigger to the agent factory
		myAgent.getAgentFactory().getEventLogger()
				.log(myAgent.getId(), event, params);
		
		// retrieve subscriptions from the event
		List<Callback> valueEvent = getSubscriptions(event);
		subscriptions.addAll(valueEvent);
		
		// retrieve subscriptions from the all event "*"
		List<Callback> valueAll = getSubscriptions("*");
		subscriptions.addAll(valueAll);
		
		ObjectNode baseParams = JOM.createObjectNode();
		if (params != null){
			if (params instanceof JsonNode) {
				baseParams.put("triggerParams",(ObjectNode) params);
			} else {
				ObjectNode jsonParams = JOM.getInstance().convertValue(params,
						ObjectNode.class);
				baseParams.put("triggerParams",jsonParams);
			}
		}
		baseParams.put("agent", url);
		baseParams.put("event", event);
		
		for (Callback subscription : subscriptions) {
			// create a task to send this trigger.
			// This way, it is sent asynchronously and cannot block this
			// trigger method
			ObjectNode triggerParams = baseParams.deepCopy();
			
			triggerParams.put("subscriptionId", subscription.id);
			
			ObjectNode taskParams = JOM.createObjectNode();
			taskParams.put("url", subscription.url);
			taskParams.put("method", subscription.method);
			
			if (subscription.params != null) {
				ObjectNode parms = (ObjectNode) JOM.getInstance()
						.readTree(subscription.params);
				triggerParams =  (ObjectNode) parms.putAll(triggerParams);
			}
			taskParams.put("params", triggerParams);
			JSONRequest request = new JSONRequest("event.doTrigger", taskParams);
			long delay = 0;
			myAgent.getScheduler().createTask(request, delay);
		}
	}
	
	@Access(AccessType.PUBLIC)
	final public String createSubscription(@Name("event") String event,
			@Name("callbackUrl") String callbackUrl,
			@Name("callbackMethod") String callbackMethod,
			@Required(false) @Name("callbackParams") ObjectNode params) {
		List<Callback> subscriptions = getSubscriptions(event);
		for (Callback subscription : subscriptions) {
			if (subscription.url == null || subscription.method == null) {
				continue;
			}
			if (subscription.url.equals(callbackUrl)
					&& subscription.method.equals(callbackMethod)
					&& ((subscription.params == null && params == null) || subscription.params != null)
					&& subscription.params.equals(params)) {
				// The callback already exists. do not duplicate it
				return subscription.id;
			}
		}
		
		// the callback does not yet exist. create it and store it
		String subscriptionId = UUID.randomUUID().toString();
		Callback callback = new Callback(subscriptionId, callbackUrl,
				callbackMethod, params);
		subscriptions.add(callback);
		
		// store the subscriptions
		putSubscriptions(event, subscriptions);//FIXME: Race condition on subscriptions!
		
		return subscriptionId;
	}
	
	@Access(AccessType.PUBLIC)
	final public void deleteSubscription(
			@Required(false) @Name("subscriptionId") String subscriptionId,
			@Required(false) @Name("event") String event,
			@Required(false) @Name("callbackUrl") String callbackUrl,
			@Required(false) @Name("callbackMethod") String callbackMethod) {
			@SuppressWarnings("unchecked")
		HashMap<String, List<Callback>> allSubscriptions = (HashMap<String, List<Callback>>) myAgent.getState()
				.get("subscriptions");
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
							&& subscriptionId.equals(subscription.id)) {
						// callback with given subscriptionId is found
						matched = true;
					} else if (callbackUrl != null
							&& callbackUrl.equals(subscription.url)) {
						if ((callbackMethod == null || callbackMethod
								.equals(subscription.method))
								&& (event == null || event
										.equals(subscriptionEvent))) {
							// callback with matching properties is found
							matched = true;
						}
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
		myAgent.getState().put("subscriptions", allSubscriptions); //TODO: Race condition!
	}
	
	/**
	 * Work-method for trigger: called by scheduler for asynchronous and/or delayed behaviour 
	 * @param url
	 * @param method
	 * @param params
	 * @throws Exception
	 */
	@Access(AccessType.PUBLIC)
	final public void doTrigger(@Name("url") String url,
			@Name("method") String method, @Name("params") ObjectNode params)
			throws Exception {
		// TODO: send the trigger as a JSON-RPC 2.0 Notification
		myAgent.send(url, method, params);
	}
}
