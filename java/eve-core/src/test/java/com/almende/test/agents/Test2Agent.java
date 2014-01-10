/**
 * @file TestAgent.java
 * 
 * @brief
 *        TODO: brief
 * 
 * @license
 *          Licensed under the Apache License, Version 2.0 (the "License"); you
 *          may not
 *          use this file except in compliance with the License. You may obtain
 *          a copy
 *          of the License at
 * 
 *          http://www.apache.org/licenses/LICENSE-2.0
 * 
 *          Unless required by applicable law or agreed to in writing, software
 *          distributed under the License is distributed on an "AS IS" BASIS,
 *          WITHOUT
 *          WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See
 *          the
 *          License for the specific language governing permissions and
 *          limitations under
 *          the License.
 * 
 *          Copyright © 2010-2012 Almende B.V.
 * 
 * @author Jos de Jong, <jos@almende.org>
 * @date 2011-03-05
 */
package com.almende.test.agents;

import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.almende.eve.agent.Agent;
import com.almende.eve.agent.AgentHost;
import com.almende.eve.agent.callback.AsyncCallback;
import com.almende.eve.rpc.annotation.Access;
import com.almende.eve.rpc.annotation.AccessType;
import com.almende.eve.rpc.annotation.Name;
import com.almende.eve.rpc.annotation.Optional;
import com.almende.eve.rpc.annotation.Sender;
import com.almende.eve.rpc.jsonrpc.JSONRPCException;
import com.almende.eve.rpc.jsonrpc.JSONRPCException.CODE;
import com.almende.eve.rpc.jsonrpc.JSONRequest;
import com.almende.eve.rpc.jsonrpc.jackson.JOM;
import com.almende.eve.transport.xmpp.XmppService;
import com.almende.test.agents.entity.Person;
import com.almende.util.TypeUtil;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.type.TypeFactory;

/**
 * The Class Test2Agent.
 */
@Access(AccessType.PUBLIC)
public class Test2Agent extends Agent implements Test2AgentInterface {
	
	/**
	 * Ping.
	 *
	 * @param message the message
	 * @param sender the sender
	 * @return the string
	 * @throws Exception the exception
	 */
	public String ping(@Name("message") final String message, @Sender final String sender)
			throws Exception {
		final ObjectNode params = JOM.createObjectNode();
		params.put("message", message);
		params.put("sender", sender);
		
		getEventsFactory().trigger("message", params);
		return message;
	}
	
	/**
	 * Slow ping.
	 *
	 * @param message the message
	 * @param sender the sender
	 * @return the string
	 * @throws Exception the exception
	 */
	public String slowPing(@Name("message") final String message,
			@Sender final String sender) throws Exception {
		final ObjectNode params = JOM.createObjectNode();
		params.put("message", message);
		params.put("sender", sender);
		
		Thread.sleep(1000);
		
		getEventsFactory().trigger("message", params);
		
		return message;
	}
	
	/* (non-Javadoc)
	 * @see com.almende.eve.agent.Agent#sigCreate()
	 */
	@Override
	public void sigCreate() {
		System.out.println("creating Test2Agent/" + getId());
		super.sigCreate();
	}
	
	/* (non-Javadoc)
	 * @see com.almende.eve.agent.Agent#sigDelete()
	 */
	@Override
	public void sigDelete() {
		System.out.println("deleting Test2Agent/" + getId());
		super.sigDelete();
	}
	
	/* (non-Javadoc)
	 * @see com.almende.eve.agent.Agent#sigInit()
	 */
	@Override
	public void sigInit() {
		System.out.println("initializing Test2Agent/" + getId());
		super.sigInit();
	}
	
	/* (non-Javadoc)
	 * @see com.almende.eve.agent.Agent#sigDestroy()
	 */
	@Override
	public void sigDestroy() {
		System.out.println("destroying Test2Agent/" + getId());
		super.sigDestroy();
	}
	
