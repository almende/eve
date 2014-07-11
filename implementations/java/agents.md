---
layout: default
title: Agents
---

# Agents
As described in the [Concepts](/concepts/introduction.html) section, Eve agents are software entities that have a certain set of capabilities. Basically a normal Java class becomes an Eve agent, if it understands (and can transport) JSON-RPC requests, and can schedule such requests at some future time. Through these capabilities the agent can operate autonomous, and implement several agent design patterns.

The core purpose of Eve is to make it easier for software developers to work with software agents. To this purpose Eve provides a set of Agent classes that can be extended by application developers. In some cases having to extend existing classes is not possible (because of legacy code, existing inheritence models, etc.) in which case it's possible to add the agent capabilities directly to any existing Java class. This is possible, because Eve agents consist entirely of glued together capabilities.

There are various ways to create Eve agents:

- [Extending Agent.java](#Agent) - A basic RPC agent, single state, single scheduler & multiple transports.
- [Extending WakeableAgent.java](#Wakeable) - An extention on top of Agent.java, in which the agent can be unloaded from memory and reloaded again on incoming traffic.
- [Custom POJO Agent](#Custom) - Create your own agent model, by cherrypicking the capabilities inside a normal Java class. 

## Agent.java {#Agent}

An Eve agent is created as a regular Java class. The class must inherit from the base class Agent. 
Public methods will be made available via [JSON-RPC](protocol.html).
The methods must have named parameters.
An agent can be accessed via a servlet or via an xmpp server
(see [Services](services.html)), depending on the Eve configuration.

The Java code of a basic Eve agent looks as follows:

{% highlight java %}
import com.almende.eve.agent.Agent;
import com.almende.eve.transform.rpc.annotation.Access;
import com.almende.eve.transform.rpc.annotation.AccessType;
import com.almende.eve.transform.rpc.annotation.Name;

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

### Configuration {#configuration}

The Agent class has two ways to be configured: Either through one of the constructors, or through the setConfig() method. In both cases the configuration consists of a [Jackson JSON DOM](configuration.html). This DOM contains the agent specific configuration and the configuration of all capabilities of the agent. To ease the configuration, an AgentConfig class is available, extending Jackson's ObjectNode, with setters and getters for the various capability configs. 



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
- `getRpc` returns a reference to the configured RPC transform

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

An agent can call an other agent using the methods `send` or `sendSync`,
or by creating a proxy to an agent and invoke methods on the proxy.

#### Synchronous request {#synchronous_request}
A synchronous call to an agent is executed using the method `sendSync`,
providing a url, method and parameters..

{% highlight java %}
@Access(AccessType.PUBLIC)
public String evaluate () {
    String url = "http://myserver.com/agents/mycalcagent/";
    String method = "eval";
    ObjectNode params = JOM.createObjectNode();
    params.put("expr", "Sin(0.25 * pi) ^ 2");
    String result = callSync(url, method, params);
    System.out.println("result=" + result);
}
{% endhighlight %}

#### Asynchronous request: {#asynchronous_request}
An asynchronous request is executed using the method `send`,
providing a url, method, parameters, and a callback.

{% highlight java %}
public String evaluate () {
    String url = "xmpp:mycalcagent@myxmppserver.com";
    String method = "getDurationHuman";
    String method = "eval";
    ObjectNode params = JOM.createObjectNode();
    params.put("expr", "Sin(0.25 * pi) ^ 2");
    
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
    ObjectNode params = JOM.createObjectNode();
    params.put("message", "hello world");
    JSONRequest request = getRpc().buildMsg("myTask", params);

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

## WakeableAgent.java {#Wakeable}

The Wakeable Agent is an extention of the normal Agent, therefor all above information holds also for the WakeableAgent. The difference between the Wakeable Agent and the normal agent lies in the ability of the wakeable to be unloaded from memory when not in use. This is achieved by the WakeService as described in the [capabilities section](capabilities.html#LifecycleCapabilities).

Usage of the WakeAgent is exactly the same as the normal agent, except for the initial configuration. Below is an example of instantiating a WakeAble agent: (Taken from the tests in the sources)

First we need a normal agent, but extending Wakeable agent. It requires a no-argument contructor.
{% highlight java %}
public class MyAgent extends WakeableAgent {
   
   public MyAgent() {};
   
   public MyAgent(final String id, final WakeService ws) {
      super(new AgentConfig(id), ws);
   }
   
   @Access(AccessType.PUBLIC)
   public String helloWorld() {
      return("Hello World");
   }
}
{% endhighlight %}

This agent can be used in the following manner:

{% highlight java %}
//First we need to setup the WakeService: (Either keep a global pointer to the wake service, or obtain it again through the same configuration)
final WakeServiceConfig config = new WakeServiceConfig();
final FileStateConfig stateconfig = new FileStateConfig();
stateconfig.setPath(".wakeservices");
stateconfig.setId("testWakeService");
config.setState(stateconfig);
      
final WakeService ws = 
   new WakeServiceBuilder()
   .withConfig(config)
   .build();

// Now create a WakeAble Agent
new MyAgent("testWakeAgent",ws);

//after a while the agent is unloaded:
System.gc();

//Now some other agent calls the agent:
new Agent("other",null){
   public void test(){
      call(new URI("local:testWakeAgent"),"helloWorld",null);
   }
}.test();

{% endhighlight %}

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
   protected Router			transport	= new Router();

   protected <T> void call(final URI url, final String method,
         final ObjectNode params, final AsyncCallback<T> callback)
         throws IOException {
      transport.send(url, rpc.buildMsg(method, params, callback).toString(),
            null);
   }
{% endhighlight %}



