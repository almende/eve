---
layout: default
title: Configuration
---


# Configuration

Eve needs a configuration file containing information on the agent classes that
needs to be hosted, settings for storage of agent states, and some environment
settings. The servlet used to host the Eve agents points to an Eve
configuration file, as explained on the page [Hosting](java_hosting.html).

The configuration file is a [YAML](http://en.wikipedia.org/wiki/YAML) file.
The file can have any name and is normally located in the same folder as
web.xml, typically war/WEB-INF. Default file name is eve.yaml. Note that the
field names are case sensitive.

file: **war/WEB-INF/eve.yaml**

    # Eve settings

    # environment settings
    environment:
      Development:
        agent_url: http://localhost:8888/agents/:class/:id/:resource
        auth_google_servlet_url: http://localhost:8888/auth/google
      Production:
        agent_url: http://myproject.appspot.com/agents/:class/:id/:resource
        auth_google_servlet_url: http://myproject.appspot.com/auth/google

    # agent settings
    agents:
      - class: com.almende.eve.agent.example.EchoAgent
        stateful: false
        thread-safe: true
      - class: com.almende.eve.agent.example.CalcAgent
        stateful: false
        thread-safe: true
      - class: com.almende.eve.agent.example.ChatAgent
        stateful: false
        thread-safe: true

    # context settings
    # the context is used by agents for storing their state.
    context:
      class: com.almende.eve.context.MemoryContext

    # Google API access
    google:
      client_id: xxxxxxxxxxxxxxxx.apps.googleusercontent.com
      client_secret: xxxxxxxxxxxxxxxx


Description of the available properties:

<table>
  <tr>
    <th>Name</th>
    <th>Description</th>
  </tr>
  <tr>
    <td>environment.Development<br>
      &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;.agent_url<br>
      environment.Production<br>
      &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;.agent_url</td>
    <td>The url template for the hosted agents. This url needs to be specified,
      as it is not possible for an agent to know via what servlet it is being
      called. The url of an agent is built up from this template url, where the
      agents class and id are filled in.<br><br>

      The url template can contain the following parameters:
      <ul>
        <li><code>:class</code> The class name of the agent
            (lowercase, simple name, not full class path).</li>
        <li><code>:id</code> The id of the agent.</li>
        <li><code>:resource</code> A resource of the agents web application.</li>
      </ul>

      For example, when the configured agent_url is
      <code>http://myproject.appspot.com/agents/:class/:id/:resource</code>,
      the agents class is <code>EchoAgent</code>,
      and the agent has id <code>100</code>, the
      agents url will be
      <code>http://myproject.appspot.com/agents/echoagent/1/</code>.
      </td>
  </tr>
  <tr>
    <td>environment.Development<br>
      &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;.servlet_url<br>
      environment.Production<br>
      &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;.servlet_url</td>
    <td>
      <span style="font-style:italic;">Deprecated. Use agent_url instead.</span>
      <br><br>

      The servlet url of the agents. This url needs to be specified,
      as it is not possible for an agent to know via what servlet it is being
      called. The url of an agent is built up by the servlet url, its class,
      and its id.<br><br>
      For example, when servlet_url is
      <code>http://myproject.appspot.com/agents</code>, the agents class is
      <code>EchoAgent</code>, and the agent has id <code>100</code>, the
      agents url will be
      <code>http://myproject.appspot.com/agents/echoagent/1/</code>.
      </td>
  </tr>
  <tr>
   <td>environment.Development<br>
      &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;.auth_google_servlet_url<br>
      environment.Production<br>
      &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;.auth_google_servlet_url
   </td>
    <td>The url where the authentication servlet GoogleAuth is hosted.
    Only required when using the GoogleAuth servlet provided by Eve Planning
    (<code>eve-planning.jar</code>).</td>
  </tr>
  <tr>
    <td>agents[]</td>
    <td>A list with properties of the agent classes to be hosted.</td>
  </tr>
  <tr>
    <td>agents[].class</td>
    <td>The full class path of the agent. Required.</td>
  </tr>
  <tr>
    <td>agents[].stateful</td>
    <td>A boolean specifying whether the agent is stateful or stateless.
      This property is optional, and is false by default.
      When stateful is true, the agent must be marked thread_safe as well.
      <br><br>
      When an agent is marked stateful, it will be loaded once and kept running,
      as opposed to stateless agents which can be loaded and unloaded any time.
      Making an agent stateful allows for keeping connections to services such
      as messaging open.
      <br><br>
      Stateful agents are not supported on Google App Engine.
      </td>
  </tr>
  <tr>
    <td>agents[].thread_safe</td>
    <td>A boolean specifying whether the agent is thread-safe.
      This property is optional, and is false by default.
      When thread-safe, the agent is instantiated once will be kept in memory
      until it has been inactive for some time, or until the system needs to
      free memory.
      When not thread-safe, a new instance of the agent created for every
      request, and is destroyed after the request has been handled.
      <br><br>
      The <i>thread_safe</i> property allows Eve to optimize the application
      by keeping agents in memory as much as possible, instead of having to
      create and destroy the agents with every request. The property does not
      guarantee a one-off instantiated, continuously running agent though,
      use the property <i>stateful</i> for that purpose instead.
      </td>
  </tr>
  <tr>
    <td>context.class</td>
    <td>
      A Context class. The context is used to store the agents state.
      Available contexts:
      <ul>
        <li><code>com.almende.eve.context.FileContext</code>.
            Located in eve-core.jar.
            Not applicable when deployed on Google App Engine.</li>
        <li><code>com.almende.eve.context.MemoryContext</code>.
            Located in eve-core.jar.</li>
        <li><code>com.almende.eve.context.google.DatastoreContext</code>.
            Located in eve-google-appengine.jar.
            Only applicable when the application is deployed on Google App Engine.
        </li>
      </ul>
    </td>
  </tr>
  <tr>
    <td>context.path</td>
    <td>
      The path on disk where the agents state will be stored.
      Only applicable when context.class is
      <code>com.almende.eve.context.FileContext</code>.
    </td>
  </tr>
  <tr>
    <td>google.client_id</td>
    <td>
      Client id of the application as registered in the
      <a href="https://code.google.com/apis/console/">Google APIs Console</a>.
      Required when using the GoogleCalendarAgent and other google agents
      provided by Eve Planning.
    </td>
  </tr>
  <tr>
    <td>google.client_secret</td>
    <td>
      Client secret of the application as registered in the
      <a href="https://code.google.com/apis/console/">Google APIs Console</a>.
      Required when using the GoogleCalendarAgent and other google agents
      provided by Eve Planning.
    </td>
  </tr>
</table>





## Accessing configuration properties {#accessing_configuration_properties}

All configuration properties can be accessed by the agents via their
[context](java_agents.html#context). If an agent needs specific properties,
for example some database configuration, these properties can be stored in the
configuration file of Eve.

For example, to
[access Google services](https://developers.google.com/accounts/docs/OAuth2),
one typically needs to have a `client_id` and `client_secret`.
These properties can be stored in the configuration file:

    # Eve settings
    ...

    # Google API access
    google:
      client_id: xxxxxxxxxxxxxxxx.apps.googleusercontent.com
      client_secret: xxxxxxxxxxxxxxxx

The properties can be retrieved by an agent via its context

    void authorizeGoogleApis () {
        // retrieve properties
        Config config = getContext().getConfig();
        String client_id = config.get('google', 'client_id');
        String client_secret = config.get('google', 'client_secret');

        // ... access google services
    }


## Examples {#examples}

This section gives a number of examples of different Eve setups:

- [Google App Engine](#google_app_engine_configuration)
- [Tomcat] (#tomcat_configuration)
- [Eve Planning] (#eve_planning_configuration)


### Google App Engine configuration {#google_app_engine_configuration}

An Eve setup running on
[Google App Engine](https://developers.google.com/appengine/) requires the
libraries `eve-core.jar` and `eve-google-appengine.jar`.
There are two environments available:

- `Development`, which is used when running an App Engine project locally in
  development mode.
- `Production`, which is used when the project is deployed on App Engine.

There is one context available on Google App Engine: `DatastoreContext`,
which uses Google Datastore to persist the state of the agents. The Datastore
context does not need any additional configuration.

Example file: **war/WEB-INF/eve.yaml**

    # Eve settings

    # environment settings
    environment:
      Development:
        agent_url: http://localhost:8888/agents/:class/:id/:resource
      Production:
        agent_url: http://myproject.appspot.com/agents/:class/:id/:resource

    # agent settings
    agent:
      classes:
      - com.almende.eve.agent.example.EchoAgent
      - com.almende.eve.agent.example.CalcAgent
      - com.almende.eve.agent.example.ChatAgent

    # context settings
    # the context is used by agents for storing their state.
    context:
      class: com.almende.eve.context.google.DatastoreContext



### Tomcat configuration {#tomcat_configuration}

An Eve setup running on [Tomcat](http://tomcat.apache.org/) requires only the
library `eve-core.jar`.
On Tomcat, there is currently only a `Production` environment available (no
`Development` as available on Google App Engine). There are two types of
context available for storing the agents state: `FileContext` and
`MemoryContext`.
In case of `FileContext`, each agent stores its state in a single file
in the configured path.

Example file: **war/WEB-INF/eve.yaml**

    # Eve settings

    # environment settings
    environment:
      Production:
        agent_url: http://localhost:8080/MyProject/agents/:class/:id/:resource

    # agent settings
    agent:
      classes:
      - com.almende.eve.agent.example.EchoAgent
      - com.almende.eve.agent.example.CalcAgent
      - com.almende.eve.agent.example.ChatAgent

    # context settings
    # the context is used by agents for storing their state.
    context:
      class: com.almende.eve.context.FileContext
      path: .eveagents



### Eve Planning configuration {#eve_planning_configuration}

The library Eve Planning (`eve-planning.jar`) comes with agents which interact
with services from Google. There is some additional configuration needed to
access these services.

Eve Planning comes with an additional servlet to handle user authentication:
`com.almende.eve.servlet.google.GoogleAuth`. This servlet requires:

- the url `auth_google_servlet_url` for each environment, pointing to the url
  where the authentication servlet `GoogleAuth` is hosted.
- a `client_id` and `client_secret` defined under `google`, in order to be able
  to get access to the Google APIs. A client id and secret can be retrieved
  via the [Google APIs Console](https://code.google.com/apis/console/).

Example file: **war/WEB-INF/eve.yaml**

    # Eve settings

    # environment settings
    environment:
      Development:
        agent_url: http://localhost:8888/agents/:class/:id/:resource
        auth_google_servlet_url: http://localhost:8888/auth/google
      Production:
        agent_url: http://myproject.appspot.com/agents/:class/:id/:resource
        auth_google_servlet_url: http://myproject.appspot.com/auth/google

    # agent settings
    ...

    # context settings
    ...

    # Google API access
    google:
      client_id: xxxxxxxxxxxxxxxx.apps.googleusercontent.com
      client_secret: xxxxxxxxxxxxxxxx