	/**
	 * Gets the name.
	 *
	 * @param person the person
	 * @return the name
	 */
	public String getName(@Name("person") final Person person) {
		return person.getName();
	}
	
	/**
	 * Gets the marks avg.
	 *
	 * @param person the person
	 * @return the marks avg
	 */
	public Double getMarksAvg(@Name("person") final Person person) {
		final List<Double> marks = person.getMarks();
		Double sum = new Double(0);
		if (marks != null) {
			for (final Double mark : marks) {
				sum += mark;
			}
		}
		return sum;
	}
	
	/**
	 * Gets the my url.
	 *
	 * @return the my url
	 */
	private URI getMyUrl() {
		return getFirstUrl();
	}
	
	/**
	 * Call myself.
	 *
	 * @param method the method
	 * @param params the params
	 * @return the string
	 * @throws IOException Signals that an I/O exception has occurred.
	 * @throws JSONRPCException the jSONRPC exception
	 * @throws Exception the exception
	 */
	public String callMyself(@Name("method") final String method,
			@Name("params") final ObjectNode params) throws IOException,
			JSONRPCException, Exception {
		final String resp = send(getMyUrl(), method, params, String.class);
		System.out.println("callMyself method=" + method + ", params="
				+ params.toString() + ", resp=" + resp);
		return resp;
	}
	
	/**
	 * Call myself async.
	 *
	 * @param method the method
	 * @param params the params
	 * @throws IOException Signals that an I/O exception has occurred.
	 * @throws JSONRPCException the jSONRPC exception
	 * @throws Exception the exception
	 */
	public void callMyselfAsync(@Name("method") final String method,
			@Name("params") final ObjectNode params) throws IOException,
			JSONRPCException, Exception {
		
		sendAsync(getMyUrl(), method, params, new AsyncCallback<String>() {
			
			@Override
			public void onSuccess(final String resp) {
				// TODO Auto-generated method stub
				System.out.println("callMyselfAsync method=" + method
						+ ", params=" + params.toString() + ", resp=" + resp);
			}
			
			@Override
			public void onFailure(final Exception exception) {
				// TODO Auto-generated method stub
				System.out.println("Failure! callMyselfAsync method=" + method
						+ ", params=" + params.toString());
				exception.printStackTrace();
			}
			
		}, String.class);
		
	}
	
	/**
	 * Call other agent.
	 *
	 * @param url the url
	 * @param method the method
	 * @param params the params
	 * @return the string
	 * @throws IOException Signals that an I/O exception has occurred.
	 * @throws JSONRPCException the jSONRPC exception
	 * @throws Exception the exception
	 */
	public String callOtherAgent(@Name("url") final String url,
			@Name("method") final String method, @Name("params") final ObjectNode params)
			throws IOException, JSONRPCException, Exception {
		final String resp = send(URI.create(url), method, params, String.class);
		System.out.println("callOtherAgent url=" + url + " method=" + method
				+ ", params=" + params.toString() + ", resp=" + resp);
		return resp;
	}
	
	/* (non-Javadoc)
	 * @see com.almende.test.agents.Test2AgentInterface#testEnum(com.almende.test.agents.Test2AgentInterface.STATUS)
	 */
	@Override
	public STATUS testEnum(@Name("status") final STATUS status) {
		System.out.println("Status: " + status);
		return status;
	}
	
	/**
	 * Test enum proxy.
	 *
	 * @return the status
	 */
	public STATUS testEnumProxy() {
		final URI url = URI.create("http://eveagents.appspot.com/agents/test/");
		final Test2AgentInterface other = createAgentProxy(url,
				Test2AgentInterface.class);
		
		final STATUS value = other.testEnum(STATUS.GOOD);
		return value;
	}
	
	/**
	 * Test double non required.
	 *
	 * @param testme the testme
	 * @param testme2 the testme2
	 * @return the string
	 */
	public String testDoubleNonRequired(
			@Optional @Name("testme") final String testme,
			@Optional @Name("testme2") final String testme2) {
		
		return testme + ":" + testme2;
	}
	
