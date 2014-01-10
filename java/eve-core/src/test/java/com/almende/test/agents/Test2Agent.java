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

@Access(AccessType.PUBLIC)
public class Test2Agent extends Agent implements Test2AgentInterface {
	public String ping(@Name("message") final String message, @Sender final String sender)
			throws Exception {
		final ObjectNode params = JOM.createObjectNode();
		params.put("message", message);
		params.put("sender", sender);
		
		getEventsFactory().trigger("message", params);
		return message;
	}
	
	public String slowPing(@Name("message") final String message,
			@Sender final String sender) throws Exception {
		final ObjectNode params = JOM.createObjectNode();
		params.put("message", message);
		params.put("sender", sender);
		
		Thread.sleep(1000);
		
		getEventsFactory().trigger("message", params);
		
		return message;
	}
	
	@Override
	public void sigCreate() {
		System.out.println("creating Test2Agent/" + getId());
		super.sigCreate();
	}
	
	@Override
	public void sigDelete() {
		System.out.println("deleting Test2Agent/" + getId());
		super.sigDelete();
	}
	
	@Override
	public void sigInit() {
		System.out.println("initializing Test2Agent/" + getId());
		super.sigInit();
	}
	
	@Override
	public void sigDestroy() {
		System.out.println("destroying Test2Agent/" + getId());
		super.sigDestroy();
	}
	
	public String getName(@Name("person") final Person person) {
		return person.getName();
	}
	
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
	
	private URI getMyUrl() {
		return getFirstUrl();
	}
	
	public String callMyself(@Name("method") final String method,
			@Name("params") final ObjectNode params) throws IOException,
			JSONRPCException, Exception {
		final String resp = send(getMyUrl(), method, params, String.class);
		System.out.println("callMyself method=" + method + ", params="
				+ params.toString() + ", resp=" + resp);
		return resp;
	}
	
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
	
	public String callOtherAgent(@Name("url") final String url,
			@Name("method") final String method, @Name("params") final ObjectNode params)
			throws IOException, JSONRPCException, Exception {
		final String resp = send(URI.create(url), method, params, String.class);
		System.out.println("callOtherAgent url=" + url + " method=" + method
				+ ", params=" + params.toString() + ", resp=" + resp);
		return resp;
	}
	
	@Override
	public STATUS testEnum(@Name("status") final STATUS status) {
		System.out.println("Status: " + status);
		return status;
	}
	
	public STATUS testEnumProxy() {
		final URI url = URI.create("http://eveagents.appspot.com/agents/test/");
		final Test2AgentInterface other = createAgentProxy(url,
				Test2AgentInterface.class);
		
		final STATUS value = other.testEnum(STATUS.GOOD);
		return value;
	}
	
	public String testDoubleNonRequired(
			@Optional @Name("testme") final String testme,
			@Optional @Name("testme2") final String testme2) {
		
		return testme + ":" + testme2;
	}
	
	public String testDoubleRequired(@Name("testme") final String testme,
			@Name("testme2") final String testme2) {
		
		return testme + ":" + testme2;
	}
	
	@Override
	public void testVoid() {
		System.out.println("testVoid");
	}
	
	public void testVoidProxy() {
		final URI url = URI.create("http://eveagents.appspot.com/agents/test/");
		final Test2AgentInterface other = createAgentProxy(url,
				Test2AgentInterface.class);
		other.testVoid();
	}
	
	public STATUS testEnumSend() throws Exception {
		final URI url = URI.create("http://eveagents.appspot.com/agents/test/");
		final Map<String, Object> params = new HashMap<String, Object>();
		params.put("status", STATUS.GOOD);
		final STATUS value = send(url, "testEnum", params, STATUS.class);
		
		return value;
	}
	
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
	
	public Person cascade2() throws IOException, JSONRPCException, Exception {
		// test sending a POJO as params
		final Person person = new Person();
		person.setName("testname");
		return send(getMyUrl(), "getPerson", person, Person.class);
	}
	
	public Person getPerson(@Name("name") final String name) {
		final Person person = new Person();
		person.setName(name);
		final List<Double> marks = new ArrayList<Double>();
		marks.add(6.8);
		marks.add(5.0);
		person.setMarks(marks);
		return person;
	}
	
