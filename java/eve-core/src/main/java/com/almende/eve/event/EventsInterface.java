package com.almende.eve.event;

import java.io.IOException;
import java.net.ProtocolException;
import java.net.URI;
import java.util.List;

import com.almende.eve.rpc.annotation.Access;
import com.almende.eve.rpc.annotation.AccessType;
import com.almende.eve.rpc.annotation.Name;
import com.almende.eve.rpc.annotation.Required;
import com.almende.eve.rpc.jsonrpc.JSONRPCException;
import com.fasterxml.jackson.databind.node.ObjectNode;

@Access(AccessType.PUBLIC)
public interface EventsInterface {
	
	
	/**
	 * Get existing even subscriptions for a given event.
	 * 
	 * @param event
	 * @return
	 */
	List<Callback> getSubscriptions(@Name("event") String event);
	
	/**
	 * Subscribe to an event. The provided callback url and method will be
	 * invoked when the event is triggered. The callback method is called
	 * with parameters:
	 * - {String} subscriptionId The id of the subscription
	 * - {String} event Name of the triggered event
	 * - {String} agent Url of the triggered agent
	 * - {Object} params Event specific parameters.
	 * See also Agent.trigger(event, params).
	 * 
	 * @param event
	 * @param callbackUrl
	 * @param callbackMethod
	 * @return subscriptionId
	 */
	String createSubscription(@Name("event") String event,
			@Name("callbackUrl") String callbackUrl,
			@Name("callbackMethod") String callbackMethod,
			@Required(false) @Name("callbackParams") ObjectNode params);
	
	/**
	 * Let an other agent unsubscribe from one of this agents events
	 * - If subscriptionId is provided, the subscription with this id will be
	 * deleted
	 * - If the parameter callbackUrl and optionally event and/or
	 * callbackMethod,
	 * all subscriptions with matching parameters will be deleted.
	 * (if only callbackUrl is provided, all subscriptions from this agent
	 * will be deleted).
	 * 
	 * @param subscriptionId
	 * @param event
	 * @param callbackUrl
	 * @param callbackMethod
	 */
	void deleteSubscription(
			@Required(false) @Name("subscriptionId") String subscriptionId,
			@Required(false) @Name("event") String event,
			@Required(false) @Name("callbackUrl") String callbackUrl,
			@Required(false) @Name("callbackMethod") String callbackMethod);
	
	/**
	 * Asynchronously trigger an event.
	 * the onTrigger method is called from a scheduled task, initiated in the
	 * method trigger
	 * 
	 * @param url
	 * @param method
	 * @param params
	 * @throws Exception
	 */
	void doTrigger(@Name("url") String url, @Name("method") String method,
			@Name("params") ObjectNode params) throws ProtocolException,
			JSONRPCException;
	
	/**
	 * Subscribe to an other agents event
	 * 
	 * @param url
	 * @param event
	 * @param callbackMethod
	 * @return subscriptionId
	 * @throws Exception
	 */
	@Access(AccessType.UNAVAILABLE)
	String subscribe(URI url, String event, String callbackMethod)
			throws ProtocolException, JSONRPCException;
	
	/**
	 * Subscribe to an other agents event
	 * 
	 * @param url
	 * @param event
	 * @param callbackMethod
	 * @return subscriptionId
	 * @throws Exception
	 */
	@Access(AccessType.UNAVAILABLE)
	String subscribe(URI url, String event, String callbackMethod,
			ObjectNode callbackParams) throws ProtocolException,
			JSONRPCException;
	
	/**
	 * Unsubscribe from an other agents event
	 * 
	 * @param url
	 * @param subscriptionId
	 * @throws Exception
	 */
	@Access(AccessType.UNAVAILABLE)
	void unsubscribe(URI url, String subscriptionId) throws ProtocolException,
			JSONRPCException;
	
	/**
	 * Unsubscribe from an other agents event
	 * 
	 * @param url
	 * @param event
	 * @param callbackMethod
	 * @throws Exception
	 */
	@Access(AccessType.UNAVAILABLE)
	void unsubscribe(URI url, String event, String callbackMethod)
			throws ProtocolException, JSONRPCException;
	
	/**
	 * Trigger an event
	 * 
	 * @param event
	 * @throws Exception
	 * @throws JSONRPCException
	 */
	@Access(AccessType.UNAVAILABLE)
	void trigger(@Name("event") String event) throws IOException;
	
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
	void trigger(@Name("event") String event, @Name("params") Object params)
			throws IOException;
	
}
