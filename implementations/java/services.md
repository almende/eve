---
layout: default
title: Services
---


# Services

Eve agents can be accessed via various transport services.
Eve has two built-in transport services: HttpService and XmppService.

- [HttpService](#HttpService) exposes agents via a regular Java servlet.
  Agents can be invoked by sending a HTTP POST request to this servlet.
- [XmppService](#XmppService) allows to connect agents to an XMPP server.
  The agents can be invoked via XMPP.

A single Eve application can have multiple XmppServices and HttpServices configured.
This allows exposure of the agents via multiple transport services at the
same time. An agent can be accessible via both XMPP and HTTP at the same time.


## HttpService {#HttpService}

Eve comes with a servlet *AgentServlet* which exposes agents via a standard
Java servlet. A specific agent can be addressed via this servlet by specifying
its id in the servlet url.

To use the AgentServlet, the servlet must be configured in the web.xml file
of the Java project, and a context listener must be configured to start an
Eve AgentFactory.

### Configuration

#### Servlet configuration

When running Eve in a servlet environment like in Jetty or Tomcat, two
things needs to be configured:

- **AgentListener**
  A servlet context listener needs to be set up to load a singleton
  AgentFactory on startup of the web application.
  This AgentFactory manages all agents.
- **AgentServlet**
  At least one servlet needs to be set up to route incoming requests for agents.
  One can use the provided `AgentServlet` for this, or build something customized.
  An AgentServlet will automatically create an HttpService with its configured
  servlet url, and register this transport service to the AgentFactory.
  It is possible to configure multiple Agent servlets, and they will all share
  the same AgentFactory.

To configure the servlet and context listener,
add the following lines to the **web.xml** file of the Java project,
inside the &lt;web-app&gt; tag:

	<context-param>
		<description>eve configuration (yaml file)</description>
		<param-name>eve_config</param-name>
		<param-value>eve.yaml</param-value>
	</context-param>
	<context-param>
		<param-name>eve_authentication</param-name>
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
            <param-value>http://localhost:8888/agents/</param-value>
        </init-param>
        <init-param>
            <param-name>environment.Production.servlet_url</param-name>
            <param-value>http://myeveproject.appspot.com/agents/</param-value>
        </init-param>
    </servlet>
    <servlet-mapping>
        <servlet-name>AgentServlet</servlet-name>
        <url-pattern>/agents/*</url-pattern>
    </servlet-mapping>

The *url-pattern* in the servlet mapping can be freely chosen (in the example
chosen as `/agents/*`).
This determines the url at which the servlet is running.
It is important to end the url with the pattern /\*,
as the url of the servlet will end with the id of the agent.

The AgentListener supports the following context parameters:

<table>
    <tr>
        <th>Name</th>
        <th>Description</th>
    </tr>
    <tr>
        <td>eve_config</td>
        <td>
            The context-param <code>eve_config</code> points to an eve configuration file
            (for example eve.yaml). The configuration file is used by the AgentFactory
            and contains configuration for the state, scheduler, and services.
            The configuration of the AgentFactory is described on the page
            <a href="configuration.html">Configuration</a>.
        </td>
    </tr>
    <tr>
        <td>eve_authentication</td>
        <td>
            The parameter <code>eve_authentication</code> is a boolean and is
            <code>true</code> by default. When authentication is enabled,
            Eve uses SSL authentication to communicate between agents.
        </td>
    </tr>
</table>


The AgentServlet configuration can contain the following init parameters:

<table>
    <tr>
        <th>Name</th>
        <th>Description</th>
    </tr>
    <tr>
        <td>servlet_url</td>
        <td>
        The url of the servlet.
        This url is needed in order to be able to built an agents url.
        The url of an agent is built up by the servlet url and its id.
        For example, when servlet_url is
        <code>http://myserver.com/agents</code>,
        and the agent has id <code>100</code>, the agents url will be
        <code>http://myserver.com/agents/100/</code>.
        </td>
    </tr>
    <tr>
        <td>environment.Development.servlet_url</td>
        <td>
        The url of the servlet while running in development mode.
        This url will override *servlet_url* if specified.
        </td>
    </tr>
    <tr>
        <td>environment.Production.servlet_url</td>
        <td>
        The url of the servlet while running in production mode.
        This url will override *servlet_url* if specified.
        </td>
    </tr>

</table>
<p></p>


### Usage

The AgentServlet supports the following request:

- `GET /agents/`

  Returns information on how to use this servlet.

- `GET /agents/{agentId}`

  Returns an agents web interface, allowing easy interaction with the agent.
  A 404 error will be returned when the agent does not exist.

- `POST /agents/{agentId}`

  Send an RPC call to an agent.
  The body of the request must contain a JSON-RPC request.
  The addressed agent will execute the request and return a
  JSON-RPC response. This response can contain the result or
  an exception.
  A 404 error will be returned when the agent does not exist.

- `PUT /agents/{agentId}?type={agentType}`

  Create an agent. `agentId` can be any string. `agentType` must
  be a full java class path of an Agent. A 500 error will be
  thrown when an agent with this id already exists.

- `DELETE /agents/{agentId}`

  Delete an agent by its id.


### Custom servlets

If the AgentServlet do not fulfill your needs,
it is possible to develop a custom servlet.
This can for example be useful when exposing an existing service via a JSON-RPC
interface to the Eve world.
When creating a custom servlet, it is possible to get a better performance,
as the servlet can be integrated tightly with the service.
It is not necessary to have a real Eve agent running via the servlet,
the essence is that a service exposes a JSON-RPC interface to the outside world.


## XmppService {#XmppService}

Agents can be connected individually to an XMPP server.
In order to support XMPP, the application requires the
[Smack XMPP libraries](http://www.igniterealtime.org/projects/smack/)
*smack.jar* and *smackx.jar* to be included in the projects build path.

Note that XmppService is not supported on Google App Engine, as it requires
continuous connections to an XMPP server from one application instance,
while Google App Engine is based on stateless application instances which can
be started and stopped any moment.

### Configuration

XMPP support must be configured in the Eve configuration file with default
file name **eve.yaml**.

    # Eve configuration

    # communication services
    services:
    - class: XmppService
      host: my_xmpp_server.com
      port: 5222
      service: my_xmpp_service_name

    # state settings (for persistency)
    state:
      class: FileStateFactory
      path: .eveagents

    # scheduler settings (for tasks)
    scheduler:
      class: RunnableSchedulerFactory

### Usage

An agent can be connected to an XMPP service programmatically via the configured
XmppService. The following code example shows how an agent can retrieve the
xmpp service via its AgentFactory,
and connect itself to the service with a username and password.

    public void xmppConnect(@Name("username") String username,
            @Name("password") String password) throws Exception {
        AgentFactory factory = getAgentFactory();

        XmppService service = (XmppService) factory.getService("xmpp");
        if (service != null) {
            service.connect(getId(), username, password);
        }
        else {
            throw new Exception("No XMPP service registered");
        }
    }

    public void xmppDisconnect() throws Exception {
        AgentFactory factory = getAgentFactory();
        XmppService service = (XmppService) factory.getService("xmpp");
        if (service != null) {
            service.disconnect(getId());
        }
        else {
            throw new Exception("No XMPP service registered");
        }
    }