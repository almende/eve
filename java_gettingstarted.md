---
layout: default
title: Getting Started
---

# Getting Started

The Eve agent platform can be easily integrated in an existing Java project
useing the provided libraries. 
Eve provides various libaries to deploy agents on different platforms, 
with different storage facilities.
The agents can for example be deployed on a regular Tomcat server that you host
yourself, or on Google App Engine in the cloud.

This tutorial shows how to create a an application with your own agent, and 
deploy it on Google App Engine, as it is easy to get started with Google 
App Engine, and it is easy to really deploy your project into the cloud.
Googles Datastore is used for persistency.
Creating a project for another type of deployment is similar, you basically
just have to include the Eve libraries and configure a web servlet.

The tutorial contains the following steps:

- [Prerequisites](#prerequisites)
- [Project Setup](#project_setup)
- [Usage](#usage)
- [Create your own agent](#create_your_own_agent)
- [Deployment](#deployment)


## Prerequisites {#prerequisites}

This tutorial uses Eclipse and the Google Web Toolkit plugin.

- Download and unzip [Eclipse Helios (3.6)](http://www.eclipse.org/helios/).
  On the site, click Download Helios, Eclipse IDE for Java Developers.
  Then select the correct zip file for your system and download it.
  Unzip it somewhere on your computer and start Eclipse.
  Note that you need to have a Java SDK installed on your computer.

- In Eclipse install the [Google Web Toolkit](http://code.google.com/webtoolkit/) plugin. 
  Go to menu Help, Install New Software... Click Add to add a new software source,
  and enter name "Google Web Toolkit" and location 
  [http://dl.google.com/eclipse/plugin/3.6](http://dl.google.com/eclipse/plugin/3.6).
  Click Ok. 
  Then, select and install "Google Plugin for Eclipse" and "SDKs".

Note that for a typical java web application you will need the 
[Web Tools Platform plugin](http://download.eclipse.org/webtools/repository/helios/) 
and a [Tomcat server](http://tomcat.apache.org/).


## Project Setup {#project_setup}

We will create a new project, add the required libraries, and configure a
web servlet.

- Create a new GWT project in Eclipse via menu New, Project, Google,
  Web Application Project. Select a project name and a package name, 
  for example "MyEveProject" and "com.mycompany.myproject".
  Unselect the option "Use Google Web Toolkit", and select the options 
  "Use Google App Engine" and "Generate GWT project sample code" checked. 
  Click Finish.

- Download the following jar files, and put them in your Eclipse project
  in the folder war/WEB-INF/lib. 
  If you don't want to download all libraries individually, you can download the
  zip files *eve-core-bundle.zip* and *eve-google-appengine-bundle.zip*
  containing all dependent libraries 
  [here](https://github.com/almende/eve/tree/master/java/bin/current).  

  - [eve-core.jar](https://github.com/almende/eve/tree/master/java/bin/current)
  
    - [json-lib-2.4-jdk15.jar](http://json-lib.sourceforge.net/)
    - [jakarta commons-lang 2.5](http://commons.apache.org/lang/)
    - [jakarta commons-beanutils 1.8.0](http://commons.apache.org/beanutils/)
    - [jakarta commons-collections 3.2.1](http://commons.apache.org/collections/)
    - [jakarta commons-logging 1.1.1](http://commons.apache.org/logging/)
    - [ezmorph 1.0.6](http://ezmorph.sourceforge.net/)

  - [eve-google-appengine.jar](https://github.com/almende/eve/tree/master/java/bin/current)
  
    - [twig-persist-2.0-beta4.jar](http://code.google.com/p/twig-persist/)
    - [guava-11.0.2.jar](http://code.google.com/p/guava-libraries/)
  
- Right-click the added jars in Eclipse, and click Build Path, "Add to Build Path". 
    
- Now, you need to configure a web-servlet to host the agents you want. 
  Open the file web.xml under war/WEB-INF. Insert the following lines
  inside the &lt;web-app&gt; tag:
  <pre><code>&lt;servlet&gt;
    &lt;servlet-name&gt;MultiAgentServlet&lt;/servlet-name&gt;
    &lt;servlet-class&gt;com.almende.eve.servlet.MultiAgentServlet&lt;/servlet-class&gt;
    &lt;init-param&gt;
      &lt;description&gt;The agent classes served by the servlet&lt;/description&gt; 
      &lt;param-name&gt;agents&lt;/param-name&gt;
      &lt;param-value&gt;
        com.almende.eve.agent.example.EchoAgent;
        com.almende.eve.agent.example.CalcAgent;        
      &lt;/param-value&gt;
    &lt;/init-param&gt;   
    &lt;init-param&gt;
      &lt;description&gt;The context for reading/writing persistent data&lt;/description&gt; 
      &lt;param-name&gt;context&lt;/param-name&gt;
      &lt;param-value&gt;com.almende.eve.agent.context.google.DatastoreContext&lt;/param-value&gt;
    &lt;/init-param&gt;
  &lt;/servlet&gt;
  &lt;servlet-mapping&gt;
    &lt;servlet-name&gt;MultiAgentServlet&lt;/servlet-name&gt;
    &lt;url-pattern&gt;/agents/*&lt;/url-pattern&gt;
  &lt;/servlet-mapping&gt;
  </code></pre>

  The configuration consists of a standard servlet and servlet mapping definition.
  Eve comes with two ready-made servlets: a SingleAgentServlet and a MultiAgentServlet.
  
  - The *SingleAgentServlet* will host a single agent which can be accessed 
  directly via the servlet url (http://server/servlet).
  
  - The *MultiAgentServlet* can host multiple agent classes and multiple instances
  of each agent class. To adress an instance of an agent, the url
  is built up with the servlet path, agent class name, and id of the agent 
  (http://server/servlet/agentclass/agentid).
  
  The MultiAgentServlet configuration needs two initialization parameters: 
  *agents* and *context*.
  
  - The *agents* parameter contains a list with the agent classes which will be
  hosted by the servlet. The classes must be separated by a semicolon.
  Eve comes with a number of example agents, such as the CalcAgent and the EchoAgent,
  so you can use these to test if your application works. 

  - The *context* parameter specifies the context that will be available for the 
  agents to read and write persistent data.



## Usage {#usage}

Now the project can be started and you can see one of the example agents in action.

- Start the project via menu Run, Run As, Web Application.
  
- To verify if the AgentsServlet of Eve is running, open your browser and
  go to http://localhost:8888/agents/.
  This should give a response *"Error: POST request containing a JSON-RPC 
  message expected"*.
  
- Agents can be accessed via an HTTP POST request. The url defines 
  the location of the agent and looks like 
  http://server/servlet/agentclass/agentid.
  The body of the post request must contain a JSON-RPC message.
  To execute HTTP requests you can use a REST client like 
  [Postman](https://chrome.google.com/webstore/detail/fdmmgilgnpjigdojojpjoooidkmcomcm) in Chrome,
  [RESTClient](https://addons.mozilla.org/en-US/firefox/addon/restclient/?src=search) in Firefox,
  or with a tool like [cURL](http://curl.haxx.se/).

  Perform an HTTP POST request to the CalcAgent on the url
  <pre><code>http://localhost:8888/agents/calcagent/1</code></pre>
  
  With request body:
  <pre><code>{
    "id": 1, 
    "method": "eval",
    "params": {
      "expr": "2.5 + 3 / sqrt(16)"
    }
  }</code></pre>
  
  This request will return the following response:
  <pre><code>{
    "jsonrpc": "2.0",
    "id": 1,
    "result": "3.25"
  }</code></pre>


## Create your own agent {#create_your_own_agent}

Now, what you want of course is create your own agents. This is quite easy:
create a java class which extends from the base class Agent, and register
your agent class in the eve.properties file.


- Create a new java class named MyFirstAgent under com.mycompany.myproject 
  with the following contents:
  
  <pre><code>package com.mycompany.myproject;

  import com.almende.eve.agent.Agent;
  import com.almende.eve.json.annotation.ParameterName;

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

- In order to make this agent available, you have to add its class name to
  the configuration of the web servlet in the file web.xml, 
  located under war/WEB-INF. Add the full classname 
  com.mycompany.myproject.MyFirstAgent to the list with agents in the init 
  parameter *agents*. Class names must be separated by a semicolon. 
  <pre><code>...
  &lt;init-param&gt;
    &lt;description&gt;The agent classes served by the servlet&lt;/description&gt; 
    &lt;param-name&gt;agents&lt;/param-name&gt;
    &lt;param-value&gt;
      com.almende.eve.agent.example.EchoAgent;
      com.almende.eve.agent.example.CalcAgent;        
      <b>com.mycompany.myproject.MyFirstAgent;</b>
    &lt;/param-value&gt;
  &lt;/init-param&gt;     
  ...
  </code></pre>

- Now you can (re)start the server, and perform an HTTP POST request to the url
  
  <pre><code>http://localhost:8888/agents/myfirstagent/1234</code></pre>
  
  With as request:
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

<!-- TODO: explain ids -->


## Deployment {#deployment}

Now you can deploy your applictation in the cloud, to Google App Engine.

- Register an application in appengine.
  In your browser, go to [https://appengine.google.com](https://appengine.google.com).
  You will need a Google account for that. Create a new application by clicking
  Create Application. Enter an identifier, for example "myeveproject" and a 
  title and click Create Application.
  
- In Eclipse, go to menu Project, Properties. Go to the page Google, App Engine.
  Under *Deployment*, enter the identifier "myeveproject" of your application 
  that you have just created on the appengine site. Set version to 1. Click Ok.

- In Eclipse, right-click your project in the Package Explorer. In the context
  menu, choose Google, Deploy to App Engine. Click Deploy in the opened window,
  and wait until the deployment is finished.
  
- Your application is now up and running and can be found at 
  http://myeveproject.appspot.com (where you have to replace the identifier with 
  your own). The agent you have created yourself is accessable at
  http://myeveproject.appspot.com/agents/myfirstagent/1234.
