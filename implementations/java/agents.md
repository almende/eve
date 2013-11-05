---
layout: default
title: Agents
---

# Agents

An Eve agent is created as a regular Java class. 
The class must inherit from the base class Agent. 
Public methods will be made available via [JSON-RPC](protocol.html).
The methods must have named parameters.
An agent can be accessed via a servlet or via an xmpp server
(see [Services](services.html)), depending on the Eve configuration.

The Java code of a basic Eve agent looks as follows:

{% highlight java %}
import com.almende.eve.agent.Agent;
import com.almende.eve.rpc.annotation.Access;
import com.almende.eve.rpc.annotation.AccessType;
import com.almende.eve.rpc.annotation.Name;

public class HelloWorldAgent extends Agent {
   @Access(AccessType.PUBLIC)
   public String welcome(@Name("name") String name) {
       return "Hello " + name + "!";
   }
}
{% endhighlight %}

Remarks on this example:

- An Eve agent extends from the base class Agent.
- The Example agent implements a method welcome, which takes one String
  "name" as parameter. Each method can have multiple parameters of any type,
  and the parameters must be named using the annotation `@Name`.
  This is needed because the JSON-RPC 2.0 protocol uses named parameters,
  and unfortunately it is not possible in Java to retrieve the parameter names
  automatically via reflection.
- By default, none of the agents methods are available externally. To make them
  accessible via JSON-RPC, they must be marked public using the annotation
  `@Access(AccessType.PUBLIC)`.


## Methods {#methods}

Eve agents communicate with each other via JSON-RPC 2.0. 
All public methods of an Eve agent are automatically exposed via JSON-RPC.

All agents automatically inherit the following methods from the base class Agent:

- `getMethods` returns an automatically generated list with all available methods.
- `getType` returns the class name of the agent.
- `getUrls` returns the full url of the agent.
- `getId` returns the id of the agent
- `onSubscribe` to recevie a subscription to an event
- `onUnsubscribe` to receive an unsubscription from an event

An agent can optionally override the following utility methods:

- `getDescription` returning a textual description of the agent.
- `getVersion` returning the version number of the agent.

The parameters of a method must be named using the `@Name` annotation.
Parameters can be marked as optional using the annotation `@Required`. 
Non-required parameters are initialized with value `null` when not provided.
Parameters can be of any type, both primitive types like Double or String, 
and Java objects such as a Contact or Person class.

{% highlight java %}
@Access(AccessType.PUBLIC)
public void storePerson (@Name("person") Person person,
    @Name("confirm") @Required(false) Boolean confirm ) {
    // ...
}
{% endhighlight %}

There is a special annotation to retrieve the url of the sender, `@Sender`.
This url can for example be used for authorization purposes.
The sender url is currently only provided when communication via XMPP,
not via HTTP. In the case of HTTP, the @Sender parameter will be null.

{% highlight java %}
@Access(AccessType.PUBLIC)
public String echo (@Name("message") String message, @Sender String senderUrl) {
    if (sender != null) {
       return "Hello " + senderUrl + ", you said: " + message;
    } else {
       return "Who are you?";
    }
}
{% endhighlight %}

## Life cycle {#life_cycle}