	/**
	 * Test double required.
	 *
	 * @param testme the testme
	 * @param testme2 the testme2
	 * @return the string
	 */
	public String testDoubleRequired(@Name("testme") final String testme,
			@Name("testme2") final String testme2) {
		
		return testme + ":" + testme2;
	}
	
	/* (non-Javadoc)
	 * @see com.almende.test.agents.Test2AgentInterface#testVoid()
	 */
	@Override
	public void testVoid() {
		System.out.println("testVoid");
	}
	
	/**
	 * Test void proxy.
	 */
	public void testVoidProxy() {
		final URI url = URI.create("http://eveagents.appspot.com/agents/test/");
		final Test2AgentInterface other = createAgentProxy(url,
				Test2AgentInterface.class);
		other.testVoid();
	}
	
	/**
	 * Test enum send.
	 *
	 * @return the status
	 * @throws Exception the exception
	 */
	public STATUS testEnumSend() throws Exception {
		final URI url = URI.create("http://eveagents.appspot.com/agents/test/");
		final Map<String, Object> params = new HashMap<String, Object>();
		params.put("status", STATUS.GOOD);
		final STATUS value = send(url, "testEnum", params, STATUS.class);
		
		return value;
	}
	
	/**
	 * Cascade.
	 *
	 * @return the string
	 * @throws IOException Signals that an I/O exception has occurred.
	 * @throws JSONRPCException the jSONRPC exception
	 * @throws Exception the exception
	 */
	public String cascade() throws IOException, JSONRPCException, Exception {
		final String name1 = get("name");
		final Map<String, Object> params = new HashMap<String, Object>();
		params.put("key", "name");
		params.put("value", Math.round(Math.random() * 1000));
		send(getMyUrl(), "put", params);
		
		final String name2 = get("name");
		
		System.out.println("callMyself name1=" + name1 + ", name2=" + name2);
		return name1 + " " + name2;
	}
	
	/**
	 * Cascade2.
	 *
	 * @return the person
	 * @throws IOException Signals that an I/O exception has occurred.
	 * @throws JSONRPCException the jSONRPC exception
	 * @throws Exception the exception
	 */
	public Person cascade2() throws IOException, JSONRPCException, Exception {
		// test sending a POJO as params
		final Person person = new Person();
		person.setName("testname");
		return send(getMyUrl(), "getPerson", person, Person.class);
	}
	
	/**
	 * Gets the person.
	 *
	 * @param name the name
	 * @return the person
	 */
	public Person getPerson(@Name("name") final String name) {
		final Person person = new Person();
		person.setName(name);
		final List<Double> marks = new ArrayList<Double>();
		marks.add(6.8);
		marks.add(5.0);
		person.setMarks(marks);
		return person;
	}
	
	/* (non-Javadoc)
	 * @see com.almende.test.agents.Test2AgentInterface#add(java.lang.Double, java.lang.Double)
	 */
	@Override
	public Double add(@Name("a") final Double a, @Name("b") final Double b) {
		return a + b;
	}
	
	/**
	 * Subtract.
	 *
	 * @param a the a
	 * @param b the b
	 * @return the double
	 */
	public Double subtract(@Name("a") final Double a, @Name("b") final Double b) {
		return a - b;
	}
	
	/* (non-Javadoc)
	 * @see com.almende.test.agents.Test2AgentInterface#multiply(java.lang.Double, java.lang.Double)
	 */
	@Override
	public Double multiply(final Double a, final Double b) {
		return a * b;
	}
	
	/**
	 * Divide.
	 *
	 * @param a the a
	 * @param b the b
	 * @return the double
	 */
	public Double divide(@Name("a") final Double a, @Name("b") final Double b) {
		return a / b;
	}
	
