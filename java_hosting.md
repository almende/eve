---
layout: default
title: Hosting
---


# Hosting

In Java, Eve agents are hosted via a web servlet. 
Eve comes with two ready-made servlets: a SingleAgentServlet and a MultiAgentServlet.
These servlets are located in the eve-core.jar library. 
Furthermore, one can create its own custom servlet when needed.

## SingleAgentServlet {#SingleAgentServlet}

The *SingleAgentServlet* will host a single agent which can be accessed 
directly via the servlet url (http://server/servlet). 
The SingleAgentServlet is a regular Java servlet, and needs to be configured
in the web.xml file in your project (typically located at war/WEB-INF/).

To configure the SingleAgentServlet, add the following lines to the web.xml file,
inside the &lt;web-app&gt; tag:

    <servlet>
      <servlet-name>SingleAgentServlet</servlet-name>
      <servlet-class>com.almende.eve.servlet.SingleAgentServlet</servlet-class>
      <init-param>
        <description>The agent class served by the servlet</description> 
        <param-name>agent</param-name>
        <param-value>com.almende.eve.agent.example.EchoAgent</param-value>
      </init-param>
      <init-param>
        <description>The context for reading and writing persistent data</description> 
        <param-name>context</param-name>
        <param-value>com.almende.eve.context.MemoryContext</param-value>
      </init-param>	  
    </servlet>  
    <servlet-mapping>
      <servlet-name>SingleAgentServlet</servlet-name>
      <url-pattern>/testagent</url-pattern>
    </servlet-mapping>

Note the folllowing:

- The servlet configuration contains two initialization parameters 
  *agent* and *context*. The parameter *agent* contains the Java class
  of the agent that will be hosted by the servlet. 
  The parameter *context* specifies a Context class, which determines the
  type of the context available to the Agent for persisting its state.

- The *url-pattern* can be freely chosen. 
  This determines the url at which the servlet is running, 
  and thus at which the agent can be found.


## MultiAgentServlet {#MultiAgentServlet}

The *MultiAgentServlet* can host multiple agent classes and multiple instances
of each agent class. To adress an instance of an agent, the url
is built up with the servlet path, agent class name, and id of the agent 
(http://server/servlet/agentclass/agentid).

A MultiAgentServlet is a regular Java servlet. 
To configure the servlet, add the following lines to the web.xml file,
inside the &lt;web-app&gt; tag:

    <servlet>
      <servlet-name>MultiAgentServlet</servlet-name>
      <servlet-class>com.almende.eve.servlet.MultiAgentServlet</servlet-class>
      <init-param>
        <description>The agent classes served by the servlet</description> 
        <param-name>agents</param-name>
        <param-value>
          com.almende.eve.agent.example.EchoAgent;
          com.almende.eve.agent.example.CalcAgent;
          com.almende.eve.agent.example.ChatAgent;
        </param-value>
      </init-param>
      <init-param>
        <description>The context for reading and writing persistent data</description> 
        <param-name>context</param-name>
        <param-value>com.almende.eve.context.MemoryContext</param-value>
      </init-param>
    </servlet>
    <servlet-mapping>
      <servlet-name>MultiAgentServlet</servlet-name>
      <url-pattern>/agents/*</url-pattern>
    </servlet-mapping>

Note that:

- The MultiAgentServlet configuration needs two initialization parameters: 
  *agents* and *context*.
  The *agents* parameter contains a list with the agent classes which will be
  hosted by the servlet. The classes must be separated by a semicolon.
  Eve comes with a number of example agents, such as the CalcAgent and the EchoAgent,
  so you can use these to test if your application works. 
  The *context* parameter specifies the context that will be available for the 
  agents to persist their state.

- The *url-pattern* can be freely chosen. 
  This determines the url at which the servlet is running. 
  It is important to end the url with the pattern /\*, as the url of the
  servlet will end with the class name and id of the agent.


## Custom servlet {#custom}

If the SingleAgentServlet and the MultiAgentServlet do not fulfill your needs,
it is possible to develop a custom servlet. 
This can for example be useful when exposing an existing service via a JSON-RPC 
interface to the Eve world. 
When creating a custom servlet, it is possible to get a better performance, 
as the servlet can be integrated tightly with the service.
It is not necessary to have a real Eve agent running via the servlet, 
the essence is that a service exposes a JSON-RPC interface to the outside world.