Eve agents are stateless.
An agent can have multiple instances running simultaneously.
The agents have a [shared state](#state) where they can persist data.
The amount of work which can be done by one agent is thus not limited to the
limitations of one physical server, but can be scaled endlessly over multiple
instances running on different servers in the cloud.

An Eve agent has the following life cycle events:
create, init, invoke, destroy, and delete.

### Create and Delete

Once in its lifetime, an agent is created via the AgentHost.
On creation of the agent, the method `create()` is called once.
This method can be overridden to perform setup tasks for the agent.

At the end of its life an agent is deleted via the AgentHost.
Before deletion, the method `delete()` is called once, which can be overridden
to cleanup the agent.

### Init and Destroy

When an agent is loaded into memory, it is instantiated.
On instantiation, the method `init()` is called, which can be overridden.
Similarly, when an agents instance is destroyed, the method `destroy()` is
called, which can be overridden too.

Initialization and destruction of an agents instances is managed by the
AgentHost. Depending on cache settings, agents may be kept in memory,
or may be destroyed at any time. An agent can also be instantiated multiple
times.
It is possible that for every incoming request a new instance of the agent
is loaded in memory, and destroyed again when done.
Therefore, it is important to design the agents in a robust, stateless manner.

### Invoke

During its life, an agent can be invoked by externals.
All methods of the agent which have named parameters and are public can be
invoked by external agent or system.
See also the sections [Methods](#methods) and  [Requests](#requests).


## Requests {#requests}

An agent can call an other agent using the methods `send` or `sendAsync`,
or by creating a proxy to an agent and invoke methods on the proxy.

### Synchronous request {#synchronous_request}
A synchronous call to an agent is executed using the method `send`,
providing a url, method, parameters, and return type.

{% highlight java %}
String url = "http://myserver.com/agents/mycalcagent/";
String method = "eval";
ObjectNode params = JOM.createObjectNode();
params.put("expr", "Sin(0.25 * pi) ^ 2");
String result = send(url, method, params, String.class);
System.out.println("result=" + result);
{% endhighlight %}


### Asynchronous request: {#asynchronous_request}
An asynchronous request is executed using the method `sendAsync`,
providing a url, method, parameters, and a callback.

{% highlight java %}
String url = "xmpp:mycalcagent@myxmppserver.com";
String method = "getDurationHuman";
String method = "eval";
ObjectNode params = JOM.createObjectNode();
params.put("expr", "Sin(0.25 * pi) ^ 2");

sendAsync(url, method, params, new AsyncCallback<String>() {
   @Override
   public void onSuccess(String result) {
       System.out.println("result=" + result);
   }
   @Override
   public void onFailure(Throwable caught) {
      caught.printStackTrace();
   }
}, String.class);
{% endhighlight %}

### Agent Proxy {#proxy}

If there is a Java interface available of the agent to be invoked, this
interface can be used to create a proxy to the agent.
Behind the scenes, the proxy executes a regular `send` request.
The proxy stays valid as long as the agents url is valid, and the interface
matches the agents actual features.

The interface must extend the interface `AgentInterface`. For example:

{% highlight java %}
import com.almende.eve.agent.AgentInterface;
import com.almende.eve.agent.annotation.Name;

public interface CalcAgent extends AgentInterface {
   @Access(AccessType.PUBLIC)
   public Double eval(@Name("expr") String expr);
}
{% endhighlight %}


To create a proxy to an agent using this interface:

{% highlight java %}
String url = "http://myserver.com/agents/mycalcagent/";
CalcAgent calcAgent = getAgentHost().createAgentProxy(url, CalcAgent.class);

String result = calcAgent.eval("Sin(0.25 * pi) ^ 2");
System.out.println("result=" + result);
{% endhighlight %}


## State {#state}

Each agent has a State available which can be used to persist the agents state.
The State offers an interface which is independent of the platform where the
agent is deployed, and is shared amongst all running instances of the agent.
The State can be accessed via the method `getState`, and offers a simple
key/value storage. 

The State is meant for storing a limited amount of state parameters,
and not as complete database solution. For storing large amounts of data,
a database natively available to the agents should be used. For example when 
running on Google App Engine, an agent can use the Google Datastore. When running 
on Amazon Elastic Cloud, Amazons SimpleDB or DynamoDB can be used.


An example of using the state is shown in the following example:

{% highlight java %}
@Access(AccessType.PUBLIC)
public void setUsername(@Name("username") String username) {
	getState().put("username", username);
}
@Access(AccessType.PUBLIC)
public String getUsername() {
	return getState().get("username");
}
{% endhighlight %}

## Events {#events}

Agents can subscribe on events from other agent.
They will be triggered when the event occurs.

To subscribe AgentX to an event from AgentY, the method `subscribe` can be used.
This method is called with three parameters: the url of the agent
to subscribe to, the name of the event, and the callback method on which to
retrieve a callback when the event is triggered. Optionally the subscription can also contain a callback parameter object, which will be send to the callback method. Similarly, an agent can use the `unsubscribe` method to remove a subscription. Behind the scenes, AgentX will make an JSON-RPC call to the `onSubscribe` or
`onUnsubscribe` methods of AgentY, providing its own url and the event parameters.

An agent can subscribe to a single event using the event name,
or subscribe to all events by specifying a star `*` as event name.

In the example below, AgentX subscribes to the event `dataChanged`, and wants
to receive a callback on its method `onEvent` when the event happens.
The callback method `onEvent` can have any name, and must have three parameters:
`agent`, `event`, and `params`.

{% highlight java %}
@Access(AccessType.PUBLIC)
public void subscribeToAgentY() throws Exception {
    String url = "http://server/agents/agenty/";
    String event = "dataChanged";
    String callback = "onEvent";
	ObjectNode callbackParams = JOM.createObjectNode();
    callbackParams.put("original_data","some data");
	callbackParams.put("message","Somewhere the dataChanged event was triggered.");
    getEventsFactory().subscribe(url, event, callback, callbackParams);
}
@Access(AccessType.PUBLIC)
public void unsubscribeFromAgentY() throws Exception {
    String url = "http://server/agents/agenty/";
    String event = "dataChanged";
    String callback = "onEvent";
    getEventsFactory().unsubscribe(url, event, callback);
}
@Access(AccessType.PUBLIC)
public void onEvent(
    @Name("agent") String agent,
    @Name("event") String event, 
    @Required(false) @Name("params") ObjectNode params) throws Exception {
    System.out.println("onEvent " +
       "agent=" + agent + ", " +
       "event=" + event + ", " +
       "params=" + ((params != null) ? params.toString() : null));
}
{% endhighlight %}

To let AgentY trigger the event "dataChanged", the method `trigger` can be used.
Behind the scenes, a JSON-RPC call will be sent to all agents that have
subscribed to that particular event.

{% highlight java %}
@Access(AccessType.PUBLIC)
public void triggerDataChangedEvent () throws Exception {
   String event = "dataChanged";
   // optionally send extra parameters (can contain anything)
   ObjectNode params = JOM.createObjectNode();
   params.put("message", "Hi, I changed the data.");
   getEventsFactory().trigger(event, params);
}
{% endhighlight %}

The trigger method has an optional parameter object, which will be merged with the subscription callbackParameter object. (The trigger parameter fields will override existing subscription fields) The merged object will be send to the event callback method.

When the trigger above is executed, the callback method `onEvent` of AgentX will
be called with the following parameters:

- `agent` containing the url of AgentY, from which the trigger comes,
- `event` containing the triggered event "dataChanged",
- `params` containing optional parameters, a merged object from the subscription and the trigger, in this case an object containing
  a field "message" with value "Hi, I changed the data." (overriding the "message" field of the subscription) and a field "original_data" with value "some data".


## Scheduler {#scheduler}

Unlike some traditional agent platforms, Eve agents are not continuously running
as a thread on some server. An Eve agent must be triggered externally
to execute a task. An action can be triggered in different ways:

- An external agent makes a call.
- An event is triggered.
- A scheduled task is triggered.

The first two ways are events triggered externally and not by the agent itself.
An agent can schedule a task for itself using the built in Scheduler.
The Scheduler can be used to schedule a single task and repeating tasks.
An agent can access the scheduler via the method `getScheduler()`.

A task is a delayed JSON-RPC call to the agent itself. 
Tasks can be created and canceled.
The following example shows how to schedule a task:

{% highlight java %}
@Access(AccessType.PUBLIC)
public String createTask() throws Exception {
    ObjectNode params = JOM.createObjectNode();
    params.put("message", "hello world");
    JSONRequest request = new JSONRequest("myTask", params);
    long delay = 5000; // milliseconds 
    String id = getScheduler().createTask(request, delay);
    return id;
}
@Access(AccessType.PUBLIC)
public void cancelTask(@Name("id") String id) {
    getScheduler().cancelTask(id);
}
@Access(AccessType.PUBLIC)
public void myTask(@Name("message") String message) {
    System.out.println("myTask is executed. Message: " + message);
}
{% endhighlight %}


## AgentHost {#agenthost}

Eve agents are managed by an AgentHost. Via the AgentHost, an agent
can be created, deleted, and invoked. The AgentHost manages the communication
services, and all incoming and outgoing request go via the AgentHost.

Each agent has access to the AgentHost via the method `getAgentHost()`.
From the AgentHost, it is possible to

- Create an agent: `createAgent(id, class)`,
- Delete an agent: `deleteAgent(id)`,
- Test existance of an agent: `hasAgent(id)`,

The AgentHost also gives access to the configuration file, which enables
reading any configuration settings. If an agent requires some specific
configuration properties, these properties can be stored in the configuration
file (typically eve.yaml), and read by the agent:

{% highlight java %}
Config config = getAgentHost().getConfig();
String database_url = config.get("database_url");
{% endhighlight %}

See also the page
[Configuration](configuration.html#accessing_configuration_properties).


## Database {#database}

The Eve libraries do not include interfaces for data storage, 
besides the agents [State](#state).
To connect an agent to a database (such as the Google Datastore),
the regular libraries and interfaces for that specific database should be used.