	/**
	 * Prints the params.
	 *
	 * @param params the params
	 * @return the string
	 */
	public String printParams(final ObjectNode params) {
		return "fields: " + params.size();
	}
	
	/**
	 * Return null string.
	 *
	 * @return the string
	 */
	public String returnNullString() {
		return null;
	}
	
	/**
	 * Throw exception.
	 *
	 * @throws Exception the exception
	 */
	public void throwException() throws Exception {
		throw new Exception("Nothing went wrong...");
	}
	
	/**
	 * Throw jsonrpc exception.
	 *
	 * @throws JSONRPCException the jSONRPC exception
	 */
	public void throwJSONRPCException() throws JSONRPCException {
		throw new JSONRPCException(CODE.NOT_FOUND);
	}
	
	// TODO: get this working
	/**
	 * Sum.
	 *
	 * @param values the values
	 * @return the double
	 */
	public Double sum(@Name("values") final List<Double> values) {
		Double sum = new Double(0);
		for (final Double value : values) {
			sum += value;
		}
		return sum;
	}
	
	/**
	 * Sum array.
	 *
	 * @param values the values
	 * @return the double
	 */
	public Double sumArray(@Name("values") final Double[] values) {
		Double sum = new Double(0);
		for (final Double value : values) {
			sum += value;
		}
		return sum;
	}
	
	/**
	 * Complex parameter.
	 *
	 * @param values the values
	 */
	public void complexParameter(
			@Name("values") final Map<String, List<Double>> values) {
		for (final String key : values.keySet()) {
			final List<Double> value = values.get(key);
			for (final Double v : value) {
				System.out.println(key + " " + v);
			}
		}
	}
	
	/**
	 * Complex result.
	 *
	 * @return the map
	 */
	public Map<String, List<Double>> complexResult() {
		System.err.println("ComplexResult called!");
		final Map<String, List<Double>> result = new HashMap<String, List<Double>>();
		final List<Double> list = new ArrayList<Double>();
		list.add(1.1);
		list.add(0.4);
		result.put("result", list);
		
		return result;
	}
	
	/**
	 * Test tf complex result.
	 *
	 * @param url the url
	 * @return the double
	 * @throws Exception the exception
	 */
	public Double testTFComplexResult(@Name("url") final String url) throws Exception {
		final TypeFactory tf = JOM.getTypeFactory();
		final Map<String, List<Double>> res = send(URI.create(url), "complexResult",
				JOM.createObjectNode(), tf.constructMapType(HashMap.class, JOM
						.getTypeFactory().constructType(String.class),
						tf.constructCollectionType(List.class,
								Double.class)));
		return res.get("result").get(0);
	}
	
	/**
	 * Test complex result.
	 *
	 * @param url the url
	 * @return the double
	 * @throws Exception the exception
	 */
	public Double testComplexResult(@Name("url") final String url) throws Exception {
		
		final Map<String, List<Double>> res = send(URI.create(url), "complexResult",
				new TypeUtil<Map<String, List<Double>>>() {
				});
		return res.get("result").get(0);
	}
	
	/* (non-Javadoc)
	 * @see com.almende.test.agents.Test2AgentInterface#increment()
	 */
	@Override
	public Double increment() {
		Double value = getState().get("count", Double.class);
		if (value == null) {
			value = new Double(0);
		}
		value++;
		getState().put("count", value);
		
		return value;
	}
	
	/**
	 * Test double ret.
	 *
	 * @return the double
	 * @throws Exception the exception
	 */
	public Double testDoubleRet() throws Exception {
		return send(getFirstUrl(), "increment", Double.class);
	}
	
	/**
	 * Gets the.
	 *
	 * @param key the key
	 * @return the string
	 */
	public String get(@Name("key") final String key) {
		return getState().get(key, String.class);
	}
	
	/**
	 * Put.
	 *
	 * @param key the key
	 * @param value the value
	 */
	public void put(@Name("key") final String key, @Name("value") final String value) {
		getState().put(key, value);
	}
	
