/**
 * @file TestAgent.java
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
 * Copyright © 2010-2012 Almende B.V.
 *
 * @author 	Jos de Jong, <jos@almende.org>
 * @date	  2011-03-05
 */
package com.almende.eve.agent.example;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.almende.eve.agent.Agent;
import com.almende.eve.agent.AgentFactory;
import com.almende.eve.entity.Person;
import com.almende.eve.json.JSONRPCException;
import com.almende.eve.json.JSONRequest;
import com.almende.eve.json.JSONRPCException.CODE;
import com.almende.eve.json.annotation.Name;
import com.almende.eve.json.annotation.Required;
import com.almende.eve.json.jackson.JOM;
import com.almende.eve.service.AsyncCallback;
import com.almende.eve.service.xmpp.XmppService;
import com.fasterxml.jackson.databind.node.ObjectNode;


// TODO: put TestAgent in a separate unit test project
public class TestAgent extends Agent {
	public String ping(@Name("message") String message) throws Exception {
		ObjectNode params = JOM.createObjectNode();
		params.put("message", message);
		trigger("message", params);
		return message;
	}

	public void init() {
		System.out.println("initializing TestAgent/" + getId());
		super.init();
	}
	
	public void destroy() {
		System.out.println("destroying TestAgent/" + getId());
		super.destroy();
	}

	public String getName(@Name("person") Person person) {
		return person.getName();
	}

	public Double getMarksAvg(@Name("person") Person person) {
		List<Double> marks = person.getMarks();
		Double sum = new Double(0);
		if (marks != null) {
			for (Double mark : marks) {
				sum += mark;
			}
		}
		return sum;
	}

	private String getMyUrl() {
		List<String> urls = getUrls();
		return urls.size() > 0 ? urls.get(0): null;
	}
	
	public String callMyself(@Name("method") String method, 
			@Name("params") ObjectNode params) 
			throws IOException, JSONRPCException, Exception {
		String resp = send(getMyUrl(), method, params, String.class);
		System.out.println("callMyself method=" + method  + ", params=" + params.toString() + ", resp=" +  resp);
		return resp;
	}

	public String cascade() throws IOException, JSONRPCException, Exception {
		String name1 = get("name");
		ObjectNode params = JOM.createObjectNode();
		params.put("key", "name");
		params.put("value", Math.round(Math.random() * 1000));
		send(getMyUrl(), "put" , params);

		String name2 = (String)get("name");

		System.out.println("callMyself name1=" + name1 + ", name2=" + name2);
		return name1 + " " + name2;
	}
	
	public Person getPerson(@Name("name") String name) {
		Person person = new Person();
		person.setName(name);
		List<Double> marks = new ArrayList<Double>();
		marks.add(6.8);
		marks.add(5.0);
		person.setMarks(marks);
		return person;
	}

	public Double add(@Name("a") Double a, 
			@Name("b") Double b) {
		return a + b;
	}

	public Double subtract(@Name("a") Double a, 
			@Name("b") Double b) {
		return a - b;
	}

	public Double multiply(@Name("a") Double a, 
			@Name("b") Double b) {
		return a * b;
	}

	public Double divide(@Name("a") Double a, 
			@Name("b") Double b) {
		return a / b;
	}

	public String printParams(ObjectNode params) {
		return "fields: " + params.size();
	}

	public void throwException() throws Exception {
		throw new Exception("Something went wrong...");
	}
	
	public void throwJSONRPCException() throws Exception {
		throw new JSONRPCException(CODE.NOT_FOUND);
	}
	
	// TODO: get this working
	public Double sum(@Name("values") List<Double> values) {
		Double sum = new Double(0);
		for (Double value : values) {
			sum += value;
		}
		return sum;
	}
	
	public Double sumArray(@Name("values") Double[] values) {
		Double sum = new Double(0);
		for (Double value : values) {
			sum += value;
		}
		return sum;
	}

	public void complexParameter(
			@Name("values") Map<String, List<Double>> values) {
	}
	
	public Double increment() {
		Double value = (Double) getContext().get("count");
		if (value == null) {
			value = new Double(0);
		}
		value++;
		getContext().put("count", value);

		return value;
	}
	
	public String get(@Name("key") String key) {
		return (String) getContext().get(key);
	}

	public void put(@Name("key") String key, 
			@Name("value") String value) {
		getContext().put(key, value);
	}
	
	public void registerPingEvent() throws Exception {
		subscribe(getMyUrl(), "ping", "pingCallback");
	}
	
	public void unregisterPingEvent() throws Exception {
		subscribe(getMyUrl(), "ping", "pingCallback");
	}
	
	public void pingCallback(@Name("params") ObjectNode params) {
		System.out.println("pingCallback " + getId() + " " + params.toString());
	}
	
	public void triggerPingEvent(
			@Name("message") @Required(false) String message ) throws Exception {
		String event = "ping";
		ObjectNode params = null;
		if (message != null) {
			params = JOM.createObjectNode();
			params.put("message", message);
		}
		trigger(event, params);
	}

