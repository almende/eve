---
layout: default
title: Getting Started
---

{% assign version = '2.0.0' %}

# Getting Started with Eve, Jetty, and Maven

Eve provides a Java library which can be integrated in new or existing Java projects. Eve can be used on different environments. Eve agents can for example run in a regular Java application, a Java web application, an Android application, on Google App Engine, etc.

This tutorial explains how to create a an application with your own agent, and run it as a Jetty web application. Creating a project for another type of deployment works similarly. It boils down to including the Eve libraries your the project, configuring an AgentHost, and instantiating your agents.

The tutorial contains the following steps:

- [Prerequisites](#prerequisites)
- [Create a web application](#create_web_application)
- [Setup Jetty](#setup_jetty)
- [Setup Eve](#setup_eve)
- [Create an example agent](#create_an_example_agent)
- [Create your own agent](#create_your_own_agent)
- [Conclusion](#conclusion)


## Prerequisites {#prerequisites}

This tutorial assumes you have the following development environment up and running:

- [Eclipse](http://www.eclipse.org/)
- [Maven 3.x](http://maven.apache.org/)
- [Maven plugin for Eclipse](http://eclipse.org/m2e/)


## Create a Maven web application {create_web_application}

First, we will create a regular Maven web application in Eclipse.

- Create a new Maven Project via menu File, New, Other..., then select `Maven Project`.
- Click next.
- Select the archetype with Artifact Id "maven-archetype-webapp".
- Click next.
- Enter Group Id "com.mycompany.myeveproject", Artifact Id "myeveproject", and Package "com.mycompany.myeveproject".
- Click Finish to create the project.


## Setup the Jetty plugin for Maven {#setup_jetty}

We will use Jetty to run the created web application. We will run jetty using the Jetty plugin for Maven.

Open the file pom.xml in the root of the project and add the jetty plugin:
{% highlight xml %}
    <project>
        ...
        <build>
            ...
            <plugins>
                <plugin>
                    <groupId>org.mortbay.jetty</groupId>
                    <artifactId>maven-jetty-plugin</artifactId>
                    <version>6.1.10</version>
                    <configuration>
                        <scanIntervalSeconds>10</scanIntervalSeconds>
                        <stopKey>foo</stopKey>
                        <stopPort>9999</stopPort>
                    </configuration>
                    <executions>
                        <execution>
                            <id>start-jetty</id>
                            <phase>pre-integration-test</phase>
                            <goals>
                                <goal>run</goal>
                            </goals>
                            <configuration>
                                <scanIntervalSeconds>0</scanIntervalSeconds>
                                <daemon>true</daemon>
                            </configuration>
                        </execution>
                        <execution>
                            <id>stop-jetty</id>
                            <phase>post-integration-test</phase>
                            <goals>
                                <goal>stop</goal>
                            </goals>
                        </execution>
                    </executions>
                </plugin>
            </plugins>
            ...
        </build>
    </project>
{% endhighlight %}

Start the project via the following command:

    mvn jetty:run

Test whether the server runs by opening the following url in your browser:

    http://localhost:8080/myeveproject/

When the web application runs fine, we can continue with adding Eve to the project.


## Setup Eve {#setup_eve}

Add the maven dependencies for Eve in pom.xml:

{% highlight xml %}
    <dependencies>
        ...
        <dependency>
            <groupId>com.almende.eve</groupId>
            <artifactId>eve-core</artifactId>
            <version>{{version}}</version>
        </dependency>
    </dependencies>
{% endhighlight %}

Maven will take care resolving all dependencies of the Eve library.


### Configure an AgentHost for Eve {#configure_agenthost}

Configure an Eve AgentHost, which manages Eve agents. Open the servlet configuration file `web.xml` in the folder `src/main/webapp/WEB-INF`, and add the following configuration:

{% highlight xml %}
<web-app>
    ...
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
</web-app>
{% endhighlight %}

Note that we have specified a number of context parameters.

- The context-param `eve_config` points to an eve configuration file `eve.yaml`, which we will create next. This configuration file is used is used to set up the AgentHost managing all Eve agents.
- The context-param `eve_authentication` determines whether we want to use a secured SSL connection to let agents communicate with each other.

Create an Eve configuration file named `eve.yaml` in the folder `war/WEB-INF` (where `web.xml` is located too). Insert the following text in this file:

{% highlight yaml %}
# Eve configuration

# communication services
# services:
# - class: ...

# state settings (for persistence)
state:
  class: FileStateFactory
  path: .eveagents

# scheduler settings (for tasks)
scheduler:
  class: RunnableSchedulerFactory
{% endhighlight %}

The configuration is a [YAML](http://en.wikipedia.org/wiki/YAML) file. It contains:

- The parameter *services* allows configuration of multiple communication services such ass HTTP or XMPP, which enables agents to communicate with each other in multiple ways. An agent will have a unique url for each of the configured services. We don't need to specify this parameter in our case: in the next step we will configure an HTTP Service, which automatically registers itself at the AgentHost.

- The parameter *state* specifies the type of state that will be available for the agents to read and write persistent data. Agents themselves are stateless. They can use a state to persist data.

- The parameter *scheduler* specifies the scheduler that will be used to let agents schedule tasks for themselves.

- Optionally, all Eve parameters can be defined for a specific environment: Development or Production. In that case, the concerning parameters can be defined under `environment.Development.[param]` and/or `environment.Production.[param]`.

Each agent has access has access to this configuration file via its AgentHost. If your agent needs specific settings (for example for database access), you can add these settings to the configuration file. More detailed information on the Eve configuration can be found on the page [Configuration](configuration.html).

The AgentHost is now fully configured so we can run agents on it. However, we don't yet have a way to interact with these agents externally. In the next step we will create a Transport Service to communicate with our agents.


### Configure an AgentServlet for Eve {#configure_agentservlet}

Now, we will configure an HTTP Transport service for Eve, which allows us to communicate via HTTP with the agents running in our application. Open the servlet configuration file `web.xml` in the folder `src/main/webapp/WEB-INF`, and add the following configuration:


{% highlight xml %}
<web-app>
	...
	<servlet>
		<servlet-name>AgentServlet</servlet-name>
		<servlet-class>com.almende.eve.transport.http.AgentServlet</servlet-class>
		<init-param>
			<param-name>servlet_url</param-name>
			<param-value>http://localhost:8080/myeveproject/agents/</param-value>
		</init-param>
		<load-on-startup>1</load-on-startup>
	</servlet>
	<servlet-mapping>
		<servlet-name>AgentServlet</servlet-name>
		<url-pattern>/agents/*</url-pattern>
	</servlet-mapping>
	...
</web-app>
{% endhighlight %}

The servlet can route incoming HTTP requests to agents running on the AgentHost. The servlet needs a parameter `servlet_url`. This url is needed in order to be able to built an agents full url, which is used to share with others via the method `getUrls()`.


## Create an example agent {#create_an_example_agent}

Now that the Jetty and Eve are configured, we can create an instance of one of the included example agents.

Stop the web application if still running, and start it again via:

    mvn jetty:run

To verify if the AgentServlet of Eve is running, open your browser and go to http://localhost:8080/myeveproject/agents/. This will return generic information explaining the usage of the servlet. Agents can be created by sending a HTTP PUT request to the servlet, deleted using a HTTP DELETE request, and invoked via an HTTP POST request. To execute HTTP requests you can use a REST client like [Postman](https://chrome.google.com/webstore/detail/fdmmgilgnpjigdojojpjoooidkmcomcm) in Chrome, [RESTClient](https://addons.mozilla.org/en-US/firefox/addon/restclient/?src=search) in Firefox, or with a tool like [cURL](http://curl.haxx.se/).

Create a CalcAgent by sending an HTTP PUT request to the servlet. We will create an agent with id `calcagent1` and class `com.almende.eve.agent.example.CalcAgent`.

    http://localhost:8080/myeveproject/agents/calcagent1/?type=com.almende.eve.agent.example.CalcAgent

If the agent is successfully created, the agents urls will be returned:

    http://localhost:8080/myeveproject/agents/calcagent1/
    local://calcagent1

Note that when an agent with this id already exists, the request will return a server error.

Agents can be invoked via an HTTP POST request. The url defines the location of the agent and looks like http://server/servlet/{agentId}. The body of the POST request must contain a JSON-RPC message.

Perform an HTTP POST request to the CalcAgent on the url

    http://localhost:8080/myeveproject/agents/calcagent1/

With request body:

{% highlight javascript %}
{
    "jsonrpc": "2.0",
    "id": 1,
    "method": "eval",
    "params": {
        "expr": "2.5 + 3 / sqrt(16)"
    }
}
{% endhighlight %}

This request will return the following response:

{% highlight javascript %}
{
    "jsonrpc": "2.0",
    "id": 1,
    "result": "3.25"
}
{% endhighlight %}

The agent also has a web interface, allowing you to interact with the agent more easily, to go to the agents web interface, open its url in your browser: http://localhost:8080/myeveproject/agents/calcagent1/.


## Create your own agent {#create_your_own_agent}

Now, what you want of course is create your own agents. This is quite easy: create a java class which extends from the base class Agent, and register your agent class in the eve.properties file.

Create a source folder `src/main/java` in the project. In there, create a new class named `MyFirstAgent` in package `com.mycompany.myeveproject` with the following contents:

{% highlight java %}
package com.mycompany.myeveproject;

import com.almende.eve.agent.Agent;
import com.almende.eve.rpc.annotation.Access;
import com.almende.eve.rpc.annotation.AccessType;
import com.almende.eve.rpc.annotation.Name;

public class MyFirstAgent extends Agent {
	@Access(AccessType.PUBLIC)
	public String echo (@Name("message") String message) {
		return "You said: " + message;
	}

	@Access(AccessType.PUBLIC)
	public double add (@Name("a") double a, @Name("b") double b) {
		return a + b;
	}
}
{% endhighlight %}

By default, an agents methods cannot be accessed by other agents. The availability of methods can be specified using the `@Access` annotation, as done in the example above where both methods `echo` and `add` are declared public. Eve agents communicate with each other via JSON-RPC 2.0, which uses named parameters. As Java doesn't support named parameters, the parameter names need to be specified using the `@Name` annotation. Furthermore, parameters can be declared optional using the `@Optional` annotation.

Create an instance of your new agent. Send an HTTP PUT request to the servlet. We will create an agent with id `myfirstagent1` and class `com.mycompany.myeveproject.MyFirstAgent`.

    http://localhost:8080/myeveproject/agents/myfirstagent1/?type=com.mycompany.myeveproject.MyFirstAgent

If the agent is successfully created, its urls will be returned:

    http://localhost:8080/myeveproject/agents/myfirstagent1/
    local://myfirstagent1

Now you can perform an HTTP POST request to the new agent

    http://localhost:8080/myeveproject/agents/myfirstagent1/

With as request body:

{% highlight javascript %}
{
    "jsonrpc": "2.0",
    "id": 1,
    "method": "echo",
    "params": {
        "message": "Hello World"
    }
}
{% endhighlight %}

which returns:

{% highlight javascript %}
{
    "jsonrpc": "2.0",
    "id": 1,
    "result": "Hello World"
}
{% endhighlight %}

or send the following request:

{% highlight javascript %}
{
    "jsonrpc": "2.0",
    "id": 1,
    "method": "add",
     "params": {
          "a": 2.1,
          "b": 3.5
     }
}
{% endhighlight %}

which returns:

{% highlight javascript %}
{
    "jsonrpc": "2.0",
    "id": 1,
    "result": 5.6
}
{% endhighlight %}

## Conclusion {#conclusion}

This tutorial demonstrated step by step how to set up a web application running via Jetty, how to set up Eve, and how to create and use Eve agents.

Now you know how to set up an Eve environment, you can start doing interesting things with the agents. Eve comes with functionality to let the agents communicate with each other, store state, and schedule tasks, subscribe to events, monitor other agents, and more. In-depth documentation can be found in the Reference section of this website.

