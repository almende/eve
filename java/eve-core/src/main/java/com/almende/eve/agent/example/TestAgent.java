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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.almende.eve.agent.Agent;
import com.almende.eve.agent.AgentFactory;
import com.almende.eve.agent.annotation.Sender;
import com.almende.eve.agent.annotation.Name;
import com.almende.eve.agent.annotation.Required;
import com.almende.eve.entity.Person;
import com.almende.eve.rpc.jsonrpc.JSONRPCException;
import com.almende.eve.rpc.jsonrpc.JSONRequest;
import com.almende.eve.rpc.jsonrpc.JSONRPCException.CODE;
import com.almende.eve.rpc.jsonrpc.jackson.JOM;
import com.almende.eve.transport.AsyncCallback;
import com.almende.eve.transport.xmpp.XmppService;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;


// TODO: put TestAgent in a separate unit test project
public class TestAgent extends Agent implements TestAgentInterface {
	public String ping(@Name("message") String message, 
			@Sender String sender) throws Exception {
		ObjectNode params = JOM.createObjectNode();
		params.put("message", message);
		params.put("sender", sender);
		
		trigger("message", params);
		return message;
	}

	public void create() {
		System.out.println("creating TestAgent/" + getId());
		super.create();
	}
	
	public void delete() {
		System.out.println("deleting TestAgent/" + getId());
		super.delete();
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

	public STATUS testEnum(@Name("status") STATUS status) {
		System.out.println("Status: " + status);
		return status;
	}

	public STATUS testEnumProxy() {
		String url = "http://eveagents.appspot.com/agents/test/";
		TestAgentInterface other = createAgentProxy(url, TestAgentInterface.class);
		
		STATUS value = other.testEnum(STATUS.GOOD);
		return value;
	}

	public void testVoid() {
		System.out.println("testVoid");
	}

	public void testVoidProxy() {
		String url = "http://eveagents.appspot.com/agents/test/";
		TestAgentInterface other = createAgentProxy(url, TestAgentInterface.class);
		other.testVoid();
	}

	public STATUS testEnumSend() throws Exception {
		String url = "http://eveagents.appspot.com/agents/test/";
		Map<String, Object> params = new HashMap<String, Object>();
		params.put("status", STATUS.GOOD);
		STATUS value = send(url, "testEnum", params, STATUS.class);
		
		return value;
	}
	
	public String cascade() throws IOException, JSONRPCException, Exception {
		String name1 = get("name");
		Map<String, Object> params = new HashMap<String, Object>();
		params.put("key", "name");
		params.put("value", Math.round(Math.random() * 1000));
		send(getMyUrl(), "put" , params);

		String name2 = (String)get("name");

		System.out.println("callMyself name1=" + name1 + ", name2=" + name2);
		return name1 + " " + name2;
	}
	
	public Person cascade2() throws IOException, JSONRPCException, Exception {
		// test sending a POJO as params
		Person person = new Person();
		person.setName("testname");
		return send(getMyUrl(), "getPerson" , person, Person.class);
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

	public Double add(@Name("a") Double a, @Name("b") Double b) {
		return a + b;
	}

	public Double subtract(@Name("a") Double a, @Name("b") Double b) {
		return a - b;
	}

	public Double multiply(Double a, Double b) {
		return a * b;
	}

	public Double divide(@Name("a") Double a, @Name("b") Double b) {
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
		for (String key : values.keySet()) {
			List<Double> value = values.get(key);
			for (Double v : value) {
				System.out.println(key + " " + v);
			}
		}
	}
	
	public Double increment() {
		Double value = (Double) getState().get("count");
		if (value == null) {
			value = new Double(0);
		}
		value++;
		getState().put("count", value);

		return value;
	}
	
	public String get(@Name("key") String key) {
		return (String) getState().get(key);
	}

	public void put(@Name("key") String key, 
			@Name("value") String value) {
		getState().put(key, value);
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
		getScheduler().cancelTask(id);
	}
	
	public String createTask(@Name("delay") long delay) throws Exception {
		ObjectNode params = JOM.createObjectNode();
		params.put("message", "hello world");
		JSONRequest request = new JSONRequest("myTask", params);
		String id = getScheduler().createTask(request, delay);
		return id;
	}
	
	public Set<String> getTasks() {
		return getScheduler().getTasks();
	}
	
	public void myTask(@Name("message") String message) throws Exception {
		ObjectNode params = JOM.createObjectNode();
		params.put("message", message);
		trigger("task", params);
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
	
	public String subscribeToAgent(@Required(false) @Name("url") String url) throws Exception {
		if (url == null) {
				url = "http://localhost:8080/agents/testagent2/";
		}
		String event = "dataChanged";
		String callback = "onEvent";
		return subscribe(url, event, callback);
	}

	public void unsubscribeFromAgent(@Required(false) @Name("url") String url,
			@Name("subscriptionId") String subscriptionId) throws Exception {
		if (url == null) {
			url = "http://localhost:8080/agents/testagent2/";
		}
		//String event = "dataChanged";
		//String callback = "onEvent";
		unsubscribe(url, subscriptionId);
	}
	
	public void triggerDataChanged() throws Exception {
		trigger("dataChanged", null);
	}
	
	public Object getEverything() {
		return getState();
	}
	
	public void onEvent(
			@Required(false) @Name("subscriptionId") String subscriptionId,
			@Required(false) @Name("agent") String agent,
	        @Required(false) @Name("event") String event, 
	        @Required(false) @Name("params") ObjectNode params) throws Exception {
	    System.out.println("onEvent " +
	    		"subscriptionId=" + subscriptionId + ", " +
	            "agent=" + agent + ", " +
	            "event=" + event + ", " +
	            "params=" + ((params != null) ? params.toString() : null));
	    
	    ObjectNode data = JOM.createObjectNode();
	    data.put("subscriptionId", subscriptionId);
	    data.put("agent", agent);
	    data.put("event", event);
	    data.put("params", params);
	    trigger ("onEvent", data);
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
			public void onFailure(Exception exception) {
				exception.printStackTrace();
			}
		}, Double.class);
	}

	public void testSyncXMPP (@Name("url") String url) throws Exception {
		System.out.println("testSyncSend, url=" + url);
		String method = "multiply";
		ObjectNode params = JOM.createObjectNode();
		params.put("a", new Double(3));
		params.put("b", new Double(4.5));
		System.out.println("testSyncXMPP, request=" + new JSONRequest(method, params));
		Double result = send(url, method, params, Double.class);
		System.out.println("testSyncXMPP result=" + result);
		try {
			ObjectNode messageParams = JOM.createObjectNode();
			messageParams.put("result", result);
			trigger("message", messageParams);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public void testGetContacts (@Name("url") String url) throws Exception {
		System.out.println("testGetContacts, url=" + url);
		String method = "getContacts";
		ObjectNode params = JOM.createObjectNode();
		params.put("filter", "");
		System.out.println("testGetContacts, request=" + new JSONRequest(method, params));
		sendAsync(url, method, params, new AsyncCallback<ArrayNode>() {
			@Override
			public void onSuccess(ArrayNode result) {
				System.out.println("testGetContacts result=" + result);
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
			public void onFailure(Exception exception) {
				exception.printStackTrace();
			}
		}, ArrayNode.class);
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
			public void onFailure(Exception exception) {
				exception.printStackTrace();
			}
		}, String.class);
		
		System.out.println("testAsyncHTTP end...");
	}
	
	public void xmppConnect(@Name("username") String username, 
			@Name("password") String password) throws Exception {
		AgentFactory factory = getAgentFactory();
		
		XmppService service = (XmppService) factory.getTransportService("xmpp");
		if (service != null) {
			service.connect(getId(), username, password);
		}
		else {
			throw new Exception("No XMPP service registered");
		}
	}
	
	public void xmppDisconnect() throws Exception {
		AgentFactory factory = getAgentFactory();
		XmppService service = (XmppService) factory.getTransportService("xmpp");
		if (service != null) {
			service.disconnect(getId());
		}
		else {
			throw new Exception("No XMPP service registered");
		}
	}
	
	public void deleteMySelf() throws Exception {
		getAgentFactory().deleteAgent(getId());
	}
	
	public Double testAgentProxy() {
		String url = "http://eveagents.appspot.com/agents/testagent/1/";
		TestAgentInterface other = createAgentProxy(url, TestAgentInterface.class);
		
		Double value = other.increment();
		return value;
	}
	
	public Double testAgentProxy2() {
		String url = "http://eveagents.appspot.com/agents/testagent/1/";
		TestAgentInterface other = createAgentProxy(url, TestAgentInterface.class);
		
		Double value = other.multiply(2.3, 4.5);
		return value;
	}

	public List<Object> testAgentProxy3() {
		String url = "http://eveagents.appspot.com/agents/testagent/1/";
		TestAgentInterface other = createAgentProxy(url, TestAgentInterface.class);
		
		List<Object> value = other.getMethods();
		return value;
	}
	
	public void testAgentProxy5() {
		String url = "http://eveagents.appspot.com/agents/testagent/1/";
		// This should fail, Person is no Interface...
		Person other = createAgentProxy(url, Person.class);
		other.setName("bla");
	}
	
	public Double testAgentProxy4() {
		String url = "http://eveagents.appspot.com/agents/testagent/1/";
		
		TestAgentInterface other = createAgentProxy(url, TestAgentInterface.class);
		
		Double value = other.add(2.3, null);
		return value;
	}
	
	public ArrayNode getUrlsOfGloria() throws Exception {
		String url = "xmpp:gloria@openid.almende.org";
		/* async works fine
		sendAsync(url, "getUrls", JOM.createObjectNode(), new AsyncCallback<ArrayNode>() {
			@Override
			public void onSuccess(ArrayNode result) {
				System.out.println("gloria's urls=" + urls);
			}

			@Override
			public void onFailure(Exception exception) {
				exception.printStackTrace();
			}
		}, ArrayNode.class);
		*/
		ArrayNode urls = send(url, "getUrls", JOM.createObjectNode(), ArrayNode.class);
		System.out.println("gloria's urls=" + urls);
		return urls;
	}
	
	public void getUrlsOfMerlinAsync() throws Exception {
		String url = "xmpp:merlin@openid.almende.org";
		sendAsync(url, "getUrls", JOM.createObjectNode(), new AsyncCallback<ArrayNode>() {
			@Override
			public void onSuccess(ArrayNode urls) {
				System.out.println("merlins urls=" + urls);
			}

			@Override
			public void onFailure(Exception exception) {
				exception.printStackTrace();
			}
		}, ArrayNode.class);
	}

	public ArrayNode getUrlsOfMerlin() throws Exception {
		String url = "xmpp:merlin@openid.almende.org";
		ArrayNode urls = send(url, "getUrls", JOM.createObjectNode(), ArrayNode.class);
		System.out.println("merlins urls=" + urls);
		return urls;
	}
	
	public ArrayNode getUrlsOfJos() throws Exception {
		String url = "xmpp:jos@openid.almende.org";
		ArrayNode urls = send(url, "getUrls", JOM.createObjectNode(), ArrayNode.class);
		System.out.println("jos's urls=" + urls);
		return urls;
	}
		
	public ArrayNode getListOfMerlin() throws Exception {
		String url = "xmpp:merlin@openid.almende.org";
		ArrayNode list = send(url, "list", JOM.createObjectNode(), ArrayNode.class);
		System.out.println("merlins list=" + list);
		return list;
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