	public void cancelTask(@Name("id") String id) {
		getContext().getScheduler().cancelTask(id);
	}
	
	public String createTask(@Name("delay") long delay) throws Exception {
		ObjectNode params = JOM.createObjectNode();
		params.put("message", "hello world");
		JSONRequest request = new JSONRequest("myTask", params);
		String id = getContext().getScheduler().createTask(request, delay);
		return id;
	}
	
	public void myTask(@Name("message") String message) {
		System.out.println("myTask is executed. Message: " + message);
	}

	public Object testSend(@Name("url") @Required(false) String url,
			@Name("method") @Required(false) String method) throws Exception {
		if (url == null) {
			url = "http://localhost:8080/EveCore/agents/chatagent/1/";
		}
		if (method == null) {
			method = "getDescription";
		}
		Object res = send(url, method, Object.class);
		System.out.println(res);
		return res;
	}

	public String testSendNonExistingMethod() throws Exception {
		String res = send("http://localhost:8080/EveCore/agents/chatagent/1/", 
				"nonExistingMethod", String.class);
		System.out.println(res);
		return res;
	}
	public void subscribeToAgent(@Required(false) @Name("url") String url) throws Exception {
		if (url == null) {
				url = "http://localhost:8080/EveCore/agents/testagent/2/";
		}
		String event = "dataChanged";
		String callback = "onEvent";
		subscribe(url, event, callback);
	}

	public void unsubscribeFromAgent(@Required(false) @Name("url") String url) throws Exception {
		if (url == null) {
			url = "http://localhost:8080/EveCore/agents/testagent/2/";
		}
		String event = "dataChanged";
		String callback = "onEvent";
		unsubscribe(url, event, callback);
	}
	
	public void triggerDataChanged() throws Exception {
		trigger("dataChanged", null);
	}
	
	public Object getAllSubscriptions() {
		return getContext().get("subscriptions");
	}
	
	public void onEvent(
	        @Name("agent") String agent,
	        @Name("event") String event, 
	        @Required(false) @Name("params") ObjectNode params) throws Exception {
	    System.out.println("onEvent " +
	            "agent=" + agent + ", " +
	            "event=" + event + ", " +
	            "params=" + ((params != null) ? params.toString() : null));
	}

	private String privateMethod() {
		return "You should not be able to execute this method via JSON-RPC! " +
			"It is private.";
	}
	
	// multiple methods with the same name
	public void methodVersionOne() {
		privateMethod();
	}
	public void methodVersionOne(@Name("param") String param) {
		privateMethod();
	}

	public String invalidMethod(@Name("param1") String param1, int param2) {
		return "This method is no valid JSON-RPC method: misses an @Name annotation.";
	}
	
	public void testAsyncXMPP (@Name("url") String url) throws Exception {
		System.out.println("testAsyncSend, url=" + url);
		String method = "multiply";
		ObjectNode params = JOM.createObjectNode();
		params.put("a", new Double(3));
		params.put("b", new Double(4.5));
		System.out.println("testAsyncXMPP, request=" + new JSONRequest(method, params));
		sendAsync(url, method, params, new AsyncCallback<Double>() {
			@Override
			public void onSuccess(Double result) {
				System.out.println("testAsyncXMPP result=" + result);
				ObjectNode params = JOM.createObjectNode();
				params.put("result", result);
				try {
					trigger("message", params);
				} catch (Exception e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}

			@Override
			public void onFailure(Throwable caught) {
				caught.printStackTrace();
			}
		}, Double.class);
	}
	
	public void testAsyncHTTP () throws Exception {
		System.out.println("testAsyncHTTP start...");
		String url = "http://eveagents.appspot.com/agents/googledirectionsagent/1/";
		String method = "getDurationHuman";
		ObjectNode params = JOM.createObjectNode();
		params.put("origin", "rotterdam");
		params.put("destination", "utrecht");
		sendAsync(url, method, params, new AsyncCallback<String>() {
			@Override
			public void onSuccess(String result) {
				System.out.println("testAsyncHTTP result=" + result);
			}

			@Override
			public void onFailure(Throwable caught) {
				caught.printStackTrace();
			}
		}, String.class);
		
		System.out.println("testAsyncHTTP end...");
	}
	
	public void xmppConnect(@Name("username") String username, 
			@Name("password") String password) throws Exception {
		AgentFactory factory = getContext().getAgentFactory();
		
		XmppService service = (XmppService) factory.getService("xmpp");
		if (service != null) {
			service.connect(getId(), username, password);
		}
		else {
			throw new Exception("No XMPP service registered");
		}
	}
	
	public void xmppDisconnect() throws Exception {
		AgentFactory factory = getContext().getAgentFactory();
		XmppService service = (XmppService) factory.getService("xmpp");
		if (service != null) {
			service.disconnect(getId());
		}
		else {
			throw new Exception("No XMPP service registered");
		}
	}
	
	@Override
	public String getVersion() {
		return "1.0";
	}
	
	@Override
	public String getDescription() {
		return 
		"This agent can be used for test purposes. " +
		"It contains a simple ping method.";
	}	
}