	@Override
	public Double add(@Name("a") final Double a, @Name("b") final Double b) {
		return a + b;
	}
	
	public Double subtract(@Name("a") final Double a, @Name("b") final Double b) {
		return a - b;
	}
	
	@Override
	public Double multiply(final Double a, final Double b) {
		return a * b;
	}
	
	public Double divide(@Name("a") final Double a, @Name("b") final Double b) {
		return a / b;
	}
	
	public String printParams(final ObjectNode params) {
		return "fields: " + params.size();
	}
	
	public String returnNullString() {
		return null;
	}
	
	public void throwException() throws Exception {
		throw new Exception("Nothing went wrong...");
	}
	
	public void throwJSONRPCException() throws JSONRPCException {
		throw new JSONRPCException(CODE.NOT_FOUND);
	}
	
	// TODO: get this working
	public Double sum(@Name("values") final List<Double> values) {
		Double sum = new Double(0);
		for (final Double value : values) {
			sum += value;
		}
		return sum;
	}
	
	public Double sumArray(@Name("values") final Double[] values) {
		Double sum = new Double(0);
		for (final Double value : values) {
			sum += value;
		}
		return sum;
	}
	
	public void complexParameter(
			@Name("values") final Map<String, List<Double>> values) {
		for (final String key : values.keySet()) {
			final List<Double> value = values.get(key);
			for (final Double v : value) {
				System.out.println(key + " " + v);
			}
		}
	}
	
	public Map<String, List<Double>> complexResult() {
		System.err.println("ComplexResult called!");
		final Map<String, List<Double>> result = new HashMap<String, List<Double>>();
		final List<Double> list = new ArrayList<Double>();
		list.add(1.1);
		list.add(0.4);
		result.put("result", list);
		
		return result;
	}
	
	public Double testTFComplexResult(@Name("url") final String url) throws Exception {
		final TypeFactory tf = JOM.getTypeFactory();
		final Map<String, List<Double>> res = send(URI.create(url), "complexResult",
				JOM.createObjectNode(), tf.constructMapType(HashMap.class, JOM
						.getTypeFactory().constructType(String.class),
						tf.constructCollectionType(List.class,
								Double.class)));
		return res.get("result").get(0);
	}
	
	public Double testComplexResult(@Name("url") final String url) throws Exception {
		
		final Map<String, List<Double>> res = send(URI.create(url), "complexResult",
				new TypeUtil<Map<String, List<Double>>>() {
				});
		return res.get("result").get(0);
	}
	
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
	
	public Double testDoubleRet() throws Exception {
		return send(getFirstUrl(), "increment", Double.class);
	}
	
	public String get(@Name("key") final String key) {
		return getState().get(key, String.class);
	}
	
	public void put(@Name("key") final String key, @Name("value") final String value) {
		getState().put(key, value);
	}
	
	public void registerPingEvent() throws Exception {
		getEventsFactory().subscribe(getMyUrl(), "ping", "pingCallback");
	}
	
	public void unregisterPingEvent() throws Exception {
		getEventsFactory().subscribe(getMyUrl(), "ping", "pingCallback");
	}
	
	public void pingCallback(@Name("params") final ObjectNode params) {
		System.out.println("pingCallback " + getId() + " " + params.toString());
	}
	
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
	
	public void cancelTask(@Name("id") final String id) {
		getScheduler().cancelTask(id);
	}
	
	public String createTask(@Name("delay") final long delay) throws Exception {
		final ObjectNode params = JOM.createObjectNode();
		params.put("message", "hello world");
		final JSONRequest request = new JSONRequest("myTask", params);
		final String id = getScheduler().createTask(request, delay);
		return id;
	}
	
	public void myTask(@Name("message") final String message) throws Exception {
		final ObjectNode params = JOM.createObjectNode();
		params.put("message", message);
		getEventsFactory().trigger("task", params);
		System.out.println("myTask is executed. Message: " + message);
	}
	
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
	
	public String testSendNonExistingMethod() throws Exception {
		final String res = send(
				URI.create("http://localhost:8080/EveCore/agents/chatagent/1/"),
				"nonExistingMethod", String.class);
		System.out.println(res);
		return res;
	}
	
