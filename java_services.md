---
layout: default
title: Services
---


# Services

Eve agents can be accessed via communication services.
Eve supports two services: HttpService and XmppService.

- [HttpService](#HttpService) exposes agents via a regular Java servlet.
  Agents can be invoked by sending a HTTP POST request to this servlet.
- [XmppService](#XmppService) allows to connect agents to an XMPP server.
  The agents can be invoked via XMPP.

A single Eve application can have multiple XmppServices and HttpServices configured.
This allows exposure of the agents via multiple communication services at the
same time. An agent can be accessible via both XMPP and HTTP at the same time.


## HttpService {#HttpService}

Eve comes with a servlet *AgentServlet* which exposes agents via a standard
Java servlet. A specific agent can be addressed via this servlet by specifying
its id in the servlet url.

To use the AgentServlet, the servlet must be configured in the web.xml file
of the Java project, and an Eve configuration file must be created.

### Configuration

#### Servlet configuration

An AgentServlet is a regular Java servlet.
To configure the servlet, add the following lines to the **web.xml** file of
the Java project, inside the &lt;web-app&gt; tag:

    <servlet>
        <servlet-name>AgentServlet</servlet-name>
        <servlet-class>com.almende.eve.service.http.AgentServlet</servlet-class>
        <init-param>
            <param-name>config</param-name>
            <param-value>eve.yaml</param-value>
        </init-param>
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

The servlet configuration can contain the following init parameters:

<p></p>
<table>
    <tr>
        <th>Name</th>
        <th>Description</th>
    </tr>
    <tr>
        <td>config</td>
        <td>
            The init-param `config` points to an eve configuration file
            (for example eve.yaml). The configuration file is used by the AgentFactory
            and contains configuration for the context, scheduler, and services.
        </td>
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



#### Eve configuration

The servlet configuration points to an Eve configuration file.
The configuration file is a [YAML](http://en.wikipedia.org/wiki/YAML) file.
The file can have any name and is normally located in the same folder as
web.xml, typically war/WEB-INF. Standard file name is **eve.yaml**.
Create a file eve.yaml and insert the following text:

    # Eve configuration

    # context settings (for persistency)
    context:
      class: FileContextFactory
      path: .eveagents

    # scheduler settings (for tasks)
    scheduler:
      class: AppEngineScheduler

The configuration contains:

<!-- TODO: cleanup
- A parameter *environment*.
  A project typically has two different environments:
  *Development* and *Production*. All Eve settings can be defined for a
  specific environment, or globally.

- Parameters *services*. One can define one or multiple communication services
  via which the agents can be accessed, for example HTTP and XMPP.
-->

- A parameter *context* specifying the type of context that will be
  available for the agents to read and write persistent data.
  Agents themselves are stateless. They can use a context to store data.

- A parameter *scheduler* specifying the scheduler that will be used to
  let agents schedule tasks for themselves.

Each agent has access has access to this configuration file via its
[context](java_agents.html#context).
If your agent needs specific settings (for example for database access),
you can add these settings to the configuration file.


### Usage

The AgentServlet supports the following request:

- `GET /agents/`

  Returns information on how to use this servlet.

- `GET /agents/{agentId}`

  Returns an agents web interface, allowing for easy interaction
  with the agent.
  A 404 error will be returned when the agent does not exist.

- `POST /agents/{agentId}`

  Send an RPC call to an agent.
  The body of the request must contain a JSON-RPC request.
  The addressed agent will execute the request and return a
  JSON-RPC response. This response can contain the result or
  an exception.
  A 404 error will be returned when the agent does not exist.

- `PUT /agents/{agentId}?class={agentClass}`

  Create an agent. `agentId` can be any string. `agentClass` must
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
      serviceName: my_xmpp_service_name

    # context settings (for persistency)
    context:
      class: FileContextFactory
      path: .eveagents

    # scheduler settings (for tasks)
    scheduler:
      class: RunnableScheduler

### Usage

An agent can be connected to an XMPP service programmatically via the configured
XmppService. The following code example shows how an agent can retrieve the
xmpp service via its [Context](java_agents.html#context),
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