---
layout: default
title: Configuration
---


# Configuration

Eve needs a configuration file containing settings for persistency of the agents
context, settings for communication services such as HTTP and XMPP, and other
environment settings. The servlet used to host the Eve agents points to an Eve
configuration file, as explained on the page [Services](java_services.html).

The configuration file is a [YAML](http://en.wikipedia.org/wiki/YAML) file.
The file can have any name and is normally located in the same folder as
web.xml, typically war/WEB-INF. Default file name is eve.yaml. Note that the
field names are case sensitive.

file: **war/WEB-INF/eve.yaml**

    # Eve configuration

    # environment specific settings
    environment:
      Development:
        # communication services
        services:
        - class: HttpService
          servlet_url: http://localhost:8888/agents/

        auth_google_servlet_url: http://localhost:8888/auth/google

      Production:
        # communication services
        services:
        - class: HttpService
          servlet_url: http://my_server.com/agents/

        auth_google_servlet_url: http://my_server.com/auth/google

    # environment independent communication services
    services:
    - class: XmppService
      host: my_xmpp_server.com
      port: 5222

    # context settings
    # the context is used by agents for storing their state.
    context:
      class: FileContextFactory
      path: .eveagents

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
            environment.Production<br>
        </td>
        <td>
            The Eve configuration supports environment specific settings.
            There are two environments available <code>Development</code> and
            <code>Production</code>.
            The environment is determined at runtime
            and can be retrieved from the AgentFactory and ContextFactory using the
            method <code>getEnvironment()</code>.<br>
            <br>
            All Eve settings can be placed both in the root of the configuration
            file as well as under a specific environment.

            <!-- TODO: move this text
              The servlet url of the agents. This url needs to be specified,
              as it is not possible for an agent to know via what servlet it is being
              called. The url of an agent is built up by the servlet url, its class,
              and its id.<br><br>
              For example, when servlet_url is
              <code>http://myproject.appspot.com/agents</code>, the agents class is
              <code>EchoAgent</code>, and the agent has id <code>100</code>, the
              agents url will be
              <code>http://myproject.appspot.com/agents/echoagent/1/</code>.
              -->
        </td>
    </tr>
    <tr>
        <td>services</td>
        <td>
        To communicate with Eve agents, one or multiple communication services
        can be configured. An agent can be accessed via each of these
        communication services. The available urls of an agent can be retrieved
        via <code>getUrls</code>.

        The following services are available:

        <h4>HttpService</h4>
        Allows communication with agents via HTTP. All agents will be accessible
        via a single servlet.

        Configuration parameters:
        <p></p>
        <table>
            <tr>
                <th>Name</th>
                <th>Description</th>
            </tr>
            <tr>
                <td>class</td>
                <td>Must be <code>HttpService</code></td>
            </tr>
            <tr>
                <td>servlet_url</td>
                <td>
                The servlet url of the agents. This url needs to be specified,
                as it is not possible for an agent to know via what servlet it is being
                called. The url of an agent is built up by the servlet url and its id.<br><br>
                For example, when servlet_url is
                <code>http://myproject.appspot.com/agents</code>,
                and the agent has id <code>100</code>, the agents url will be
                <code>http://myproject.appspot.com/agents/100/</code>.
                .</td>
            </tr>
        </table>
        <p></p>

        <h4>XmppService</h4>
        Allows communication of agents via XMPP. Each agent needs to have an account.
        Configuration parameters:
        <p></p>
        <table>
            <tr>
                <th>Name</th>
                <th>Description</th>
            </tr>
            <tr>
                <td>class</td>
                <td>Must be <code>XmppService</code></td>
            </tr>
            <tr>
                <td>host</td>
                <td>
                Connection host, for example jabber.com.</td>
            </tr>
            <tr>
                <td>port</td>
                <td>Connection port The XMPP host, 5222 by default.</td>
            </tr>
            <tr>
                <td>serviceName</td>
                <td>Optional service name for the connection.</td>
            </tr>
        </table>
        <p></p>

        </td>
    </tr>
    <tr>
        <td>context</td>
        <td>
            Configuration for the context, used to persist the agents state.
            An object containing parameters:

            <p></p>
            <table>
                <tr>
                    <th>Name</th>
                    <th>Description</th>
                </tr>
                <tr>
                    <td>class</td>
                    <td>The full class path of an ContextFactory.
                    For built-in context factories, it is enough to specify
                    the classes simple name instead of the full path.</td>
                </tr>
                <tr>
                    <td>path</td>
                    <td>The path on disk where the agents state will be stored.
                        Only applicable for the <code>FileContextFactory</code>.</td>
                </tr>
            </table>
            <p></p>

            The following context factories are available:

            <ul>
                <li><code>FileContextFactory</code>.
                    Located in eve-core.jar.
                    Not applicable when deployed on Google App Engine.</li>
                <li><code>MemoryContextFactory</code>.
                    Located in eve-core.jar.</li>
                <li><code>DatastoreContextFactory</code>.
                    Located in eve-google-appengine.jar.
                    Only applicable when the application is deployed on Google App Engine.
                </li>
            </ul>
        </td>
    </tr>
    <tr>
        <td>auth_google_servlet_url</td>
        <td>The url where the authentication servlet GoogleAuth is hosted.
            Only required when using the GoogleAuth servlet provided by Eve Planning
            (<code>eve-planning.jar</code>).</td>
    </tr>
    <tr>
        <td>google</td>
        <td>
            Parameters for API access to Googles services.
            These parameters are provided when registering an application in the
            <a href="https://code.google.com/apis/console/">Google APIs Console</a>.
            Required when using the GoogleCalendarAgent and other google agents
            provided by Eve Planning.

            Contains the following parameters:
            <p></p>
            <table>
                <tr>
                    <th>Name</th>
                    <th>Description</th>
                </tr>
                <tr>
                    <td>client_id</td>
                    <td>Client id of the application.</td>
                </tr>
                <tr>
                    <td>client_secret</td>
                    <td>Client secret of the application.</td>
                </tr>
            </table>
            <p></p>
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

    # Eve configuration

    # environment specific settings
    environment:
      Development:
        services:
        - class: HttpService
          servlet_url: http://localhost:8888/agents/
      Production:
        services:
        - class: HttpService
          servlet_url: http://myproject.appspot.com/agents/

    # context settings
    # the context is used by agents for storing their state.
    context:
      class: DatastoreContextFactory



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

    # Eve configuration

    services:
    # communication services
    services:
    - class: HttpService
      servlet_url: http://localhost:8080/MyProject/agents/
    - class: XmppService
      host: my_xmpp_server.com
      port: 5222

    # context settings
    # the context is used by agents for storing their state.
    context:
      class: com.almende.eve.context.FileContextFactory
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

    # environment specific settings
    environment:
      Development:
        services:
        - class: HttpService
          servlet_url: http://localhost:8888/agents/
        auth_google_servlet_url: http://localhost:8888/auth/google
      Production:
        services:
        - class: HttpService
          servlet_url: http://myproject.appspot.com/agents/
        auth_google_servlet_url: http://myproject.appspot.com/auth/google

    # context settings
    # the context is used by agents for storing their state.
    context:
      class: DatastoreContextFactory

    # Google API access
    google:
      client_id: xxxxxxxxxxxxxxxx.apps.googleusercontent.com
      client_secret: xxxxxxxxxxxxxxxx
