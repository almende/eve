/**
 * @file AbstractAgent.java
 * 
 * @brief 
 * TODO: brief
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
 * Copyright © 2010-2011 Almende B.V.
 *
 * @author 	Jos de Jong, <jos@almende.org>
 * @date	  2011-05-02
 */

package eve.agent;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import net.sf.json.JSONObject;

import eve.agent.annotation.Access;
import eve.agent.annotation.AccessType;
import eve.agent.context.AgentContext;
import eve.agent.context.SimpleContext;
import eve.entity.Subscription;
import eve.json.JSONRPC;
import eve.json.JSONRequest;
import eve.json.annotation.ParameterName;

@SuppressWarnings("serial")
abstract public class Agent implements Serializable {
	protected AgentContext context = new SimpleContext();
	//protected Context session = new SimpleContext(); // todo: add session context
	
	private List<Subscription> subscriptions = null;
	
	public abstract String getDescription();
	public abstract String getVersion();

	public Agent() {}
	
	public void setContext(AgentContext context) {
		if (context != null) {
			this.context = context;
		}
	}

	public AgentContext getContext() {
		return context;
	}
	
	/**
	 * Retrieve the type name of this agent, its class
	 * @return classname
	 */
	public String getType() {
		return this.getClass().getSimpleName();
	}
	
	/**
	 * Retrieve a web page via which a user can communicate with this agent
	 * in a browser.
	 * @return html
	 */
	@Access(AccessType.UNAVAILABLE)
	public String getHTMLPage() {
		/* TODO
		String html = "<html>"
			+ "<body>"
			+ "Sorry, this agent has no HTML page available"
			+ "</body>" 
			+ "</html>";
		
		return html;
		*/
		return null;
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
	final public void subscribe(@ParameterName("event") String event, 
			@ParameterName("callbackUrl") String callbackUrl, 
			@ParameterName("callbackMethod") String callbackMethod) {
		if (subscriptions == null) {
			subscriptions = new ArrayList<Subscription>(); 
		}
		
		// first unsubscribe to prevent double subscriptions
		unsubscribe(event, callbackUrl, callbackMethod);
		
		Subscription subscription = new Subscription(event, callbackUrl, 
				callbackMethod);
		subscriptions.add(subscription);
	}
	
	/**
	 * Unsubscribe from an event
	 * @param event
	 * @param callbackUrl
	 */
	//@Access(AccessType.PRIVATE) // TODO
	final public void unsubscribe(@ParameterName("event") String event, 
			@ParameterName("callbackUrl") String callbackUrl,
			@ParameterName("callbackMethod") String callbackMethod) {
		if (subscriptions == null) {
			// there are no subscriptions so lets just do nothing and return
			return;
		}

		// TODO: create a way to unsubscribe an url from all subscribed events at once?
		int i = 0;
		while (i < subscriptions.size()) {
			Subscription subscription = subscriptions.get(i);
			if (event.equals(subscription.event) && 
					callbackUrl.equals(subscription.callbackUrl) &&
					callbackMethod.equals(subscription.callbackMethod)) {
				subscriptions.remove(i);
			}
			else {
				i++;
			}
		}
	}
	
	/**
	 * Trigger an event
	 * @param event
	 * @param params
	 */
	@Access(AccessType.UNAVAILABLE)
	final public void trigger(@ParameterName("event") String event, 
			@ParameterName("params") JSONObject params) {
		if (subscriptions == null) {
			// there are no subscriptions so lets just do nothing and return
			return;
		}

		for (Subscription subscription : subscriptions) {
			if (subscription.event.equals(event)) {
				try {
					// TODO: change for async callback!
					send(subscription.callbackUrl, subscription.callbackMethod, 
							params);
				} catch (Exception e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}
	}

	/**
	 * Send a request to an agent in JSON-RPC format
	 * @param url    The url of the agent
	 * @param method The name of the method
	 * @param params A JSONObject containing the parameter values of the method
	 * @return       
	 * @throws Exception 
	 * @throws JSONException 
	 */
	@Access(AccessType.UNAVAILABLE)
	final public Object send(String url, String method, JSONObject params) 
		throws Exception {
			return JSONRPC.send(url, new JSONRequest(method, params));
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
	final public void sendAsync(String url, String method, JSONObject params,
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
		return context.getId();
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
	final public static String getClassName(String url) {
		String[] parts = url.split("/");
		if (parts.length > 1) {
			return parts[parts.length - 2];
		}
		
		return "";
	}
}