	/**
	 * Register ping event.
	 *
	 * @throws Exception the exception
	 */
	public void registerPingEvent() throws Exception {
		getEventsFactory().subscribe(getMyUrl(), "ping", "pingCallback");
	}
	
	/**
	 * Unregister ping event.
	 *
	 * @throws Exception the exception
	 */
	public void unregisterPingEvent() throws Exception {
		getEventsFactory().subscribe(getMyUrl(), "ping", "pingCallback");
	}
	
	/**
	 * Ping callback.
	 *
	 * @param params the params
	 */
	public void pingCallback(@Name("params") final ObjectNode params) {
		System.out.println("pingCallback " + getId() + " " + params.toString());
	}
	
	/**
	 * Trigger ping event.
	 *
	 * @param message the message
	 * @throws Exception the exception
	 */
	public void triggerPingEvent(@Name("message") @Optional final String message)
			throws Exception {
		final String event = "ping";
		ObjectNode params = null;
		if (message != null) {
			params = JOM.createObjectNode();
			params.put("message", message);
		}
		getEventsFactory().trigger(event, params);
	}
	
	/**
	 * Cancel task.
	 *
	 * @param id the id
	 */
	public void cancelTask(@Name("id") final String id) {
		getScheduler().cancelTask(id);
	}
	
	/**
	 * Creates the task.
	 *
	 * @param delay the delay
	 * @return the string
	 * @throws Exception the exception
	 */
	public String createTask(@Name("delay") final long delay) throws Exception {
		final ObjectNode params = JOM.createObjectNode();
		params.put("message", "hello world");
		final JSONRequest request = new JSONRequest("myTask", params);
		final String id = getScheduler().createTask(request, delay);
		return id;
	}
	
	/**
	 * My task.
	 *
	 * @param message the message
	 * @throws Exception the exception
	 */
	public void myTask(@Name("message") final String message) throws Exception {
		final ObjectNode params = JOM.createObjectNode();
		params.put("message", message);
		getEventsFactory().trigger("task", params);
		System.out.println("myTask is executed. Message: " + message);
	}
	
	/**
	 * Test send.
	 *
	 * @param url the url
	 * @param method the method
	 * @return the object
	 * @throws Exception the exception
	 */
	public Object testSend(@Name("url") @Optional String url,
			@Name("method") @Optional String method) throws Exception {
		if (url == null) {
			url = "http://localhost:8080/EveCore/agents/chatagent/1/";
		}
		if (method == null) {
			method = "getDescription";
		}
		final Object res = send(URI.create(url), method, Object.class);
		System.out.println(res);
		return res;
	}
	
	/**
	 * Test send non existing method.
	 *
	 * @return the string
	 * @throws Exception the exception
	 */
	public String testSendNonExistingMethod() throws Exception {
		final String res = send(
				URI.create("http://localhost:8080/EveCore/agents/chatagent/1/"),
				"nonExistingMethod", String.class);
		System.out.println(res);
		return res;
	}
	
	/**
	 * Subscribe to agent.
	 *
	 * @param url the url
	 * @return the string
	 * @throws Exception the exception
	 */
	public String subscribeToAgent(@Optional @Name("url") String url)
			throws Exception {
		if (url == null) {
			url = "http://localhost:8080/agents/testagent2/";
		}
		final String event = "dataChanged";
		final String callback = "onEvent";
		return getEventsFactory().subscribe(URI.create(url), event, callback);
	}
	
	/**
	 * Unsubscribe from agent.
	 *
	 * @param url the url
	 * @param subscriptionId the subscription id
	 * @throws Exception the exception
	 */
	public void unsubscribeFromAgent(@Optional @Name("url") String url,
			@Name("subscriptionId") final String subscriptionId) throws Exception {
		if (url == null) {
			url = "http://localhost:8080/agents/testagent2/";
		}
		// String event = "dataChanged";
		// String callback = "onEvent";
		getEventsFactory().unsubscribe(URI.create(url), subscriptionId);
	}
	
