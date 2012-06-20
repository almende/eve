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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.almende.eve.agent.Agent;
import com.almende.eve.entity.Person;
import com.almende.eve.json.JSONRPCException;
import com.almende.eve.json.JSONRequest;
import com.almende.eve.json.JSONRPCException.CODE;
import com.almende.eve.json.annotation.Name;
import com.almende.eve.json.annotation.Required;
import com.almende.eve.json.jackson.JOM;
import com.fasterxml.jackson.databind.node.ObjectNode;


// TODO: put TestAgent in a separate unit test project
public class TestAgent extends Agent {
	public String ping(@Name("message") String message) {
		return message;
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
		Double value = new Double(0);
		if (getContext().has("count")) {
			value = getContext().get("count", Double.class);
		}
		value++;
		getContext().put("count", value);

		return value;
	}
	
	public String get(@Name("key") String key) {
		return getContext().get(key, String.class);
	}

	public void put(@Name("key") String key, 
			@Name("value") String value) {
		getContext().put(key, value);
	}
	
	// TODO: onTrigger does not show up in getMethods
	public void onTrigger(ObjectNode params) {
		System.out.println("onTrigger " + getId() + " " + params.toString());
	}

	public void doTrigger(@Name("event") String event, 
			@Name("params") @Required(false) ObjectNode params) 
			throws Exception {
		trigger(event, params);
	}


	public String createTaskInterval(@Name("interval") long interval) throws Exception {
		ObjectNode params = JOM.createObjectNode();
		params.put("message", "hello world");
		JSONRequest request = new JSONRequest("pingTask", params);
		String id = getContext().getScheduler().setInterval(getUrl(), request, interval);
		return id;
	}
	
	public void cancelTask(@Name("id") String id) {
		getContext().getScheduler().cancelTimer(id);
	}
	
	public Set<String> getTasks() {
		return getContext().getScheduler().getTimers();
		// TODO: must a getTasks also return the contents of the task?
	}
	

	public String createTask(@Name("delay") long delay) throws Exception {
		ObjectNode params = JOM.createObjectNode();
		params.put("message", "hello world");
		JSONRequest request = new JSONRequest("myTask", params);
		String id = getContext().getScheduler().setTimeout(getUrl(), request, delay);
		return id;
	}
	
	public void myTask(@Name("message") String message) {
		System.out.println("myTask is executed. Message: " + message);
	}
	

	public List<String> testSend() throws Exception {
		ArrayList<String> type = new ArrayList<String>();
		@SuppressWarnings("unchecked")
		List<String> res = send("http://localhost:8080/EveCore/agents/chatagent/1", 
				"getConnections", type.getClass());
		System.out.println(res);
		return res;
	}

	public String testSendNonExistingMethod() throws Exception {
		String res = send("http://localhost:8080/EveCore/agents/chatagent/1", 
				"nonExistingMethod", String.class);
		System.out.println(res);
		return res;
	}
	public void subscribeToAgent() throws Exception {
		String url = "http://server/agents/agenttype/agentx";
		String method = "subscribe";
		ObjectNode params = JOM.createObjectNode();
		params.put("event", "dataChanged");
		params.put("callbackUrl", getUrl());
		params.put("callbackMethod", "onEvent");
		send(url, method, params);
	}

	public void unsubscribeFromAgent() throws Exception {
		String url = "http://server/agents/agenttype/agentx";
		String method = "unsubscribe";
		ObjectNode params = JOM.createObjectNode();
		params.put("event", "dataChanged");
		params.put("callbackUrl", getUrl());
		params.put("callbackMethod", "onEvent");
		send(url, method, params);
	}
	
	public void onEvent(@Name("agent") String agent, 
			@Name("event") String event, 
			@Required(false) @Name("params") ObjectNode params) 
			throws Exception {
		System.out.println("onEvent " + agent + " " + event + " " + 
				((params != null) ? params.toString() : ""));
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
