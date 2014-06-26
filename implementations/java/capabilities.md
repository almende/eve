---
layout: default
title: Capabilities
---


# Capabilities

Eve implements agents as a collection of capabilities. Please refer to the [Agent section](/implementations/java/agents.html) for an overview how these capabilities can form agents. 

This section provides an overview of the available capabilities, and their purpose:

- [Capability](#Capabilities): This part describes the generic capability model as implemented by all other capabilities
- [Transports](#TransportCapabilities): Providing an asynchronous, string-based communication capability
- [Transforms](#TransformCapabilities): Providing message transformation and call handling capability
- [States](#StateCapabilities): Providing persistent state storage, in the form of a key-value store
- [Scheduling](#SchedulerCapabilities): Providing the ability to receive calls at scheduled future moments
- [Lifecycle](#LifecycleCapabilities): Provide capabilities for booting and suspending agents

See image below for the highlevel interaction between the capabilities.

<img src="../../img/eve_java_architecture.png"
  style="margin-top: 30px;width:90%;margin-left:auto;margin-right:auto;display:block"
  title="Eve agent services infograph">

## Capability model {#Capabilities}

All capabilities follow the same usage pattern, implementing the same interface. Obtaining an specific instance of a capability is done through a builder pattern. This is a three step process:

1. Create a configuration for the capability
2. Define a callback method for the capability
3. Build a capabilityBuilder with the configuration and the callback

### Configuration format

All capabilities accept a Jackson JSON DOM node as configuration.This allows very flexible configuration and allows the agent to serialize its configuration. It also allows the configuration to be obtained from file in various file formats (JSON, Yaml, etc.) For most capabilities there is a one-on-one mapping from JSON config to specific capability instance. This means if you build a capability with exactly the same configuration, you most probably get the same capability instance again.

For most capabilities there is a specific config class, extending Jackson's JsonNode, which you can use to programatically set the various configuration fields. (=bean-like interface)

### Callback method

Several capabilities need to be able to invoke a callback method on the agent. For example, an incoming message needs to be able to invoke a receive method in the agent. The capability model provides the possibility to provide such a callback method to the capability. This callback is wrapped in a handler object, to allow the agent to be suspended. (see [lifecycle](#LifecycleCapabilities) for more information about suspension) Depending on the capability category, the callback method needs to implement a specific interface. (e.g. Receiver for Transports) 

### Capability builder

All capabilities have their own builder. This allows compile-time selection of the capability. But for full data driven setups, in which it's not yet known which capability will be used during runtime, it is possible to use the generic CapabilityBuilder. This builder uses a "class" field in the configuration to dynamically select the needed capability.

## Transport capabilities {#TransportCapabilities}

Eve agents can communicate via various transports. Eve currently has four built-in transport protocol implementations: Http, Websockets, Xmpp and ZeroMQ.

- [Http transport](#HttpTransport) allows agents to contact each other through a HTTP client.
  Agents can be invoked by sending a HTTP POST request via a regular Java servlet.
- [Websocket transport](#WSTransport) allows agents to contact one specific other agent through a websocket connection. One agent uses the server side transport, the other the client side. 
- [Xmpp transport](#XmppTransport) allows to connect agents to an XMPP server. The agents can then send messages to each other through XMPP.
- [Zmq transport](#ZmqTransport) allows agents to contact each other through ZMQ PUSH/PULL sockets.

A single Eve agent can have multiple Transports configured, each with its own URL structure.
This allows exposure of the agents via multiple transports at the same time. This also means each agent has multiple addresses as well.

### Http transport {#HttpTransport}

The agent needs to initialise the HttpCapability through the HttpTransportBuilder. This capability acts like an adapter to a servlet which implements the HTTP endpoint. This servlet needs to be setup separately from the HttpCapability, although in the embedded setup this is done in one configuration action.

Eve comes with a servlet *EveServlet* which exposes agents via a standard Java servlet. A specific agent can be addressed via this servlet by specifying
its id in the servlet url in a RESTFull manner: &lt;baseUrl&gt;/&lt;id&gt;, e.g. "http://example.com/agents/myAgentId".

#### Configuration

There are two ways to setup the servlet environment:

- Through an embedded Jetty setup, configured through the normal CapabilityConfig
- Configuration through web.xml, besides the normal CapabilityConfig

##### Generic configuration

{% highlight java %}
	#Setup the configuration:
	final HttpTransportConfig config = new HttpTransportConfig();
	config.setServletUrl("http://localhost:8080/agents/");
	config.setId("testAgent");

	#Build the transport:
	final Transport transport = 
		new TransportBuilder()
		.withConfig(config)
		.withHandle(new myReceiver())
		.build();

{% endhighlight %}

##### Servlet configuration with embedded Jetty

This configuration is very similar to the above setup, except that some more configuration is added to configure the servlet. This setup requires that the embedded Jetty is bundled

{% highlight java %}
	#Setup the configuration:
	final HttpTransportConfig config = new HttpTransportConfig();
	config.setServletUrl("http://localhost:8080/agents/");
	config.setId("testAgent");
	
	#Add a servlet launcher to the http config:
	config.setServletLauncher("JettyLauncher");

	#Add Jetty specific configuration to the http config:
	final ObjectNode jettyParms = JOM.createObjectNode();
	jettyParms.put("port", 8080);
	config.put("jetty", jettyParms);

	#Build the transport:
	final Transport transport = 
		new TransportBuilder()
		.withConfig(config)
		.withHandle(new myReceiver())
		.build();
{% endhighlight %}

The equivalent Json configuration is:
{% highlight json %}
	{
		"class":"com.almende.eve.transport.http.HttpTransportBuilder",
		"servletUrl":"http://localhost:8080/agents/",
		"id":"testAgent",
		"servletLauncher":"JettyLauncher",
		"jetty":{
			"port":8080,
		}
	}
{% endhighlight %}


##### Servlet configuration through Web.xml

When running Eve in an external servlet environment like in Jetty or Tomcat, the servlet needs to be configured in the web.xml:

To configure the servlet add the following lines to the **web.xml** file of the Java project,
inside the &lt;web-app&gt; tag:

{% highlight xml %}
	<servlet>
		<servlet-name>war</servlet-name>
		<servlet-class>com.almende.eve.transport.http.EveServlet</servlet-class>
		<init-param>
			<param-name>ServletUrl</param-name>
			<param-value>http://localhost:8080/war/agents/</param-value>
		</init-param>
		<load-on-startup>1</load-on-startup>
	</servlet>

	<servlet-mapping>
		<servlet-name>war</servlet-name>
		<url-pattern>/agents/*</url-pattern>
	</servlet-mapping>
{% endhighlight %}

The *url-pattern* in the servlet mapping can be freely chosen (in the example
chosen as `/agents/*`). This determines the url at which the servlet is running.
It is important to end the url with the pattern /\*,
as the url of the servlet will end with the id of the agent. Together with the hostname of the server, this url-pattern forms the servlet base URL, which needs to be provided to the servlet, through the ServletUrl init parameter. This parameter is used by the servlet to lookup the HttpTransportCapabilities of the configured agents.
It is therefor important that the ServletUrl parameter is equal to the ServletUrl parameter in the HttpTransportConfig instance that is given to the HttpTransportBuilder.

#### Debug servlet

Besides the EveServlet, there is also a debug servlet available, which exposes a simpel debugging GUI when GET-ing the agent url. This debug servlet is implemented in class: com.almende.eve.transport.http.DebugServlet. Just replace EveServlet with DebugServlet in the above mentioned configuration.

### Xmpp transport {#XmppTransport}

Agents can be connected individually to an XMPP server through the Xmpp transport capability. Each agent can be addressed through the given JabberID. (e.g. xmpp:agent@example.com/endpoint)

#### Configuration

{% highlight java %}
	final XmppTransportConfig params = new XmppTransportConfig();
	params.setAddress("xmpp://alice@example.com/example");
	params.setPassword("wonderland");
		
	final Transport transport = 
		new XmppTransportBuilder()
		.withConfig(params)
		.withHandle(new MyReceiver()).build();

	#Connect to the server
	transport.connect();
		
	#Send some data to the other end
	transport.send(URI.create("xmpp:bob@example.com"),"Hello World", null);
	

	#Disconnect again if required
	transport.disconnect();

{% endhighlight %}

The equivalent Json configuration is:
{% highlight json %}
	{
		"class":"com.almende.eve.transport.xmpp.XmppTransportBuilder",
		"address":"xmpp://alice@example.com/example",
		"password":"wonderland"
	}
{% endhighlight %}

#### Usage

An agent can be connected to an XMPP server programmatically via the configured
Xmpp transport. The following code example shows how an agent can retrieve the
xmpp service via its AgentHost,
and connect itself to the service with a username and password.

{% highlight java %}
@Access(AccessType.PUBLIC)
public void xmppConnect(@Name("username") String username,
	@Name("password") String password) throws Exception {
	AgentHost host = getAgentHost();
	XmppService service = (XmppService) host.getService("xmpp");
	if (service != null) {
		service.connect(getId(), username, password);
	} else {
		throw new Exception("No XMPP service registered");
	}
}

@Access(AccessType.PUBLIC)
public void xmppDisconnect() throws Exception {
	AgentHost host = getAgentHost();
	XmppService service = (XmppService) host.getService("xmpp");
	if (service != null) {
		service.disconnect(getId());
	} else {
		throw new Exception("No XMPP service registered");
	}
}
{% endhighlight %}

### Zmq transport {#ZmqTransport}

<div class="Evehighlight">
<span>Linux 64-bit only!</span><br>
As it stands, the current implementation of ZMQ sockets in Eve depends Maven artifacts only available for Linux 64-bit architectures.
</div>
Agents can also be provided with ZeroMQ sockets. Eve supports all three types of ZeroMQ addresses: TCP sockets, IPC sockets and inproc sockets. When the agentHost is configured for ZeroMQ, each agent is provided with an inbound PULL socket at the configured address. Each outbound call will instantiate a PUSH socket which pairs with the remote PULL socket.

#### Configuration

{% highlight java %}

{% endhighlight %}

The equivalent Json configuration is:
{% highlight json %}
	{
	}
{% endhighlight %}

With the above mentioned configuration each agent will get three different ZMQ sockets assigned with the following addresses:

- A TCP address of form:  **tcp://{address}:{basePort+agentOffset}** (e.g.  tcp://127.0.0.1:5447 for the third agent in the system)
- A local socket in the form of: **ipc:///tmp/zmq-socket-{agentId}**
- A inproc socket in the form of: **inproc://{agentId}**

For routing to these addresses from within an agent a zmq: prefix needs to be added (as reported through agent.getUrls()). (e.g. zmq:ipc:///tmp/zmq-socket-testAgent1)

## State Capabilities {#StateCapabilities}

Currently Eve offers the choice between four different state storage services, of which only one can be active for a given VM at the same time.The available state services are:

- In-memory state
- JSON based file state
- Java object serialization file state
- CouchDB state

Within the codebase of Eve these State services are provided through a configured StateFactory.

### Configuration

To configure the state factory one of the below shown configurations can be used in **eve.yaml**. You\'ll need to modify the parameters somewhat to match your local settings, especially in the CouchDB case. As mentioned: only one state factory can be used per application!

{% highlight yaml %}

# State settings: Choose only one!
# memory state:
state:
  class: MemoryStateFactory

# Java serialization file state:
state:
  class: FileStateFactory
  path: .eveagents

# JSON based file state:
state:
  class: FileStateFactory
  path: .eveagents
  json: true

# CouchDB state:
state:
  class: CouchdbStateFactory
  url: http://localhost:5984
  database: eve
  username: eve_user
  password: eve_passwd

{% endhighlight %}

### Usage

Each agent can reach it\'s State through the getState() method (in the Agent.java superclass). State acts similar to a Java collections Map&lt;String,Object&gt;, but with a few distinct differences. The biggest difference if that the state can be serialized to JSON (for persistency) which potentially loses type information on the value. This means that the methods for getting the value need to reinject this type information. 

There is a normal put(key,value) method for placing data in the state, overwriting potential existing values. Similarly there are normal remove(key) and containsKey(key) methods. However, other methods are not provided, most notably entrySet() and values().

As mentioned, the getter methods need to reinject the missing type information, as can be seen in the get(key, type) methods:

{% highlight java %}

<T> T get(String key, Class<T> type);
<T> T get(String key, Type type);
<T> T get(String key, JavaType type);
<T> T get(String key, TypeUtil<T> type);
<T> T get(TypedKey<T> key);

{% endhighlight %}

These 5 methods each given a different way for putting type information back into the object. These methods actually reflect the same set of options that the JSON-RPC library also offers on it's send() methods. 

#### Optimistic locking

Eve agents normally have a thread per method call, which means that state operations need to be coordinated. Because its not guaranteed that each thread operates on the same agent object instance, it is not possible to use normal java synchronisation tooling.(and we would not advice workarounds to get to that behaviour) However, the state offers some distinct tooling for concurrency handling, based on optimistic locking. This is based on the atomic putIfUnchanged() method:

{% highlight java %}

boolean putIfUnchanged(String key, Object newVal, Object oldVal);

{% endhighlight %}

This method is normally used in the following manner:

{% highlight java %}

public void incr(key){

	int oldval = getState().get(key, Integer.class);
	int newval = oldval + 1;
	if (!getState().putIfUnchanged(key, newval, oldval)){
		//recursive retry:
		incr(key);
	}
}

{% endhighlight %}

Basically you get the current value, make a copy which you modify. Next step you store the value again, but with a check that no other thread has just modified the same value, in which case you just retry the operation.

## Scheduling Capabilities {#SchedulerCapabilities}

To facilitate the autonomous behavior of the software agents, Eve offers each agent a scheduler service. The scheduler can call agent methods after a given delay, possibly repetitive. Currently there are two scheduler services available:

- RunnableSchedulerFactory  - A basic scheduler that keeps a list of all scheduled tasks for all agents in the system. Offers a pretty precise scheduling (&lt;10ms delays) but is not very scalable, it\'s performance degrades significantly at around 100 tasks per second.
- ClockSchedulerFactory  - A more scalable design, which stores the tasks in the agents state. Because the data is now distributed among the agents, it is more scalable, but at a latency price. Currently this scheduler has delays in the 80-100ms range for normal tasks, but doesn\'t degrade at scale.

### Configuration

{% highlight yaml %}
# Use one of the two options below:

# scheduler settings
scheduler:
  class: RunnableSchedulerFactory
  id: _runnableScheduler

scheduler:
  class: ClockSchedulerFactory

{% endhighlight %}

The \"id\" option of the RunnableSchedulerFactory depicts the agentname the scheduler will use. This shown name \"_runnableScheduler\" is the default, which will be used if the option is omitted.

### Usage

Each agent is offered a getScheduler() method. (through the Agent.java superclass). The scheduler object that is returned has a createTask() method that does the actual scheduling:

{% highlight java %}

	String createTask(JSONRequest request, long delay);
	String createTask(JSONRequest request, long delay, boolean repeat, boolean sequential);

{% endhighlight %}

The former is uses the latter, with both optional parameters at their default false. The parameters have the following effect:

- JSONRequest request  - The method (with it\'s parameters) which needs to be called at the scheduled moment. This needs to be accessible through JSON-RPC at a minimal accessType of AccessType.SELF. (AccessType.UNAVAILABLE (which is the default) is not callable from the scheduler)
- long delay  - The schedule delay in milliseconds from now. 
- boolean repeat  - Should the task be repeated multiple times, at *delay* intervals?
- boolean sequential - When repeating the task, may multiple instances run in parallel? When given the *true* value, the next schedule round waits until the earlier execution has finished before scheduling the next execution. (at delay interval after the finish) If this parameter has a value of *false* the next iteration will be scheduled directly from the start of the current round, allowing the next to run in parallel if the execution takes longer than the delay.


