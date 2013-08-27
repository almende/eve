---
layout: default
title: Configuration
---


# Configuration

Eve needs a configuration file containing settings for persistency of the agents
state, settings for transport services such as HTTP and XMPP, and other
environment settings. The servlet used to host the Eve agents points to an Eve
configuration file, as explained on the page [Services](services.html).

The configuration file is a [YAML](http://en.wikipedia.org/wiki/YAML) file.
The file can have any name and is normally located in the same folder as
web.xml, typically war/WEB-INF. Default file name is eve.yaml. Note that the
field names are case sensitive.

file: **war/WEB-INF/eve.yaml**

    # Eve configuration

    # environment specific settings
    environment:
      Development:
        auth_google_servlet_url: http://localhost:8888/auth/google
      Production:
        auth_google_servlet_url: http://my_server.com/auth/google

    # environment independent transport services
    services:
    - class: XmppService
      host: my_xmpp_server.com
      port: 5222

    # state settings (for persistency)
    state:
      class: FileStateFactory
      path: .eveagents

    # scheduler settings (for tasks)
    scheduler:
      class: RunnableSchedulerFactory

    # bootstrap agents
    # agents will be automatically created on system startup (if not existing)
    bootstrap:
      agents:
        calc1: com.almende.eve.agent.example.CalcAgent
        echo1: com.almende.eve.agent.example.EchoAgent

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
        <td>bootstrap.agents</td>
        <td>
            A map with ids as keys, and class paths as value.
            Allows to configure a set of agents to be created automatically
            on system startup if not existing.
        </td>
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
            and can be retrieved from the AgentFactory and StateFactory using the
            method <code>getEnvironment()</code>.<br>
            <br>
            All Eve settings can be placed both in the root of the configuration
            file as well as under a specific environment.
        </td>
    </tr>
    <tr>
        <td>services</td>
        <td>
        To communicate with Eve agents, one or multiple transport services
        can be configured. An agent will get an url for each of the configured
        services, which can be retrieved via <code>getUrls</code>.

        The following services are available:

        <h4>HttpService</h4>
        Allows communication with agents via HTTP.
        All agents will be accessible via a servlet, for example the AgentServlet.
        The servlet initiates and registers an HttpService itself,
        there is no need to configure an HttpService in eve.yaml. See
        <a href="services.html#HttpService">HttpService</a> for more details.

        <h4>XmppService</h4>
        Allows communication of agents via XMPP. Each agent needs to have an account.
        See <a href="services.html#XmppService">XmppService</a> for more details.

        Available configuration parameters:
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
                <td>service</td>
                <td>Service name for the connection.</td>
            </tr>
        </table>
        <p></p>

        </td>
    </tr>
    <tr>
        <td>state</td>
        <td>
            Configuration for the state, used to persist data for an agent.
            An object containing parameters:

            <p></p>
            <table>
                <tr>
                    <th>Name</th>
                    <th>Description</th>
                </tr>
                <tr>
                    <td>class</td>
                    <td>The full class path of an StateFactory.
                    For built-in state factories, it is enough to specify
                    the classes simple name instead of the full path.</td>
                </tr>
                <tr>
                    <td>path</td>
                    <td>The path on disk where the state of the agents will be stored.
                        Only applicable for the <code>FileStateFactory</code>.</td>
                </tr>
            </table>
            <p></p>

            The following state factories are available:

            <ul>
                <li><code>FileStateFactory</code>.
                    Located in eve-core.jar.
                    The FileStateFactory stores the state of each agent as a
                    file on disk. The files may only be used by a single Eve
                    application. Multiple Eve applications running on the same
                    machine must use a different `path`.
                    This state is not applicable when deployed on Google App Engine.</li>
                <li><code>MemoryStateFactory</code>.
                    Located in eve-core.jar.</li>
                <li><code>DatastoreStateFactory</code>.
                    Located in eve-gae.jar.
                    Only applicable when the application is deployed on Google App Engine.
                </li>
            </ul>
        </td>
    </tr>
    <tr>
        <td>scheduler</td>
        <td>
            Configuration for the scheduler, which allows agents to schedule
            tasks for themselves.
            An object containing parameters:

            <p></p>
            <table>
                <tr>
                    <th>Name</th>
                    <th>Description</th>
                </tr>
                <tr>
                    <td>class</td>
                    <td>The full class path of a Scheduler.
                    For built-in schedulers, it is enough to specify
                    the classes simple name instead of the full path.</td>
                </tr>
            </table>
            <p></p>

            The following scheduler factories are available:

            <ul>
                <li><code>RunnableSchedulerFactory</code>.
                    Located in eve-core.jar.
                    Not applicable when deployed on Google App Engine.</li>
                <li><code>AppEngineSchedulerFactory</code>.
                    Located in eve-gae.jar.
                    Only applicable when the application is deployed on
                    Google App Engine.</li>
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
agent factory. If an agent needs specific properties,
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

The properties can be retrieved by an agent via its agent factory:

    void authorizeGoogleApis () {
        // retrieve properties
        Config config = getAgentFactory().getConfig();
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
libraries `eve-core.jar` and `eve-gae.jar`.
There are two environments available:

- `Development`, which is used when running an App Engine project locally in
  development mode.
- `Production`, which is used when the project is deployed on App Engine.

There is one state factory available on Google App Engine:
`DatastoreStateFactory`, which uses Google Datastore to persist the state of
the agents. The Datastore state does not need any additional configuration.
There is one scheduler available: `AppEngineSchedulerFactory`.

Example file: **war/WEB-INF/web.inf**

    <?xml version="1.0" encoding="utf-8"?>
    <web-app xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
            xmlns="http://java.sun.com/xml/ns/javaee"
            xmlns:web="http://java.sun.com/xml/ns/javaee/web-app_2_5.xsd"
            xsi:schemaLocation="http://java.sun.com/xml/ns/javaee
            http://java.sun.com/xml/ns/javaee/web-app_2_5.xsd" version="2.5">

        <welcome-file-list>
            <welcome-file>index.html</welcome-file>
        </welcome-file-list>

        <context-param>
            <description>eve configuration (yaml file)</description>
            <param-name>config</param-name>
            <param-value>eve.yaml</param-value>
        </context-param>
        <context-param>
            <param-name>authentication</param-name>
            <param-value>false</param-value>
        </context-param>
        <listener>
            <listener-class>com.almende.eve.transport.http.AgentListener</listener-class>
        </listener>

        <servlet>
            <servlet-name>AgentServlet</servlet-name>
            <servlet-class>com.almende.eve.transport.http.AgentServlet</servlet-class>
            <init-param>
                <param-name>environment.Development.servlet_url</param-name>
                <param-value>http://localhost:8888/agents</param-value>
            </init-param>
            <init-param>
                <param-name>environment.Production.servlet_url</param-name>
                <param-value>http://eveagents.appspot.com/agents</param-value>
            </init-param>
        </servlet>
        <servlet-mapping>
            <servlet-name>AgentServlet</servlet-name>
            <url-pattern>/agents/*</url-pattern>
        </servlet-mapping>
    </web-app>

Example file: **war/WEB-INF/eve.yaml**

    # Eve configuration

    # state settings (for persistency)
    state:
      class: DatastoreStateFactory

    # scheduler settings (for tasks)
    scheduler:
      class: AppEngineSchedulerFactory



### Tomcat configuration {#tomcat_configuration}

An Eve setup running on [Tomcat](http://tomcat.apache.org/) requires only the
library `eve-core.jar`.
On Tomcat, there is currently only a `Production` environment available (no
`Development` as available on Google App Engine). There are two state factories
available for storing the agents state: `FileStateFactory` and
`MemoryStateFactory`.
In case of `FileStateFactory`, each agent stores its state in a single file
in the configured path.
There is one scheduler available: `RunnableSchedulerFactory`.


Example file: **war/WEB-INF/web.inf**

    <?xml version="1.0" encoding="utf-8"?>
    <web-app xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
            xmlns="http://java.sun.com/xml/ns/javaee"
            xmlns:web="http://java.sun.com/xml/ns/javaee/web-app_2_5.xsd"
            xsi:schemaLocation="http://java.sun.com/xml/ns/javaee
            http://java.sun.com/xml/ns/javaee/web-app_2_5.xsd" version="2.5">

        <welcome-file-list>
            <welcome-file>index.html</welcome-file>
        </welcome-file-list>

        <context-param>
            <description>eve configuration (yaml file)</description>
            <param-name>config</param-name>
            <param-value>eve.yaml</param-value>
        </context-param>
        <context-param>
            <param-name>authentication</param-name>
            <param-value>false</param-value>
        </context-param>
        <listener>
            <listener-class>com.almende.eve.transport.http.AgentListener</listener-class>
        </listener>

        <servlet>
            <servlet-name>AgentServlet</servlet-name>
            <servlet-class>com.almende.eve.transport.http.AgentServlet</servlet-class>
            <init-param>
                <param-name>servlet_url</param-name>
                <param-value>http://localhost:8888/MyProject/agents</param-value>
            </init-param>
        </servlet>
        <servlet-mapping>
            <servlet-name>AgentServlet</servlet-name>
            <url-pattern>/agents/*</url-pattern>
        </servlet-mapping>
    </web-app>

Example file: **war/WEB-INF/eve.yaml**

    # Eve configuration

    services:
    # communication services
    services:
    - class: XmppService
      host: my_xmpp_server.com
      port: 5222

    # state settings (for persistency)
    state:
      class: FileStateFactory
      path: .eveagents

    # scheduler settings (for tasks)
    scheduler:
      class: RunnableSchedulerFactory


### Eve Planning configuration {#eve_planning_configuration}

The library Eve Planning (`eve-planning.jar`) comes with agents which interact
with services from Google. There is some additional configuration needed to
access these services.

Eve Planning comes with an additional servlet to handle user authentication:
`com.almende.eve.servlet.google.GoogleAuth`. This servlet requires:

- The url `auth_google_servlet_url` for each environment, pointing to the url
  where the authentication servlet `GoogleAuth` is hosted.
- A `client_id` and `client_secret` defined under `google`, in order to be able
  to get access to the Google APIs. A client id and secret can be retrieved
  via the [Google APIs Console](https://code.google.com/apis/console/).

Example file: **war/WEB-INF/web.inf**

    <?xml version="1.0" encoding="utf-8"?>
    <web-app xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
            xmlns="http://java.sun.com/xml/ns/javaee"
            xmlns:web="http://java.sun.com/xml/ns/javaee/web-app_2_5.xsd"
            xsi:schemaLocation="http://java.sun.com/xml/ns/javaee
            http://java.sun.com/xml/ns/javaee/web-app_2_5.xsd" version="2.5">

        <welcome-file-list>
            <welcome-file>index.html</welcome-file>
        </welcome-file-list>

        <context-param>
            <description>eve configuration (yaml file)</description>
            <param-name>config</param-name>
            <param-value>eve.yaml</param-value>
        </context-param>
        <context-param>
            <param-name>authentication</param-name>
            <param-value>false</param-value>
        </context-param>
        <listener>
            <listener-class>com.almende.eve.transport.http.AgentListener</listener-class>
        </listener>

        <servlet>
            <servlet-name>AgentServlet</servlet-name>
            <servlet-class>com.almende.eve.transport.http.AgentServlet</servlet-class>
            <init-param>
                <param-name>environment.Development.servlet_url</param-name>
                <param-value>http://localhost:8888/agents</param-value>
            </init-param>
            <init-param>
                <param-name>environment.Production.servlet_url</param-name>
                <param-value>http://eveagents.appspot.com/agents</param-value>
            </init-param>
        </servlet>
        <servlet-mapping>
            <servlet-name>AgentServlet</servlet-name>
            <url-pattern>/agents/*</url-pattern>
        </servlet-mapping>

        <servlet>
            <servlet-name>GoogleAuth</servlet-name>
            <servlet-class>com.almende.eve.servlet.google.GoogleAuth</servlet-class>
            <init-param>
            <param-name>config</param-name>
            <param-value>eve.yaml</param-value>
            </init-param>
        </servlet>
        <servlet-mapping>
            <servlet-name>GoogleAuth</servlet-name>
            <url-pattern>/auth/google</url-pattern>
        </servlet-mapping>
    </web-app>

Example file: **war/WEB-INF/eve.yaml**

    # Eve settings

    # environment specific settings
    environment:
      Development:
        auth_google_servlet_url: http://localhost:8888/auth/google
      Production:
        auth_google_servlet_url: http://myproject.appspot.com/auth/google

    # state settings
    # the state is used by agents for storing their state.
    state:
      class: DatastoreStateFactory

    # Google API access
    google:
      client_id: xxxxxxxxxxxxxxxx.apps.googleusercontent.com
      client_secret: xxxxxxxxxxxxxxxx
