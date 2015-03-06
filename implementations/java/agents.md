---
layout: default
title: Agents
---

# Agents
As described in the [Concepts](/concepts/introduction.html) section, Eve agents are software entities that have a certain set of capabilities. Basically any normal Java class becomes an Eve agent, if it understands (and can transport) JSON-RPC requests, and can schedule such requests at some future time. Through these capabilities the agent can operate autonomous, and implement several agent design patterns.

The core purpose of Eve is to make it easier for software developers to work with software agents. To this purpose Eve provides an Agent class that can be extended by application developers. In some cases having to extend existing classes is not possible (because of legacy code, existing inheritence models, etc.) in which case it's possible to add the agent capabilities directly to any existing Java class. This is possible, because Eve agents consist entirely of glued together capabilities.

There are various ways to create Eve agents:

- [Extending Agent.java](#Agent) - A basic RPC agent, single state, single scheduler & multiple transports.
- [Custom POJO Agent](#Custom) - Create your own agent model, by cherrypicking the capabilities inside a normal Java class.

## Agent.java {#Agent}

An Eve agent is created as a regular Java class. The class must inherit from the base class Agent.
Public methods will be made available via [JSON-RPC](protocol.html). Such methods must have named parameters.
An agent can be accessed via a servlet or via an xmpp server (see [Services](services.html)), depending on the Eve configuration.

The Java code of a basic Eve agent looks as follows:

{% highlight java %}
import com.almende.eve.agent.Agent;
import com.almende.eve.transform.rpc.annotation.Access;
import com.almende.eve.transform.rpc.annotation.AccessType;
import com.almende.eve.transform.rpc.annotation.Name;

public class HelloWorldAgent extends Agent {
   @Access(AccessType.PUBLIC)
   public String welcome(@Name("yourName") String yourName) {
       return "Hello " + yourName + "!";
   }
}
{% endhighlight %}

Remarks on this example:

- An Eve agent extends from the base class Agent.
- The Example agent implements a method welcome, which takes one String
  "yourName" as parameter. Each method can have multiple parameters of any type,
  and the parameters must be named using the annotation `@Name`.
  This is needed because the JSON-RPC 2.0 protocol uses named parameters,
  and unfortunately it is not possible in Java to retrieve the parameter names
  automatically via reflection.
- By default, none of the agents methods are available externally. To make them
  accessible via JSON-RPC, they must be marked public using the annotation
  `@Access(AccessType.PUBLIC)`.

### Configuration {#configuration}

All configuration within Eve is done through Jackson JSON DOM structures. This offers highly flexible configuration strategies, but such flexibility comes at the price of having no clear code-level handle on the available options. Therefor Eve offers Config classes (extending the JSON DOM) which have setters and getters for the specific options. For example, at the Agent level there is an AgentConfig class, which contains a couple of agent specific setters: setId(), setScheduler(), setState(), etc. Each of these setters maps to a JSON field within the configuration. Each agent can get the JSON configuration structure through its getConfig() method.

There are various deployment scenarios for Eve agents, see [setups.html](setups.md) for more details. But all these scenarios offer some way to load the configuration of the agents from a configuration file (typically an eve.yaml file). This file just reflects the JSON structure of the various configuration elements. For example, it typically contains an "agents" object, which contains an list of agent configurations.

### Methods {#methods}

Eve agents communicate with each other via JSON-RPC 2.0.
All public methods of an Eve agent are automatically exposed via JSON-RPC.

All agents automatically inherit the following methods from the base class Agent:

- `getMethods` returns an automatically generated list with all available methods.
- `getType` returns the class name of the agent.
- `getUrls` returns the full urls of the agent.
- `getId` returns the id of the agent

Besides these methods, the agent also exposes some internal capabilities:

- `getConfig` return the JSON configuration of this agent
- `getScheduler` returns a reference to the configured scheduler
- `getState` provides access to the configured state

The parameters of a method must be named using the `@Name` annotation.
Parameters can be marked as optional using the annotation `@Optional`.
Non-required parameters are initialized with value `null` when not provided.
Parameters can be of any type, both primitive types like Double or String,
and Java objects such as a Contact or Person class.

{% highlight java %}
@Access(AccessType.PUBLIC)
public void storePerson (@Name("person") Person person,
    @Name("confirm") @Optional Boolean confirm ) {
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

### Requests {#requests}

An agent can call an other agent using the methods `call` or `callSync`,
or by creating a proxy to an agent and invoke methods on the proxy.

#### Synchronous request {#synchronous_request}
A synchronous call to an agent is executed using the method `callSync`,
providing a url, method and parameters..

{% highlight java %}
@Access(AccessType.PUBLIC)
public String evaluate () {
    URI url = URI.create("http://myserver.com/agents/mycalcagent/");
    String method = "eval";
    Params params = new Params();
    params.add("expr", "Sin(0.25 * pi) ^ 2");

    String result = callSync(url, method, params);
    System.out.println("result=" + result);
}
{% endhighlight %}

#### Asynchronous request: {#asynchronous_request}
An asynchronous request is executed using the method `call`,
providing a url, method, parameters, and a callback.

{% highlight java %}
public String evaluate () {
    URI url = URI.create("xmpp:mycalcagent@myxmppserver.com");
    String method = "eval";
    Params params = new Params();
    params.add("expr", "Sin(0.25 * pi) ^ 2");

    call(url, method, params, new AsyncCallback<String>() {
       @Override
       public void onSuccess(String result) {
           System.out.println("result=" + result);
       }

       @Override
       public void onFailure(Throwable caught) {
          caught.printStackTrace();
       }
    }, String.class);
}
{% endhighlight %}

#### Agent Proxy {#proxy}

If there is a Java interface available of the agent to be invoked, this
interface can be used to create a proxy to the agent.
Behind the scenes, the proxy executes a regular `callSync` request.
The proxy stays valid as long as the agents url is valid, and the interface
matches the agents actual features.

{% highlight java %}
import com.almende.eve.transform.rpc.annotation.Name;
import com.almende.eve.transform.rpc.annotation.Access;
import com.almende.eve.transform.rpc.annotation.AccessType;

public interface CalcAgent {
   @Access(AccessType.PUBLIC)
   public Double eval(@Name("expr") String expr);
}
{% endhighlight %}

To create a proxy to an agent using this interface:

{% highlight java %}
    //Create some sender agent:
    final Agent agent = new AgentBuilder().withConfig(<some config>).build();
    //Create the proxy:
    final ExampleAgentInterface proxy = agent.createAgentProxy(
        URI.create("http://localhost:8081/agents/calcExample"),
        CalcAgent.class);
    LOG.warning("Proxy got reply:" + proxy.eval("2+4"));
{% endhighlight %}


### State {#state}

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

### Scheduler {#scheduler}

Unlike some traditional agent platforms, Eve agents are not continuously running
as a thread on some server. An Eve agent must be triggered externally
to execute a task. An action can be triggered in different ways:

- An external agent makes a call.
- An event is triggered.
- A scheduled task is triggered.

The first two ways are events triggered externally and not by the agent itself.
An agent can schedule a task for itself using the built in Scheduler.
The Scheduler can be used to schedule a single incoming request, as a sort of task runner.

Tasks can be created and canceled.
The following example shows how to schedule a task:

{% highlight java %}
@Access(AccessType.PUBLIC)
public String createTask() throws Exception {
    Params params = new Params();
    params.add("message", "hello world");
    JSONRequest request = new JSONRequest("myTask", params);

    long delay = 5000; // milliseconds
    String id = schedule(request, delay);
    return id;
}

@Access(AccessType.PUBLIC)
public void cancelTask(@Name("id") String id) {
    cancelTask(id);
}

@Access(AccessType.PUBLIC)
public void myTask(@Name("message") String message) {
    System.out.println("myTask is executed. Message: " + message);
}
{% endhighlight %}

The mentioned methods "schedule()" is a simple wrapper around the "getScheduler().schedule()" method. (cancelTask() likewise)

### Hibernating Agents

By annotating the Agent with the @CanHibernate annotation, the agent gains the ability to be unloaded from memory when not in use. This is achieved by the InstantiationService as described in the [capabilities section](capabilities.html#LifecycleCapabilities).

The TestWake test in the code repository demonstrates through a WeakReference that the agent is actually unloaded from memory.

## Custom POJO Agent {#Custom}

Any Java class can become an Eve agent, by obtaining agent [capabilities](capabilities.html). The best way to get a feeling for how this works is by taking a look at the Agent.java source code in the git repository. In this section aspects of that base class are highlighted:

#### Basic data
Some capabilities need an identifier to distinguish between various instances. The agent class bundles these capabilities by offering them all the same id, called agentId in Agent.java.

The agent keeps track of its configuration in a field called "config". There is a loadConfig() method that is used to (re)initialize the capabilities based on this configuration. The configurations are idempotent with regard to the configuration, which means that you can initialize the capability multiple times with the same parameters. In all such cases the same instance is returned.

The default agent can only reference one State and one Scheduler implementation. This is not a limitation created by the capabilities themselves, but rather a simplification choice in the agent code. Custom POJO agents might choose to take a different approach, for example by having a persistent State besides a memory State, for different purposes.

#### Handling receival
Because the agent needs to be able to handle incoming messages, the agent implements the Receiver interface. These messages are handled by a RPCTransform, which targets the agent class itself as it's method repository. Because this agent doesn't need to be wakeable, a SimpleHandler is used to wrap the class.
{% highlight java %}

public class Agent implements Receiver {

   private RpcTransform rpc = new RpcTransformBuilder()
      .withHandle(new SimpleHandler<Object>(this))
      .build();
   private Handler<Receiver> receiver = new SimpleHandler<Receiver>(this);

   @Override
   public void receive(final Object msg, final URI senderUrl, final String tag) {
      final JSONResponse response = rpc.invoke(msg, senderUrl);
      if (response != null) {
         try {
            transport.send(senderUrl, response.toString(), tag);
         } catch (final IOException e) {
            LOG.log(Level.WARNING, "Couldn't send message", e);
         }
      }
   }
}
{% endhighlight %}

#### Handling sending

Similar to the receival of RPC messages, the agent code also handles the sending of RPC messages, through the call() methods. This method uses the same RPCTransform to create RPC messages, and to keep track of the callback of asyncronous calls. To handle multiple transports and select the correct one based on URL scheme, the agent has a special kind of transport called "Router", which has a register of other transports.

{% highlight java %}
   protected Router	transport	= new Router();

   protected <T> void call(final URI url, final String method,
         final ObjectNode params, final AsyncCallback<T> callback)
         throws IOException {
      transport.send(url, rpc.buildMsg(method, params, callback).toString(),
            null);
   }
{% endhighlight %}