	public String subscribeToAgent(@Optional @Name("url") String url)
			throws Exception {
		if (url == null) {
			url = "http://localhost:8080/agents/testagent2/";
		}
		final String event = "dataChanged";
		final String callback = "onEvent";
		return getEventsFactory().subscribe(URI.create(url), event, callback);
	}
	
	public void unsubscribeFromAgent(@Optional @Name("url") String url,
			@Name("subscriptionId") final String subscriptionId) throws Exception {
		if (url == null) {
			url = "http://localhost:8080/agents/testagent2/";
		}
		// String event = "dataChanged";
		// String callback = "onEvent";
		getEventsFactory().unsubscribe(URI.create(url), subscriptionId);
	}
	
	public void triggerDataChanged() throws Exception {
		getEventsFactory().trigger("dataChanged", null);
	}
	
	public Object getEverything() {
		return getState();
	}
	
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
	
	private String privateMethod() {
		return "You should not be able to execute this method via JSON-RPC! "
				+ "It is private.";
	}
	
	// multiple methods with the same name
	public void methodVersionOne() {
		privateMethod();
	}
	
	public void methodVersionOne(@Name("param") final String param) {
		privateMethod();
	}
	
	public String invalidMethod(@Name("param1") final String param1, final int param2) {
		return "This method is no valid JSON-RPC method: misses an @Name annotation.";
	}
	
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
	
	public void xmppDisconnect() throws Exception {
		final AgentHost host = getAgentHost();
		final XmppService service = (XmppService) host.getTransportService("xmpp");
		if (service != null) {
			service.disconnect(getId());
		} else {
			throw new Exception("No XMPP service registered");
		}
	}
	
	public void deleteMySelf() throws Exception {
		getAgentHost().deleteAgent(getId());
	}
	
	public Double testAgentProxy() {
		final String url = "http://eveagents.appspot.com/agents/testagent/1/";
		final Test2AgentInterface other = createAgentProxy(URI.create(url),
				Test2AgentInterface.class);
		final Double value = other.increment();
		return value;
	}
	
	public Double testAgentProxy2() {
		final String url = "http://eveagents.appspot.com/agents/testagent/1/";
		final Test2AgentInterface other = createAgentProxy(URI.create(url),
				Test2AgentInterface.class);
		
		final Double value = other.multiply(2.3, 4.5);
		return value;
	}
	
	public List<Object> testAgentProxy3() {
		final String url = "http://eveagents.appspot.com/agents/testagent/1/";
		final Test2AgentInterface other = createAgentProxy(URI.create(url),
				Test2AgentInterface.class);
		
		final List<Object> value = other.getMethods();
		return value;
	}
	
	public Double testAgentProxy4() {
		final String url = "http://eveagents.appspot.com/agents/testagent/1/";
		
		final Test2AgentInterface other = createAgentProxy(URI.create(url),
				Test2AgentInterface.class);
		
		final Double value = other.add(2.3, null);
		return value;
	}
	
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
	
	public ArrayNode getUrlsOfMerlin() throws Exception {
		final String url = "xmpp:merlin@openid.almende.org";
		final ArrayNode urls = send(URI.create(url), "getUrls",
				JOM.createObjectNode(), ArrayNode.class);
		System.out.println("merlins urls=" + urls);
		return urls;
	}
	
	public ArrayNode getUrlsOfJos() throws Exception {
		final String url = "xmpp:jos@openid.almende.org";
		final ArrayNode urls = send(URI.create(url), "getUrls",
				JOM.createObjectNode(), ArrayNode.class);
		System.out.println("jos's urls=" + urls);
		return urls;
	}
	
	public ArrayNode getListOfMerlin() throws Exception {
		final String url = "xmpp:merlin@openid.almende.org";
		final ArrayNode list = send(URI.create(url), "list", JOM.createObjectNode(),
				ArrayNode.class);
		System.out.println("merlins list=" + list);
		return list;
	}
	
	@Override
	public String getVersion() {
		return "1.0";
	}
	
	@Override
	public String getDescription() {
		return "This agent can be used for test purposes. "
				+ "It contains a simple ping method.";
	}
}
