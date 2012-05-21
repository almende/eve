---
layout: default
title: Agents
---

# Agents

An Eve agent is created as a regular Java class. 
The class must inherit from the base class Agent. 
Public methods will be made available via [JSON-RPC](protocol.html).
The methods must have named parameters.
An agent is [hosted](java_hosting.html) via a servlet. 
To make a new agent class available via the servlet, 
the class must be added to the servlet configuration.

The Java code of a basic Eve agent looks as follows:

    import com.almende.eve.agent.Agent;
    import com.almende.eve.json.annotation.Name;

    public class ExampleAgent extends Agent {
        public Double welcome(@Name("name") String name) {
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
- `subscribe` to subscribe to an event
- `unsubscribe` to unsubscribe from an event 

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
        Context context = getContext();
        context.put("username", username);
    }
    
    public String getUsename() {
        Context context = getContext();
        if (context.has("username")) {
            return (String) context.get("username");
        }
        else {
            return null;
        }
    }

From the context, an agent has access to the [scheduler](#scheduler) via the 
method `getScheduler`:

    Scheduler scheduler = getContext().getScheduler();

The context offers agent specific information via the methods `getAgentUrl`, 
`getAgentId`, and `getAgentClass`. 
The context offers system information via the methods `getEnvironment`,
`getServletUrl` and `getConfig`. 

For example if an agent requires some specific settings, 
these settings can be stored in the configuration file (typically eve.yaml),
and read by the agent:

    Config config = getContext().getConfig();
    String database_url = config.get('database_url');


## Events {#events}

Agents can subscribe on events from other agent.
They will be triggered when the event occurs.

To subscribe AgentX to an event from AgentY, a JSON-RPC call is made to AgentY.
The method `subscribe` is called, with three parameters: the name of the event, 
a callback url containing the url of AgentX, and a callback method on which
AgentX wants to receive the triggered event.
In the example below, AgentX subscribes to the event `dataChanged`, and wants
to receive a triggered event on its method `onEvent`.

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

To let AgentY trigger the event `dataChanged`, the method `trigger` can be used.
This will send a JSON-RPC call to all agents that have subscribed to that event.

    trigger("dataChanged");


An agent can subscribe to a single event using the event name, 
or subscribe to all events by specifying a star `*` as event name.



## Scheduler {#scheduler}

Unlike some traditional agent platforms, Eve agents are not contiuously running
as a thread on some server. An Eve agent must be triggered externally
to execute a task. An action can be triggered in different ways:

- An external agent makes a call.
- An event is triggered.
- A scheduled task is triggered.

The first two ways are events triggered exernally and not by the agent itself. 
An agent can schedule a task for itself using the built in Scheduler.
The Scheduler can be used to schedule a single task and repeating tasks.
An agent can access the schedular via its [Context](#context).

A task is a delayed JSON-RPC call to the agent itself. 
Tasks can be created, listed, and canceled.
A task can be scheduled in the following way 

    public String createTask() throws Exception {
        String url = getUrl();
        ObjectNode params = JOM.createObjectNode();
        params.put("message", "hello world");
        JSONRequest request = new JSONRequest("myTask", params);
        long delay = 5000; // milliseconds
        
        Scheduler scheduler = getContext().getScheduler();
        String id = scheduler.setTimeout(url, request, delay);
        return id;
    }
    
    public void myTask(@Name("message") String message) {
        System.out.println("myTask is executed. Message: " + message);
    }



## Database {#database}

The Eve libraries do not include interfaces for data storage, 
besides the context to store an agents state.
To connect an agent to a database (such as the Google Datastore),
the regular libraries and interfaces for that specific database should be used.