	/**
	 * Trigger data changed.
	 *
	 * @throws Exception the exception
	 */
	public void triggerDataChanged() throws Exception {
		getEventsFactory().trigger("dataChanged", null);
	}
	
	/**
	 * Gets the everything.
	 *
	 * @return the everything
	 */
	public Object getEverything() {
		return getState();
	}
	
	/**
	 * On event.
	 *
	 * @param subscriptionId the subscription id
	 * @param agent the agent
	 * @param event the event
	 * @param params the params
	 * @throws Exception the exception
	 */
	public void onEvent(
			@Optional @Name("subscriptionId") final String subscriptionId,
			@Optional @Name("agent") final String agent,
			@Optional @Name("event") final String event,
			@Optional @Name("params") final ObjectNode params) throws Exception {
		System.out.println("onEvent " + "subscriptionId=" + subscriptionId
				+ ", " + "agent=" + agent + ", " + "event=" + event + ", "
				+ "params=" + ((params != null) ? params.toString() : null));
		
		final ObjectNode data = JOM.createObjectNode();
		data.put("subscriptionId", subscriptionId);
		data.put("agent", agent);
		data.put("event", event);
		data.put("params", params);
		getEventsFactory().trigger("onEvent", data);
	}
	
	/**
	 * Private method.
	 *
	 * @return the string
	 */
	private String privateMethod() {
		return "You should not be able to execute this method via JSON-RPC! "
				+ "It is private.";
	}
	
	// multiple methods with the same name
	/**
	 * Method version one.
	 */
	public void methodVersionOne() {
		privateMethod();
	}
	
	/**
	 * Method version one.
	 *
	 * @param param the param
	 */
	public void methodVersionOne(@Name("param") final String param) {
		privateMethod();
	}
	
	/**
	 * Invalid method.
	 *
	 * @param param1 the param1
	 * @param param2 the param2
	 * @return the string
	 */
	public String invalidMethod(@Name("param1") final String param1, final int param2) {
		return "This method is no valid JSON-RPC method: misses an @Name annotation.";
	}
	
