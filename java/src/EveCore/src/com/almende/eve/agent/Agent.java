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
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.TreeSet;

import com.almende.eve.agent.annotation.Access;
import com.almende.eve.agent.annotation.AccessType;
import com.almende.eve.context.AgentContext;
import com.almende.eve.json.JSONRPC;
import com.almende.eve.json.JSONRPCException;
import com.almende.eve.json.JSONRequest;
import com.almende.eve.json.JSONResponse;
import com.almende.eve.json.annotation.Name;
import com.almende.eve.json.annotation.Required;
import com.almende.eve.json.jackson.JOM;
import com.fasterxml.jackson.databind.node.ObjectNode;


abstract public class Agent {
	private AgentContext context = null;
	//protected Context session = new SimpleContext(); // todo: add session context?
	
	public abstract String getDescription();
	public abstract String getVersion();

	public Agent() {}
	
	@Access(AccessType.UNAVAILABLE)
	final public void setContext(AgentContext context) {
		if (context != null) {
			this.context = context;
		}
	}

	@Access(AccessType.UNAVAILABLE)
	final public AgentContext getContext() {
		return context;
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
		List<Callback> subscriptions = null;
		String key = getSubscriptionsKey(event);
		Object value = context.get(key);		
		if (value != null) {
			subscriptions = (List<Callback>) value;
		}
		else {
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
		Object value = context.get(key);
		if (value != null) {
			List<Callback> subscriptions = (List<Callback>) value;
			
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
		Object valueEvent = context.get(keyEvent);
		if (valueEvent != null) {
			subscriptions.addAll( (List<Callback>) valueEvent);
		}
		
		// retrieve subscriptions from the all event "*"
		String keyAll = getSubscriptionsKey("*");
		Object valueAll = context.get(keyAll);
		if (valueAll != null) {
			subscriptions.addAll( (List<Callback>) valueAll);
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
	 * Get type description from a class. Returns for example "String" or 
	 * "List<String>".
	 * @param c
	 * @return
	 */
	private String getType(Type c) {
		String s = c.toString();
		
		// replace full namespaces to short names
		int point = s.lastIndexOf(".");
		while (point >= 0) {
			int angle = s.lastIndexOf("<", point);
			int space = s.lastIndexOf(" ", point);
			int start = Math.max(angle, space);
			s = s.substring(0, start + 1) + s.substring(point + 1);
			point = s.lastIndexOf(".");
		}
		
		// remove modifiers like "class blabla" or "interface blabla"
		int space = s.indexOf(" ");
		int angle = s.indexOf("<", point);
		if (space >= 0 && (angle < 0 || angle > space)) {
			s = s.substring(space + 1);
		}
		
		return s;
		
		/*
		// TODO: do some more professional reflection...
		String s = c.getSimpleName();	

		// the following seems not to work
		TypeVariable<?>[] types = c.getTypeParameters();
		if (types.length > 0) {
			s += "<";
			for (int j = 0; j < types.length; j++) {
				TypeVariable<?> jj = types[j];
				s += jj.getName();
				 ... not working
				//s += types[j].getClass().getSimpleName();
			}
			s += ">";
		}
		*/
	}
	
	/**
	 * Get all available methods of this agent
	 * @return
	 */
	final public List<Object> getMethods(@Name("asJSON") 
			@Required(false) Boolean asJSON) {
		Map<String, Object> methods = new TreeMap<String, Object>();
		if (asJSON == null) {
			asJSON = false;
		}

		Class<?> c = this.getClass();
		while (c != null && c != Object.class) {
			for (Method method : c.getDeclaredMethods()) {
				String methodName = method.getName();
				int mod = method.getModifiers();
				Access access = method.getAnnotation(Access.class); 
				// TODO: apply access when invoking a method of the agent

				boolean available = 
					!Modifier.isAbstract(mod) &&
					Modifier.isPublic(mod) &&
					(access == null || 
							(access.value() != AccessType.UNAVAILABLE &&
							 access.visible()));

				if (available) {
					//Class<?>[] types = method.getParameterTypes();
					Type[] types = method.getGenericParameterTypes();
					int paramNum = types.length;
					Annotation[][] paramAnnotations = method.getParameterAnnotations();
					String[] paramTypes = new String[paramNum];
					for(int i = 0; i < paramNum; i++){
						paramTypes[i] = getType(types[i]);	
					}
					
					// get parameters
					boolean validParamNames = true;
					String[] paramNames = new String[paramNum];
					boolean[] paramRequired = new boolean[paramNum];
					for(int i = 0; i < paramNum; i++){
						paramTypes[i] = getType(types[i]);	
						paramRequired[i] = true;
						
						Annotation[] annotations = paramAnnotations[i];
						for(Annotation annotation : annotations){
							if(annotation instanceof Name){
								Name pn = (Name) annotation;
								paramNames[i] = pn.value();
							}
							if(annotation instanceof Required){
								Required pr = (Required) annotation;
								paramRequired[i] = pr.value();
							}
						}
						
						if (paramNames[i] == null) {
							validParamNames = false;
						}
					}

					// TODO: not so nice 
					if (!validParamNames) {
						Class<?>[] pt = method.getParameterTypes();
						if (pt.length == 1 && pt[0].equals(ObjectNode.class)) {
							paramNames[0] = "params";
							validParamNames = true;
						}
					}
					
					// get return type
					String returnType = getType(method.getGenericReturnType());
					
					if (validParamNames) {
						if (asJSON) {
							// format as JSON
							List<Object> descParams = new ArrayList<Object>();
							for(int i = 0; i < paramNum; i++){
								Map<String, Object> paramData = new HashMap<String, Object>();
								paramData.put("name", paramNames[i]);
								paramData.put("type", paramTypes[i]);
								paramData.put("required", paramRequired[i]);
								descParams.add(paramData);
							}
							
							Map<String, Object> result = new HashMap<String, Object>(); 
							result.put("type", returnType);
							
							Map<String, Object> desc = new HashMap<String, Object>();
							desc.put("method", methodName);
							desc.put("params", descParams);
							desc.put("result", result);
							methods.put(methodName, desc);
						}
						else {
							// format as string
							String p = "";
							for(int i = 0; i < paramNum; i++){
								if (!p.isEmpty()) {
									p += ", ";
								}
								if (paramRequired[i]) {
									p += paramTypes[i] + " " + paramNames[i];
								}
								else {
									p += "[" + paramTypes[i] + " " + paramNames[i] + "]";
								}
							}
							String desc = returnType + " " + methodName + "(" + p + ")";
							methods.put(methodName, desc);							
						}
					}
				}
			}

			c = c.getSuperclass();
		}

		// create a sorted array
		List<Object> sortedMethods = new ArrayList<Object>();
		TreeSet<String> methodNames = new TreeSet<String>(methods.keySet());
		for (String methodName : methodNames) { 
		   sortedMethods.add(methods.get(methodName));
		   // do something
		}
		
		return sortedMethods;
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
	final public Object send(String url, String method, ObjectNode params) 
			throws IOException, JSONRPCException {
		JSONResponse response = JSONRPC.send(url, new JSONRequest(method, params));
		JSONRPCException err = response.getError();
		if (err != null) {
			throw err;
		}
		return response.getResult();
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
	final public Object send(String url, String method) 
			throws JSONRPCException, IOException {
		return send(url, method, null);
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
