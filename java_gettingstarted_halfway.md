---
layout: default
title: Getting Started
---

# Getting Started

With the provided Java library you can easily integrate the Eve agent platform
in your Java project. This tutorial contains the following steps:

* [Prerequisites](#prerequisites)
* [Project Setup](#project_setup)
* [Usage](#usage)
* [Create your own agent](#create_your_own_agent)


## Prerequisites {#prerequisites}

* This tutorial assumes that you are using [Eclipse Helios (3.6)](http://www.eclipse.org/helios/).

* In Eclipse, you must have installed the 
  [Web Tools Platform](http://download.eclipse.org/webtools/repository/helios/) plugin.

* In Eclipse, you must have configured a [Tomcat server](http://tomcat.apache.org/).


## Project Setup {#project_setup}

* Create a new Dynamic Web Project in Eclipse via menu New, Other, 
  Dynamic Web Project. Select a project name and a package name, 
  for example "MyEveProject". Click Next twice. Then, enter "war" as 
  Content Directory (instead of the default "WebContent"), and check the 
  checkbox "Generate web.xml deployment descriptor".
  
* Download the following jar files, and store them in your Eclipse project
  in the folder war/WEB-INF/lib.

  * jakarta commons-lang 2.5
  * jakarta commons-beanutils 1.8.0
  * jakarta commons-collections 3.2.1
  * jakarta commons-logging 1.1.1
  * ezmorph 1.0.6
  * json-lib-2.4-jdk15.jar
  * eve-core.jar
  
* In Eclipse, go to menu Project, Properties, Java Build Path. 
  Go to the tab Libraries, and click Add Jars. Select the downloaded 
  jar files and click Ok.
    
* Now, we need to configure a web-servlet to host our agents. 
  Open the file web.xml under war/WEB-INF. Insert the following lines
  inside the &lt;web-app&gt; tag:
  <pre><code>&lt;servlet&gt;
    &lt;servlet-name&gt;MultiAgentServlet&lt;/servlet-name&gt;
    &lt;servlet-class&gt;eve.servlet.MultiAgentServlet&lt;/servlet-class&gt;
    &lt;init-param&gt;
      &lt;description&gt;The agent classes served by the servlet&lt;/description&gt; 
      &lt;param-name&gt;agents&lt;/param-name&gt;
      &lt;param-value&gt;
        eve.agent.TestAgent;        
        eve.agent.EchoAgent;
      &lt;/param-value&gt;
    &lt;/init-param&gt;   
    &lt;init-param&gt;
      &lt;description&gt;The context for reading/writing persistent data&lt;/description&gt; 
      &lt;param-name&gt;context&lt;/param-name&gt;
      &lt;param-value&gt;eve.agent.context.SimpleContext&lt;/param-value&gt;
    &lt;/init-param&gt;
  &lt;/servlet&gt;
  &lt;servlet-mapping&gt;
    &lt;servlet-name&gt;MultiAgentServlet&lt;/servlet-name&gt;
    &lt;url-pattern&gt;/agents/*&lt;/url-pattern&gt;
  &lt;/servlet-mapping&gt;
  </code></pre>

  <!-- TODO: explain the configuration parameters -->

## Usage {#usage}

Now the project can be started.

* Start the project via menu Run, Run As, Run on Server.
  
* To verify if the AgentsServlet of Eve is running, open your browser and
  go to http://localhost:8080/MyEveProject/agents/.
  This should give a response *"Error: POST request containing a JSON-RPC 
  message expected"*.
  
* Agents can be accessed via an HTTP POST message. The url defines 
  the location of the agent and looks like 
  http://server/context/agents/agentclass/agentid.
  The body of the post message must contain an JSON-RPC message.

  For example, perform an HTTP POST request to the url
  <pre><code>http://localhost:8080/MyEveProject/agents/echoagent/1234</code></pre>
  
  And with request body:
  <pre><code>{
    "id":1, 
    "method": "ping",
    "params": {
      "message": "hello world"
    }
  }</code></pre>
  
  This request will return the following response:
  <pre><code>{
    "jsonrpc": "2.0",
    "id":1,
    "result": "hello world"
  }</code></pre>



## Create your own agent {#create_your_own_agent}

Now, what you want of course is create your own agents. This is quite easy:
create a java class which extends from the base class Agent, and register
your agent class in the eve.properties file.


* Create a new java class named MyFirstAgent under com.myproject.server with the 
  following contents:
  
  <pre><code>package com.myproject;

  import eve.agent.Agent;
  import eve.json.annotation.ParameterName;

  public class MyFirstAgent extends Agent {
      public String echo (@ParameterName("message") String message) {
          return message;	
      }

      public double add (@ParameterName("a") double a, 
              @ParameterName("b") double b) {
          return a + b;	
      }

      @Override
      public String getDescription() {
          return "My first agent";
      }

      @Override
      public String getVersion() {
          return "0.1";
      }
  }
  </code></pre>
  
  Each agent must contain at least two default methods: getDescription 
  and getVersion. Next, you can add your own methods, in this example the 
  methods echo and add. 

* In order to make this agent available, we have to add its class name to
  the configuration of the web servlet in the file web.xml, 
  located under war/WEB-INF. Add the full classname 
  (com.myproject.MyFirstAgent) to the list with agents in the init parameter
  *agents*. Class names must be separated by a semicolon. 
  <pre><code>...
  &lt;init-param&gt;
    &lt;description&gt;The agent classes served by the servlet&lt;/description&gt; 
    &lt;param-name&gt;agents&lt;/param-name&gt;
    &lt;param-value&gt;
      eve.agent.TestAgent;        
      eve.agent.EchoAgent;
      <b>com.myproject.MyFirstAgent;</b>
    &lt;/param-value&gt;
  &lt;/init-param&gt;     
  ...
  </code></pre>

* Now you can (re)start the server, and in your browser go to the following
  url: http://localhost:8080/MyEveProject/agents/MyFirstAgent/1234. 
  Now you can enter JSON-RPC requests like:
  <pre><code>{
    "id": 1, 
    "method": "echo", 
    "params": {
      "message": "Hello World"
    }
  }</code></pre>
  which returns:
  <pre><code>{
    "jsonrpc": "2.0",
    "id": 1,
    "result": "Hello World"
  }</code></pre>

  or send the following request:
  <pre><code>{
    "id": 1, 
    "method": "add", 
    "params": {
      "a": 2.1, 
      "b": 3.5
    }
  }</code></pre>
  which returns:
  <pre><code>{
    "jsonrpc": "2.0",
    "id": 1,
    "result": 5.6
  }</code></pre>

