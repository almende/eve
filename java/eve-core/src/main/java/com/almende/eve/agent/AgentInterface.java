package com.almende.eve.agent;

import java.util.List;

import com.almende.eve.agent.annotation.Name;
import com.almende.eve.agent.annotation.Required;
import com.almende.eve.agent.annotation.Sender;

public interface AgentInterface {
	/**
	 * Retrieve the agents id
	 * @return id
	 */
	public String getId();
	
	/**
	 * Retrieve the agents type (its simple class name)
	 * @return version
	 */
	public String getType();
	
	/**
	 * Retrieve the agents version number
	 * @return version
	 */
	public String getVersion();
	
	/**
	 * Retrieve a description of the agents functionality
	 * @return description
	 */
	public String getDescription();
	
	/**
	 * Retrieve an array with the agents urls (can be one or multiple), and 
	 * depends on the configured transport services.
	 * @return urls
	 */
	public List<String> getUrls();

	/**
	 * Internal method, implementing this method allows adding authorization to your agent. 
	 * All methods annotated with AccessType.PRIVATE will only be called if this method returns true.
	 * The function_tag parameter can be used to check against @Access(tag="foobar") annotation on the called method.
	 * ( e.g. add roles to methods )
	 * 
	 * @param senderId
	 * @param function_tag
	 * @return
	 */
	public boolean onAccess(@Sender String senderId, String function_tag);
	
	/**
	 * Retrieve a list with all the available methods.
	 * @param asJSON   If true, result is in a JSON format easily parsable by
	 *                 a machine. If false (default), the returned list
	 *                 contains human readable strings.
	 * @return methods
	 */
	public List<Object> getMethods(
			@Name("asJSON") @Required(false) Boolean asJSON);
	
	/**
	 * Subscribe to an event. The provided callback url and method will be 
	 * invoked when the event is triggered. The callback method is called
	 * with parameters:
	 * - {String} subscriptionId  The id of the subscription
	 * - {String} event           Name of the triggered event
	 * - {String} agent           Url of the triggered agent
	 * - {Object} params          Event specific parameters.
	 * See also Agent.trigger(event, params).
	 * @param event
	 * @param callbackUrl
	 * @param callbackMethod
	 * @return subscriptionId
	 */
	public String onSubscribe (
			@Name("event") String event, 
			@Name("callbackUrl") String callbackUrl, 
			@Name("callbackMethod") String callbackMethod);
	
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
	public void onUnsubscribe (
			@Required(false) @Name("subscriptionId") String subscriptionId, 
			@Required(false) @Name("event") String event, 
			@Required(false) @Name("callbackUrl") String callbackUrl, 
			@Required(false) @Name("callbackMethod") String callbackMethod);
}
