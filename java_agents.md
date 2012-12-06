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
(see [Services](java_services.html)), depending on the Eve configuration.

The Java code of a basic Eve agent looks as follows:

    import com.almende.eve.agent.Agent;
    import com.almende.eve.json.annotation.Name;

    public class HelloWorldAgent extends Agent {
        public String welcome(@Name("name") String name) {
            return "Hello " + name + "!";
        }
        
        @Override
        public String getVersion() {
            return "0.1";
        }
        
        @Override
        public String getDescription() {
            return "This agent can do this and that for you.";
        }    
    }

Remarks on this example:

-  An Eve agent extends from the base class Agent.
-  Two methods are required to implement: getVersion and getDescription.
   Other methods, such as getMethods, getId, getUrl, are inherited from the 
   class Agent.
-  The Example agent implements a method welcome, which takes one String 
   "name" as parameter. Each method can have multiple parameters of any type,
   and the parameters must be named using the annotation @Name. 
   This is needed because the JSON-RPC 2.0 protocol uses named parameters, 
   and unfortunately it is not possible in Java to retrieve the parameter names 
   automatically via reflection.


## Methods {#methods}

Eve agents communicate with each other via JSON-RPC 2.0. 
All public methods of an Eve agent are automatically exposed via JSON-RPC.
An agent must implement the following methods:

- `getDescription` returning a textual description of the agent. 
- `getVersion` returning the version number of the agent.

All agents automatically inherit the following methods from the base class Agent:

- `getMethods` returns an automatically generated list with all available methods.
- `getType` returns the class name of the agent.
- `getUrl` returns the full url of the agent.
- `getId` returns the id of the agent
- `onSubscribe` to recevie a subscription to an event
- `onUnsubscribe` to receive an unsubscription from an event

The parameters of a method must be named using the `@Name` annotation.
Parameters can be marked as optional using the annotation `@Required`. 
Non-required parameters are initialized with value `null` when not provided.
Parameters can be of any type, both primitive types like Double or String, 
and Java objects such as a Contact or Person class.

    public void storePerson (@Name("person") Person person, 
            @Name("confirm") @Required(false) Boolean confirm ) {
        // ...
    }


## Instances {#instances}

Eve agents themselves are stateless. Every request, a new instance of the agent 
is loaded in memory, and destroyed again when done. Variables in the agents
class are not persisted. Instead, an agent can store its state using its 
[context](#context).

As the agents are stateless, it is possible to have multiple instances 
of the same agent running simultaneously. The amout of work which can be done
by one agent is thus not limited to the limitations of one physical server,
but can be scaled endlessly over more instances running on different servers
in the cloud. 
One interesting use for this is parallel processing of computationally 
intensive tasks.


## Context {#context}

Each agent has a Context available which can be used to persist the agents state.
The Context offers an interface which is independent of the platform where the 
agent is deployed, and is shared amongst all running instances of the agent.
The Context can be accessed via the method `getContext`, and offers a simple
key/value storage. 

The Context is meant for storing a limited amount of state parameters, 
and not as complete database solution. For storing large amounts of data,
a database natively available to the agents should be used. For example when 
running on Google App Engine, an agent can use the Google Datastore. When running 
on Amazon Elastic Cloud, Amazons SimpleDB or DynamoDB can be used.


An example of using the context is shown in the following example:

    public void setUsername(@Name("username") String username) {
        getContext().put("username", username);
    }
    
    public String getUsename() {
        return getContext().get("username");
    }

From the context, an agent has access to the [scheduler](#scheduler) via the 
method `getScheduler`:

    Scheduler scheduler = getContext().getScheduler();

The context offers agent specific information via the methods `getAgentUrl`, 
`getAgentId`, and `getAgentClass`. 
The context offers system information via the methods `getEnvironment`,
`getServletUrl` and `getConfig`. 

For example if an agent requires some specific configuration properties,
these properties can be stored in the configuration file (typically eve.yaml),
and read by the agent:

    Config config = getContext().getConfig();
    String database_url = config.get('database_url');

See also the page
[Configuration](java_configuration.html#accessing_configuration_properties).


## Events {#events}

Agents can subscribe on events from other agent.
They will be triggered when the event occurs.

To subscribe AgentX to an event from AgentY, the method `subscribe` can be used.
This method is called with three parameters: the url of the agent
to subscribe to, the name of the event, and the callback method on which to
retrieve a callback when the event is triggered. Similarly, an agent can use
the `unsubscribe` method to remove a subscription.
Behind the scenes, AgentX will make an JSON-RPC call to the `onSubscribe` or
`onUnsubscribe` methods of AgentY, providing its own url and the event parameters.

An agent can subscribe to a single event using the event name,
or subscribe to all events by specifying a star `*` as event name.

In the example below, AgentX subscribes to the event `dataChanged`, and wants
to receive a callback on its method `onEvent` when the event happens.
The callback method `onEvent` can have any name, and must have three parameters:
`agent`, `event`, and `params`.

    public void subscribeToAgentY() throws Exception {
        String url = "http://server/agents/agenttype/agenty";
        String event = "dataChanged";
        String callback = "onEvent";

        subscribe(url, event, callback);
    }

    public void unsubscribeFromAgentY() throws Exception {
        String url = "http://server/agents/agenttype/agenty";
        String event = "dataChanged";
        String callback = "onEvent";

        unsubscribe(url, event, callback);
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

To let AgentY trigger the event "dataChanged", the method `trigger` can be used.
Behind the scenes, a JSON-RPC call will be sent to all agents that have
subscribed to that particular event.

    public void triggerDataChangedEvent () throws Exception {
        String event = "dataChanged";

        // optionally send extra parameters (can contain anything)
        ObjectNode params = JOM.createObjectNode();
        params.put("message": "Hi, I changed the data.");

        trigger(event, params);
    }

When the trigger above is executed, the callback method `onEvent` of AgentX will
be called with the following parameters:

- `agent` containing the url of AgentY, from which the trigger comes,
- `event` containing the triggered event "dataChanged",
- `params` containing optional parameters, in this case an object containing
  a field "message" with value "Hi, I changed the data.".


## Scheduler {#scheduler}

Unlike some traditional agent platforms, Eve agents are not contiuously running
as a thread on some server. An Eve agent must be triggered externally
to execute a task. An action can be triggered in different ways:

- An external agent makes a call.
- An event is triggered.
- A scheduled task is triggered.

The first two ways are events triggered externally and not by the agent itself.
An agent can schedule a task for itself using the built in Scheduler.
The Scheduler can be used to schedule a single task and repeating tasks.
An agent can access the scheduler via its [Context](#context).

A task is a delayed JSON-RPC call to the agent itself. 
Tasks can be created and canceled.
The following example shows how to schedule a task:

    public String createTask() throws Exception {
        ObjectNode params = JOM.createObjectNode();
        params.put("message", "hello world");
        JSONRequest request = new JSONRequest("myTask", params);
        long delay = 5000; // milliseconds
        
        String id = getContext().getScheduler().createTask(request, delay);
        return id;
    }

    public void cancelTask(@Name("id") String id) {
        getContext().getScheduler().cancelTask(id);
    }

    public void myTask(@Name("message") String message) {
        System.out.println("myTask is executed. Message: " + message);
    }



## Database {#database}

The Eve libraries do not include interfaces for data storage, 
besides the context to store an agents state.
To connect an agent to a database (such as the Google Datastore),
the regular libraries and interfaces for that specific database should be used.

