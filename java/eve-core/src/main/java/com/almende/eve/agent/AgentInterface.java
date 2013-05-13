package com.almende.eve.agent;

import java.util.List;

import com.almende.eve.agent.annotation.Name;
import com.almende.eve.agent.annotation.Required;
import com.almende.eve.agent.annotation.Sender;
import com.fasterxml.jackson.databind.node.ObjectNode;

public interface AgentInterface {
	/**
	 * Retrieve the agents id
	 * 
	 * @return id
	 */
	public String getId();
	
	/**
	 * Retrieve the agents type (its simple class name)
	 * 
	 * @return version
	 */
	public String getType();
	
	/**
	 * Retrieve the agents version number
	 * 
	 * @return version
	 */
	public String getVersion();
	
	/**
	 * Retrieve a description of the agents functionality
	 * 
	 * @return description
	 */
	public String getDescription();
	
	/**
	 * Retrieve an array with the agents urls (can be one or multiple), and
	 * depends on the configured transport services.
	 * 
	 * @return urls
	 */
	public List<String> getUrls();
	
	/**
	 * Retrieve an JSON Array with the agents scheduled tasks
	 */
	public String getTasks();
	
	/**
	 * Internal method, implementing this method allows adding authorization to
	 * your agent.
	 * All methods annotated with AccessType.PRIVATE will only be called if this
	 * method returns true.
	 * The function_tag parameter can be used to check against
	 * 
	 * @Access(tag="foobar") annotation on the called method.
	 *                       ( e.g. add roles to methods )
	 * 
	 * @param senderId
	 * @param functionTag
	 * @return
	 */
	public boolean onAccess(String senderId, String functionTag);
	
	/**
	 * Retrieve a list with all the available methods.
	 * 
	 * @return methods
	 */
	public List<Object> getMethods();
	
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
	public String onSubscribe(@Name("event") String event,
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
	public void onUnsubscribe(
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
	public void onTrigger(@Name("url") String url,
			@Name("method") String method, @Name("params") ObjectNode params)
			throws Exception;
	
	/**
	 * Callback method for monitoring framework, doing the work for
	 * requester-side polling
	 * part of a connection.
	 * 
	 * @param monitorId
	 * @throws Exception
	 */
	public void doPoll(@Name("monitorId") String monitorId) throws Exception;
	
	/**
	 * Callback method for monitoring framework, doing the work for pushing data
	 * back to the requester.
	 * 
	 * @param pushParams
	 * @throws Exception
	 */
	public void doPush(@Name("params") ObjectNode pushParams) throws Exception;
	
	/**
	 * Callback method for the monitoring framework, doing the work for
	 * receiving pushed data in the requester.
	 * 
	 * @param result
	 * @param monitorId
	 */
	public void callbackPush(@Name("result") Object result,
			@Name("monitorId") String monitorId);
	
	/**
	 * Register a Push request as part of the monitoring framework. The sender
	 * in this case is the requesting agent, the receiver has the requested RPC
	 * method.
	 * 
	 * @param pushParams
	 * @param senderUrl
	 * @return
	 */
	public List<String> registerPush(@Name("params") ObjectNode pushParams,
			@Sender String senderUrl);
	
	/**
	 * Unregister a Push request, part of the monitoring framework.
	 * 
	 * @param id
	 */
	public void unregisterPush(@Name("pushId") String id);
}
