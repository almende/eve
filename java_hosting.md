---
layout: default
title: Hosting
---


# Hosting

In Java, Eve agents are hosted via a web servlet. 
Eve comes with two ready-made servlets, located in the eve-core.jar library. 

- [SingleAgentServlet](#SingleAgentServlet)
- [MultiAgentServlet](#MultiAgentServlet)

It is possible to create a [custom servlet](#custom) when the provided servlets
do not meet the requirements.



## SingleAgentServlet {#SingleAgentServlet}

The *SingleAgentServlet* will host a single agent which can be accessed 
directly via the servlet url (http://server/servlet).
To use a SingleAgentServlet, the servlet must be configured in the web.xml file
of the Java project, and an Eve configuration file must be created.

### Servlet configuration

The SingleAgentServlet is a regular Java servlet, and needs to be configured
in the **web.xml** file of the project (typically located at war/WEB-INF/).
Add the following lines to the web.xml file,
inside the &lt;web-app&gt; tag:

    <servlet>
      <servlet-name>SingleAgentServlet</servlet-name>
      <servlet-class>com.almende.eve.servlet.SingleAgentServlet</servlet-class>
      <init-param>
        <description>servlet configuration (yaml file)</description> 
        <param-name>config</param-name>
        <param-value>eve.yaml</param-value>
      </init-param>
    </servlet>
    <servlet-mapping>
      <servlet-name>SingleAgentServlet</servlet-name>
      <url-pattern>/testagent/*</url-pattern>
    </servlet-mapping>

The servlet configuration contains an init-param `config`, 
which must point to an Eve configuration file (for example eve.yaml).
It is possible to configure multiple servlets, and use a different 
configuration file for each of them.


### Eve configuration

The servlet configuration points to an Eve configuration file.
The configuration file is a [YAML](http://en.wikipedia.org/wiki/YAML) file.
Create a file named **eve.yaml** and put it in the folder war/WEB-INF 
(where web.xml is located too). Insert the following text:

    # Eve singleagent settings

    # environment settings
    environment:
      Development:
        servlet_url: http://localhost:8080/MyProject/testagent
      Production:
        servlet_url: http://MyServer/MyProject/testagent

    # agent settings
    # the SingleAgentServlet can only host one agent class.
    agent:
      classes:
      - com.almende.eve.agent.example.TestAgent

    # The context for reading and writing persistent data
    context:
      class: com.almende.eve.context.MemoryContextFactory


The configuration contains:

- A parameter *environment*. 
  A project typically has two different environments: 
  *Development* and *Production*.
  The parameter *servlet_url* defines the url of the agents. 
  This url needs to be specified, as it is not possible for an agent to know 
  via what servlet it is being called.

- A parameter *agent.classes* containing a list with the agent classes which 
  will be hosted by the servlet.
  Eve comes with a number of example agents, such as the CalcAgent and the EchoAgent,
  these agents can be used to test if the application runs correctly.

- A parameter *context.class* specifying the type of context that will be 
  available for the agents to read and write persistent data.
  Agents themselves are stateless. They can use a context to store data.

Each agent has access has access to this configuration file via its 
[context](java_agents.html#context).
If your agent needs specific settings (for example for database access), 
you can add these settings to the configuration file.



## MultiAgentServlet {#MultiAgentServlet}

The *MultiAgentServlet* can host multiple agent classes and multiple instances
of each agent class. To adress an instance of an agent, the url
is built up with the servlet path, agent class name, and id of the agent 
(http://server/servlet/agentclass/agentid).
To use a MultiAgentServlet, the servlet must be configured in the web.xml file
of the Java project, and an Eve configuration file must be created.


### Servlet configuration

A MultiAgentServlet is a regular Java servlet. 
To configure the servlet, add the following lines to the **web.xml** file of 
the Java project, inside the &lt;web-app&gt; tag:

    <servlet>
      <servlet-name>MultiAgentServlet</servlet-name>
      <servlet-class>com.almende.eve.servlet.MultiAgentServlet</servlet-class>
      <init-param>
        <description>servlet configuration (yaml file)</description> 
        <param-name>config</param-name>
        <param-value>eve.yaml</param-value>
      </init-param>
    </servlet>
    <servlet-mapping>
      <servlet-name>MultiAgentServlet</servlet-name>
      <url-pattern>/agents/*</url-pattern>
    </servlet-mapping>

The *url-pattern* can be freely chosen (in the example chosen as `/agents/*`.
This determines the url at which the servlet is running. 
It is important to end the url with the pattern /\*, as the url of the
servlet will end with the class name and id of the agent.

The servlet configuration contains an init-param `config`,
which must point to an Eve configuration file (for example eve.yaml).
It is possible to configure multiple servlets, and use a different 
configuration file for each of them.


### Eve configuration

The servlet configuration points to an Eve configuration file.
The configuration file is a [YAML](http://en.wikipedia.org/wiki/YAML) file.
The file can have any name and is normally located in the same folder as
web.xml, typically war/WEB-INF. Standard file name is **eve.yaml**.
Create a file eve.yaml and insert the following text:

    # Eve multiagent settings

    # environment settings
    environment:
      Development:
        servlet_url: http://localhost:8080/MyProject/agents
      Production:
        servlet_url: http://MyServer/MyProject/agents

    # agent settings
    agent:
      classes:
      - com.almende.eve.agent.example.EchoAgent
      - com.almende.eve.agent.example.CalcAgent
      - com.almende.eve.agent.example.ChatAgent

    # context settings
    # the context is used by agents for storing their state.
    context:
      class: com.almende.eve.context.MemoryContextFactory

The configuration contains:

- A parameter *environment*. 
  A project typically has two different environments: 
  *Development* and *Production*.
  The parameter *servlet_url* defines the url of the agents. 
  This url needs to be specified, as it is not possible for an agent to know 
  via what servlet it is being called.

- A parameter *agent.classes* containing a list with the agent classes which 
  will be hosted by the servlet.
  Eve comes with a number of example agents, such as the CalcAgent and the EchoAgent,
  these agents can be used to test if the application runs correctly.

- A parameter *context.class* specifying the type of context that will be 
  available for the agents to read and write persistent data.
  Agents themselves are stateless. They can use a context to store data.

Each agent has access has access to this configuration file via its 
[context](java_agents.html#context).
If your agent needs specific settings (for example for database access), 
you can add these settings to the configuration file.



## Custom servlet {#custom}

If the SingleAgentServlet and the MultiAgentServlet do not fulfill your needs,
it is possible to develop a custom servlet. 
This can for example be useful when exposing an existing service via a JSON-RPC 
interface to the Eve world. 
When creating a custom servlet, it is possible to get a better performance, 
as the servlet can be integrated tightly with the service.
It is not necessary to have a real Eve agent running via the servlet, 
the essence is that a service exposes a JSON-RPC interface to the outside world.