	/**
	 * Test async xmpp.
	 *
	 * @param url the url
	 * @throws Exception the exception
	 */
	public void testAsyncXMPP(@Name("url") final String url) throws Exception {
		System.out.println("testAsyncSend, url=" + url);
		final String method = "multiply";
		final ObjectNode params = JOM.createObjectNode();
		params.put("a", new Double(3));
		params.put("b", new Double(4.5));
		System.out.println("testAsyncXMPP, request="
				+ new JSONRequest(method, params));
		sendAsync(URI.create(url), method, params, new AsyncCallback<Double>() {
			@Override
			public void onSuccess(final Double result) {
				System.out.println("testAsyncXMPP result=" + result);
				final ObjectNode params = JOM.createObjectNode();
				params.put("result", result);
				try {
					getEventsFactory().trigger("message", params);
				} catch (final Exception e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
			
			@Override
			public void onFailure(final Exception exception) {
				exception.printStackTrace();
			}
		}, Double.class);
	}
	
	/**
	 * Test sync xmpp.
	 *
	 * @param url the url
	 * @throws Exception the exception
	 */
	public void testSyncXMPP(@Name("url") final String url) throws Exception {
		System.out.println("testSyncSend, url=" + url);
		final String method = "multiply";
		final ObjectNode params = JOM.createObjectNode();
		params.put("a", new Double(3));
		params.put("b", new Double(4.5));
		System.out.println("testSyncXMPP, request="
				+ new JSONRequest(method, params));
		final Double result = send(URI.create(url), method, params, Double.class);
		System.out.println("testSyncXMPP result=" + result);
		try {
			final ObjectNode messageParams = JOM.createObjectNode();
			messageParams.put("result", result);
			getEventsFactory().trigger("message", messageParams);
		} catch (final Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	/**
	 * Test get contacts.
	 *
	 * @param url the url
	 * @throws Exception the exception
	 */
	public void testGetContacts(@Name("url") final String url) throws Exception {
		System.out.println("testGetContacts, url=" + url);
		final String method = "getContacts";
		final ObjectNode params = JOM.createObjectNode();
		params.put("filter", "");
		System.out.println("testGetContacts, request="
				+ new JSONRequest(method, params));
		sendAsync(URI.create(url), method, params,
				new AsyncCallback<ArrayNode>() {
					@Override
					public void onSuccess(final ArrayNode result) {
						System.out.println("testGetContacts result=" + result);
						final ObjectNode params = JOM.createObjectNode();
						params.put("result", result);
						try {
							getEventsFactory().trigger("message", params);
						} catch (final Exception e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
					}
					
					@Override
					public void onFailure(final Exception exception) {
						exception.printStackTrace();
					}
				}, ArrayNode.class);
	}
	
	/**
	 * Test async http.
	 *
	 * @throws Exception the exception
	 */
	public void testAsyncHTTP() throws Exception {
		System.out.println("testAsyncHTTP start...");
		final String url = "http://eveagents.appspot.com/agents/googledirectionsagent/1/";
		final String method = "getDurationHuman";
		final ObjectNode params = JOM.createObjectNode();
		params.put("origin", "rotterdam");
		params.put("destination", "utrecht");
		sendAsync(URI.create(url), method, params, new AsyncCallback<String>() {
			@Override
			public void onSuccess(final String result) {
				System.out.println("testAsyncHTTP result=" + result);
			}
			
			@Override
			public void onFailure(final Exception exception) {
				exception.printStackTrace();
			}
		}, String.class);
		
		System.out.println("testAsyncHTTP end...");
	}
	
	/**
	 * Xmpp connect.
	 *
	 * @param username the username
	 * @param password the password
	 * @throws Exception the exception
	 */
	public void xmppConnect(@Name("username") final String username,
			@Name("password") final String password) throws Exception {
		final AgentHost host = getAgentHost();
		
		final XmppService service = (XmppService) host.getTransportService("xmpp");
		if (service != null) {
			service.connect(getId(), username, password);
		} else {
			throw new Exception("No XMPP service registered");
		}
	}
	
	/**
	 * Xmpp disconnect.
	 *
	 * @throws Exception the exception
	 */
	public void xmppDisconnect() throws Exception {
		final AgentHost host = getAgentHost();
		final XmppService service = (XmppService) host.getTransportService("xmpp");
		if (service != null) {
			service.disconnect(getId());
		} else {
			throw new Exception("No XMPP service registered");
		}
	}
	
	/**
	 * Delete my self.
	 *
	 * @throws Exception the exception
	 */
	public void deleteMySelf() throws Exception {
		getAgentHost().deleteAgent(getId());
	}
	
	/**
	 * Test agent proxy.
	 *
	 * @return the double
	 */
	public Double testAgentProxy() {
		final String url = "http://eveagents.appspot.com/agents/testagent/1/";
		final Test2AgentInterface other = createAgentProxy(URI.create(url),
				Test2AgentInterface.class);
		final Double value = other.increment();
		return value;
	}
	
	/**
	 * Test agent proxy2.
	 *
	 * @return the double
	 */
	public Double testAgentProxy2() {
		final String url = "http://eveagents.appspot.com/agents/testagent/1/";
		final Test2AgentInterface other = createAgentProxy(URI.create(url),
				Test2AgentInterface.class);
		
		final Double value = other.multiply(2.3, 4.5);
		return value;
	}
	
	/**
	 * Test agent proxy3.
	 *
	 * @return the list
	 */
	public List<Object> testAgentProxy3() {
		final String url = "http://eveagents.appspot.com/agents/testagent/1/";
		final Test2AgentInterface other = createAgentProxy(URI.create(url),
				Test2AgentInterface.class);
		
		final List<Object> value = other.getMethods();
		return value;
	}
	
	/**
	 * Test agent proxy4.
	 *
	 * @return the double
	 */
	public Double testAgentProxy4() {
		final String url = "http://eveagents.appspot.com/agents/testagent/1/";
		
		final Test2AgentInterface other = createAgentProxy(URI.create(url),
				Test2AgentInterface.class);
		
		final Double value = other.add(2.3, null);
		return value;
	}
	
	/**
	 * Gets the urls of gloria.
	 *
	 * @return the urls of gloria
	 * @throws Exception the exception
	 */
	public ArrayNode getUrlsOfGloria() throws Exception {
		final String url = "xmpp:gloria@openid.almende.org";
		/*
		 * async works fine
		 * sendAsync(url, "getUrls", JOM.createObjectNode(), new
		 * AsyncCallback<ArrayNode>() {
		 * 
		 * @Override
		 * public void onSuccess(ArrayNode result) {
		 * System.out.println("gloria's urls=" + urls);
		 * }
		 * 
		 * @Override
		 * public void onFailure(Exception exception) {
		 * exception.printStackTrace();
		 * }
		 * }, ArrayNode.class);
		 */
		final ArrayNode urls = send(URI.create(url), "getUrls",
				JOM.createObjectNode(), ArrayNode.class);
		System.out.println("gloria's urls=" + urls);
		return urls;
	}
	
	/**
	 * Gets the urls of merlin async.
	 *
	 * @throws Exception the exception
	 */
	public void getUrlsOfMerlinAsync() throws Exception {
		final String url = "xmpp:merlin@openid.almende.org";
		sendAsync(URI.create(url), "getUrls", JOM.createObjectNode(),
				new AsyncCallback<ArrayNode>() {
					@Override
					public void onSuccess(final ArrayNode urls) {
						System.out.println("merlins urls=" + urls);
					}
					
					@Override
					public void onFailure(final Exception exception) {
						exception.printStackTrace();
					}
				}, ArrayNode.class);
	}
	
	/**
	 * Gets the urls of merlin.
	 *
	 * @return the urls of merlin
	 * @throws Exception the exception
	 */
	public ArrayNode getUrlsOfMerlin() throws Exception {
		final String url = "xmpp:merlin@openid.almende.org";
		final ArrayNode urls = send(URI.create(url), "getUrls",
				JOM.createObjectNode(), ArrayNode.class);
		System.out.println("merlins urls=" + urls);
		return urls;
	}
	
	/**
	 * Gets the urls of jos.
	 *
	 * @return the urls of jos
	 * @throws Exception the exception
	 */
	public ArrayNode getUrlsOfJos() throws Exception {
		final String url = "xmpp:jos@openid.almende.org";
		final ArrayNode urls = send(URI.create(url), "getUrls",
				JOM.createObjectNode(), ArrayNode.class);
		System.out.println("jos's urls=" + urls);
		return urls;
	}
	
	/**
	 * Gets the list of merlin.
	 *
	 * @return the list of merlin
	 * @throws Exception the exception
	 */
	public ArrayNode getListOfMerlin() throws Exception {
		final String url = "xmpp:merlin@openid.almende.org";
		final ArrayNode list = send(URI.create(url), "list", JOM.createObjectNode(),
				ArrayNode.class);
		System.out.println("merlins list=" + list);
		return list;
	}
	
	/* (non-Javadoc)
	 * @see com.almende.eve.agent.Agent#getVersion()
	 */
	@Override
	public String getVersion() {
		return "1.0";
	}
	
	/* (non-Javadoc)
	 * @see com.almende.eve.agent.Agent#getDescription()
	 */
	@Override
	public String getDescription() {
		return "This agent can be used for test purposes. "
				+ "It contains a simple ping method.";
	}
}
