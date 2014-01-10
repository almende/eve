/*
 * Copyright: Almende B.V. (2014), Rotterdam, The Netherlands
 * License: The Apache Software License, Version 2.0
 */
package com.almende.eve.event;

import java.io.IOException;
import java.net.URI;
import java.util.List;

import com.almende.eve.rpc.annotation.Access;
import com.almende.eve.rpc.annotation.AccessType;
import com.almende.eve.rpc.annotation.Name;
import com.almende.eve.rpc.annotation.Optional;
import com.almende.eve.rpc.jsonrpc.JSONRPCException;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * The Interface EventsInterface.
 */
@Access(AccessType.PUBLIC)
public interface EventsInterface {
	
	/**
	 * Get existing even subscriptions for a given event.
	 * 
	 * @param event
	 *            the event
	 * @return the subscriptions
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
	 *            the event
	 * @param callbackUrl
	 *            the callback url
	 * @param callbackMethod
	 *            the callback method
	 * @param params
	 *            the params
	 * @return subscriptionId
	 */
	String createSubscription(@Name("event") String event,
			@Name("callbackUrl") String callbackUrl,
			@Name("callbackMethod") String callbackMethod,
			@Optional @Name("callbackParams") ObjectNode params);
	
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
	 *            the subscription id
	 * @param event
	 *            the event
	 * @param callbackUrl
	 *            the callback url
	 * @param callbackMethod
	 *            the callback method
	 */
	void deleteSubscription(
			@Optional @Name("subscriptionId") String subscriptionId,
			@Optional @Name("event") String event,
			@Optional @Name("callbackUrl") String callbackUrl,
			@Optional @Name("callbackMethod") String callbackMethod);
	
	/**
	 * Asynchronously trigger an event.
	 * the onTrigger method is called from a scheduled task, initiated in the
	 * method trigger
	 * 
	 * @param url
	 *            the url
	 * @param method
	 *            the method
	 * @param params
	 *            the params
	 * @throws IOException
	 *             Signals that an I/O exception has occurred.
	 * @throws JSONRPCException
	 *             the jSONRPC exception
	 */
	void doTrigger(@Name("url") String url, @Name("method") String method,
			@Name("params") ObjectNode params) throws IOException,
			JSONRPCException;
	
	/**
	 * Subscribe to an other agents event.
	 * 
	 * @param url
	 *            the url
	 * @param event
	 *            the event
	 * @param callbackMethod
	 *            the callback method
	 * @return subscriptionId
	 * @throws IOException
	 *             Signals that an I/O exception has occurred.
	 * @throws JSONRPCException
	 *             the jSONRPC exception
	 */
	@Access(AccessType.UNAVAILABLE)
	String subscribe(URI url, String event, String callbackMethod)
			throws IOException, JSONRPCException;
	
	/**
	 * Subscribe to an other agents event.
	 * 
	 * @param url
	 *            the url
	 * @param event
	 *            the event
	 * @param callbackMethod
	 *            the callback method
	 * @param callbackParams
	 *            the callback params
	 * @return subscriptionId
	 * @throws IOException
	 *             Signals that an I/O exception has occurred.
	 * @throws JSONRPCException
	 *             the jSONRPC exception
	 */
	@Access(AccessType.UNAVAILABLE)
	String subscribe(URI url, String event, String callbackMethod,
			ObjectNode callbackParams) throws IOException, JSONRPCException;
	
	/**
	 * Unsubscribe from an other agents event.
	 * 
	 * @param url
	 *            the url
	 * @param subscriptionId
	 *            the subscription id
	 * @throws IOException
	 *             Signals that an I/O exception has occurred.
	 * @throws JSONRPCException
	 *             the jSONRPC exception
	 */
	@Access(AccessType.UNAVAILABLE)
	void unsubscribe(URI url, String subscriptionId) throws IOException,
			JSONRPCException;
	
	/**
	 * Unsubscribe from an other agents event.
	 * 
	 * @param url
	 *            the url
	 * @param event
	 *            the event
	 * @param callbackMethod
	 *            the callback method
	 * @throws IOException
	 *             Signals that an I/O exception has occurred.
	 * @throws JSONRPCException
	 *             the jSONRPC exception
	 */
	@Access(AccessType.UNAVAILABLE)
	void unsubscribe(URI url, String event, String callbackMethod)
			throws IOException, JSONRPCException;
	
	/**
	 * Trigger an event.
	 * 
	 * @param event
	 *            the event
	 * @throws IOException
	 *             Signals that an I/O exception has occurred.
	 */
	@Access(AccessType.UNAVAILABLE)
	void trigger(@Name("event") String event) throws IOException;
	
	/**
	 * Trigger an event.
	 * 
	 * @param event
	 *            the event
	 * @param params
	 *            An ObjectNode, Map, or POJO
	 * @throws IOException
	 *             Signals that an I/O exception has occurred.
	 */
	@Access(AccessType.UNAVAILABLE)
	void trigger(@Name("event") String event, @Name("params") Object params)
			throws IOException;
	
}
