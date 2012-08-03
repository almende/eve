---
layout: default
title: Getting Started
---


{% assign eve_core_version = '0.7' %}
{% assign eve_google_appengine_version = '0.6' %}


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

Note that for a typical java web application you would need the 
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
  

  - [eve-core-{{eve_core_version}}.jar](files/java/eve-core-{{eve_core_version}}.jar)
  
    - [jackson-databind-2.0.0.jar](http://jackson.codehaus.org)
    - [jackson-core-2.0.0.jar](http://jackson.codehaus.org)
    - [jackson-annotations-2.0.0.jar](http://jackson.codehaus.org)
    - [snakeyaml-1.10.jar](http://snakeyaml.org)

  - [eve-google-appengine-{{eve_google_appengine_version}}.jar](files/java/eve-google-appengine-{{eve_google_appengine_version}}.jar)
  
    - [twig-persist-2.0-beta4.jar](http://code.google.com/p/twig-persist)
    - [guava-11.0.2.jar](http://code.google.com/p/guava-libraries)
  
  If you don't want to download all libraries individually, you can download the
  zip files 
  [eve-core-{{eve_core_version}}-bundle.zip](files/java/eve-core-{{eve_core_version}}-bundle.zip) and 
  [eve-google-appengine-{{eve_google_appengine_version}}-bundle.zip](files/java/eve-google-appengine-{{eve_google_appengine_version}}-bundle.zip)
  containing all dependent libraries. 
  
- Right-click the added jars in Eclipse, and click Build Path, "Add to Build Path". 
    
- Now, you need to configure a web-servlet which will host your agents. 
  Open the file web.xml under war/WEB-INF. Insert the following lines
  inside the &lt;web-app&gt; tag:

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
      
  Note that we have added an init-param `config` to the the servlet settings. 
  This parameter points to a configuration file eve.yaml,
  which we will create next. 

- Create an Eve configuration file named eve.yaml in the folder war/WEB-INF 
  (where web.xml is located too). Insert the following text in this file:
  
      # Eve settings

      # environment settings
      environment:
        Development:
          servlet_url: http://localhost:8888/agents
        Production:
          servlet_url: http://myeveproject.appspot.com/agents

      # agent settings
      agent:
        classes:
        - com.almende.eve.agent.example.EchoAgent
        - com.almende.eve.agent.example.CalcAgent
        - com.almende.eve.agent.example.ChatAgent

      # context settings
      # the context is used by agents for storing their state.
      context:
        class: com.almende.eve.context.google.DatastoreContextFactory
  
  The configuration is a [YAML](http://en.wikipedia.org/wiki/YAML) file.
  It contains:
  
  - A parameter *environment*. 
    A project typically has two different environments: 
    *Development* and *Production*.
    The parameter *servlet_url* defines the url of the agents. 
    This url needs to be specified, as it is not possible for an agent to know 
    via what servlet it is being called.
  
  - The parameter *agent.classes* contains a list with the agent classes which 
    will be hosted by the servlet.
    Eve comes with a number of example agents, such as the CalcAgent and the EchoAgent,
    these agents can be used to test if the application runs correctly.

  - The parameter *context.class* specifies the type of context that will be 
    available for the agents to read and write persistent data.
    Agents themselves are stateless. They can use a context to store data.

  Each agent has access has access to this configuration file via its 
  [context](java_agents.html#context).
  If your agent needs specific settings (for example for database access), 
  you can add these settings to the configuration file.


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

      http://localhost:8888/agents/calcagent/1

  With request body:

      {
        "id": 1, 
        "method": "eval",
        "params": {
          "expr": "2.5 + 3 / sqrt(16)"
        }
      }
  
  This request will return the following response:
  
      {
        "jsonrpc": "2.0",
        "id": 1,
        "result": "3.25"
      }


## Create your own agent {#create_your_own_agent}

Now, what you want of course is create your own agents. This is quite easy:
create a java class which extends from the base class Agent, and register
your agent class in the eve.properties file.


- Create a new java class named MyFirstAgent under com.mycompany.myproject 
  with the following contents:
  
      package com.mycompany.myproject;
      
      import com.almende.eve.agent.Agent;
      import com.almende.eve.json.annotation.Name;
      
      public class MyFirstAgent extends Agent {
          public String echo (@Name("message") String message) {
              return message;  
          }
          
          public double add (@Name("a") double a, 
                  @Name("b") double b) {
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

  
  Each agent must contain at least two default methods: getDescription 
  and getVersion. Next, you can add your own methods, in this example the 
  methods echo and add. 

- In order to make this agent available, you have to add its class name to
  the Eve configuration in the file eve.yaml, earlier created and stored in 
  war/WEB-INF. 
  Add the full classname com.mycompany.myproject.MyFirstAgent to the list with 
  classes under *agent*, *classes*:
  
      ...
      # agent settings
      agent:
        classes:
        - com.almende.eve.agent.example.EchoAgent
        - com.almende.eve.agent.example.CalcAgent
        - com.almende.eve.agent.example.ChatAgent
        - com.mycompany.myproject.MyFirstAgent
      ...

- Now you can (re)start the server, and perform an HTTP POST request to the url
  
      http://localhost:8888/agents/myfirstagent/1234
  
  With as request:
  
      {
        "id": 1, 
        "method": "echo", 
        "params": {
          "message": "Hello World"
        }
      }
  
  which returns:

      {
        "jsonrpc": "2.0",
        "id": 1,
        "result": "Hello World"
      }

  or send the following request:
  
      {
        "id": 1, 
        "method": "add", 
        "params": {
          "a": 2.1, 
          "b": 3.5
        }
      }

  which returns:

      {
        "jsonrpc": "2.0",
        "id": 1,
        "result": 5.6
      }

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
  
- Ensure the servlet_url for the production environment in the configuration 
  file war/WEB-INF/eve.yaml corresponds with your application
  identifier: http://myeveproject.appspot.com/agents/
  
      ...
      # environment settings
      environment:
        Development:
          servlet_url: http://localhost:8888/agents
        Production:
          servlet_url: http://myeveproject.appspot.com/agents
      ...

- In Eclipse, right-click your project in the Package Explorer. In the context
  menu, choose Google, Deploy to App Engine. Click Deploy in the opened window,
  and wait until the deployment is finished.
  
- Your application is now up and running and can be found at 
  http://myeveproject.appspot.com (where you have to replace the identifier with 
  your own). The agent you have created yourself is accessable at
  http://myeveproject.appspot.com/agents/myfirstagent/1234.
