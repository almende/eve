---
layout: default
title: Deployment scenarios
---

# Deployment scenarios

Eve is designed to offer a large flexibility in deployment scenarios, it is possible to add agent capabilities to most Java classes, in many VM environments. In this section of the documentation three example scenarios are described which can be used as a base for real application deployment. 

- [Standalone](#standalone) - A standalone java application, packaged in a JAR with a main class starting the agents
- [Standalone with WakeService](#standalone_wake) - Like the standalone, but with agents that can be unloaded from memory
- [Standalone with Embedded Jetty server](#standalone_embed) - Like the standalone, but with a Httptransport running in the embedded Jetty server
- [Servlet](#servlet) - A java servlet application, packaged in a WAR archive, with agents being started in a ServletListener
- [Android](#android) - An example setup in an Android application

## Standalong Java setup {#standalone}

This scenario describes a standalone java executable setup of Eve, either without a http transport or with http transport through the embedded Jetty server. Eve gets started through an executable jar file, for example from the commandline. It shows how to obtain configuration data from a YAML file and use this to initiate agents. The Game-of-life demo in the Eve sourcecode is started in this manner and can be used as an example for such a setup.

For this example we first create a 'yaml' configuration file:

{% highlight yaml %}
#First we define a template for usage per agent. The ExampleAgent extends Eve's Agent class.
templates:
   defaultAgent:
      class: com.almende.eve.agent.ExampleAgent
      state:
         class: com.almende.eve.state.memory.MemoryStateBuilder
      scheduler:
         class: com.almende.eve.scheduling.SimpleSchedulerBuilder

#Here we define the agents themselves:
agents:
-  id: example
   extends: templates/defaultAgent
-  id: another
   extends: templates/defaultAgent

{% endhighlight %}

This configuration file can be read in the main class of the application. Eve offers a configuration structure, based on Jackson's Json DOM, which can parse and expand this configuration into agents. The 'extends' fields are special, they refer to another part of the DOM (as a path from the root node). The expand() method of the Eve configuration can replace these fields by the part of the tree they refer to.

The main class is given below:

{% highlight java %}
public class Example {

   public static void main(final String[] args){
      //First obtain the configuration:
      if (args.length == 0) {
         System.err.println("Missing argument pointing to yaml file!");
         return;
      }
      final Config config = 
         YamlReader
         .load(new FileInputStream(new File(args[0])))
         .expand();

      //Config is now a Jackson JSON DOM, 'expand()' allows for template resolvement in the configuration. 

      //Now we instantiate two example agents, getting their classpath (and configuration) from the DOM
      final ArrayNode agents = (ArrayNode) config.get("agents");
      for (final JsonNode agent : agents) {
         final AgentConfig agentConfig = new AgentConfig((ObjectNode) agent);
         final Agent newAgent = 
            new AgentBuilder()
            .with(agentConfig)
            .build();
         System.out.println("Created agent:" + newAgent.getId());
      }
   }
}
{% endhighlight %}

To get this code to execute, you can use Maven to produce the executable jar file, using the below given pom.xml:

{% highlight xml %}
<project xmlns="http://maven.apache.org/POM/4.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
   xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
   <modelVersion>4.0.0</modelVersion>

   <groupId>com.almende.eve.example</groupId>
   <artifactId>standalone</artifactId>
   <version>3.0.0-SNAPSHOT</version>

   <properties>
      <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>
      <maven.shade.version>2.3</maven.shade.version>
      <eve.version>3.0.0-SNAPSHOT</eve.version>
   </properties>

   <dependencies>
      <dependency>
         <groupId>com.almende.eve</groupId>
         <artifactId>eve-bundle-full</artifactId>
         <version>${eve.version}</version>
      </dependency>
   </dependencies>

   <build>
      <plugins>
         <plugin>
            <groupId>org.apache.maven.plugins</groupId>
               <artifactId>maven-shade-plugin</artifactId>
               <version>${maven.shade.version}</version>
               <configuration>
                  <transformers>
                     <transformer implementation="org.apache.maven.plugins.shade.resource.ManifestResourceTransformer">
                        <mainClass>com.almende.eve.example.Example</mainClass>
                     </transformer>
                  </transformers>
               </configuration>
               <executions>
                  <execution>
                     <phase>package</phase>
                     <goals>
                        <goal>shade</goal>
                     </goals>
                  </execution>
               </executions>
           </plugin>
      </plugins>
   </build>
</project>
{% endhighlight %}

In this pom, the maven-shade-plugin is used to repackage the app into one jar with all its dependencies. Part of the configuration of the plugin is the addition of a mainClass section in the jars manifest, allowing the jar to be executed:

{% highlight bash %}
user@example:~$ java -jar 'jar-with-dependencies.jar' example.yaml
Created agent:example
Created agent:another

user@example:~$ 
{% endhighlight %}

## Standalone setup with WakeService {#standalone_wake}

This setup is identical to the above given [standalone](#standalone) setup, except for some minor changes to the classes:

The ExampleAgent class should extend "com.almende.eve.agent.WakeableAgent" instead of just "Agent". The main class also needs to be changed somewhat, adding a reference to a WakeService:

{% highlight java %}
public class Example {

   public static void main(final String[] args){
      //First obtain the configuration:
      if (args.length == 0) {
         System.err.println("Missing argument pointing to yaml file!");
         return;
      }
      final Config config = 
         YamlReader
         .load(new FileInputStream(new File(args[0])))
         .expand();

      //Config is now a Jackson JSON DOM, 'expand()' allows for template resolvement in the configuration. 

      //Next we need a WakeService instance:
      final WakeServiceConfig wsconfig = new WakeServiceConfig();
      final WakeService ws = 
         new WakeServiceBuilder()
         .withConfig(wsconfig)
         .build();

      //Now we instantiate two example agents, getting their classpath (and configuration) from the DOM
      final ArrayNode agents = (ArrayNode) config.get("agents");
      for (final JsonNode agent : agents) {
         final AgentConfig agentConfig = new AgentConfig((ObjectNode) agent);
         final Agent newAgent = 
            new AgentBuilder()
            .withWakeService(ws)
            .with(agentConfig)
            .build();
         System.out.println("Created agent:" + newAgent.getId());
      }
   }
}
{% endhighlight %}

## Standalone setup with embedded Jetty {#standalone_embed}

This setup is identical to the above given [standalone](#standalone) setup, except for some addition configuration. In the pom.xml the dependency on eve-bundle-full need to be changed to a dependency on eve-bundle-full-embed:

{% highlight xml %}
   <dependencies>
      <dependency>
         <groupId>com.almende.eve</groupId>
         <artifactId>eve-bundle-full-embed</artifactId>
         <version>${eve.version}</version>
      </dependency>
   </dependencies>
{% endhighlight %}

The yaml configuration needs to be extended with a http transport configuration, including a reference to the jetty launcher.

{% highlight yaml %}
#First we define a template for usage per agent. The ExampleAgent extends Eve's Agent class.
templates:
   defaultAgent:
      class: com.almende.eve.agent.ExampleAgent
      state:
         class: com.almende.eve.state.memory.MemoryStateBuilder
      scheduler:
         class: com.almende.eve.scheduling.SimpleSchedulerBuilder
      transport:
         class: com.almende.eve.transport.http.HttpTransportBuilder
         servletUrl: http://localhost:8080/agents/
         servletLauncher: JettyLauncher
         jetty: 
            port: 8080

#Here we define the agents themselves:
agents:
-  id: example
   extends: templates/defaultAgent
-  id: another
   extends: templates/defaultAgent

{% endhighlight %}

In this example, the jetty server is listening on port 8080. This port is also the default, so the 'jetty' part of the config could have been omitted. 

## Servlet setup {#servlet}

To run Eve as a web project, packaged in a WAR, you need to create a normal Maven web project layout.
In src/main/webapp/WEB-INF there will be a web.xml, which needs to be loaded with the following configuration:

{% highlight xml %}
<?xml version="1.0" encoding="UTF-8"?>
<web-app xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
   xmlns="http://java.sun.com/xml/ns/javaee" xmlns:web="http://java.sun.com/xml/ns/javaee/web-app_2_5.xsd"
   xsi:schemaLocation="http://java.sun.com/xml/ns/javaee http://java.sun.com/xml/ns/javaee/web-app_3_0.xsd"
   id="WebApp_ID" metadata-complete="true" version="3.0">
   <display-name>WAR deployment</display-name>

   <context-param>
      <description>eve configuration (yaml file)</description>
      <param-name>eve_config</param-name>
      <param-value>eve.yaml</param-value>
   </context-param>

   <listener>
      <listener-class>com.almende.eve.test.MyListener</listener-class>
   </listener>

   <servlet>
      <servlet-name>war</servlet-name>
      <servlet-class>com.almende.eve.transport.http.DebugServlet</servlet-class>
      <init-param>
         <param-name>ServletUrl</param-name>
         <param-value>http://localhost:8080/war/agents/</param-value>
      </init-param>
      <load-on-startup>1</load-on-startup>
   </servlet>

   <servlet-mapping>
      <servlet-name>war</servlet-name>
      <url-pattern>/agents/*</url-pattern>
   </servlet-mapping>
</web-app>
{% endhighlight %}

The agent configuration is similar to the standalone situation. This is placed in a file called eve.yaml, in the WEB-INF folder of your webproject (besides the web.xml). In the web.xml shown above this file is provided as a context-param, called "eve_config".
{% highlight yaml %}
#First we define a template for usage per agent. The ExampleAgent extends Eve's Agent class.
templates:
   defaultAgent:
      class: com.almende.eve.agent.ExampleAgent
      state:
         class: com.almende.eve.state.memory.MemoryStateBuilder
      scheduler:
         class: com.almende.eve.scheduling.SimpleSchedulerBuilder
      transport:
         class: com.almende.eve.transport.http.HttpTransportBuilder
         servletUrl: http://localhost:8080/war/agents/
         doAuthentication: false

#Here we define the agents themselves:
agents:
-  id: example
   extends: templates/defaultAgent
-  id: another
   extends: templates/defaultAgent

{% endhighlight %}

To initialize the agents using the above configuration, the following ServletListener can be used. This listener is also configured in the above web.xml example.
{% highlight java %}
public class MyListener implements ServletContextListener {

   @Override
   public void contextInitialized(ServletContextEvent sce) {
      final ServletContext sc = sce.getServletContext();
      
      // Get the eve.yaml file and load it. (In production code this requires some serious input checking)
      String path = sc.getInitParameter("eve_config");
      final String fullname = "/WEB-INF/" + path;
      final InputStream is = sc.getResourceAsStream(fullname);
      final Config config = YamlReader.load(is).expand();
			
      // Config is now a Jackson JSON DOM, 'expand()' allows for template resolvement in the configuration.

      // Now we instantiate two example agents, getting their classpath (and configuration) from the DOM
      final ArrayNode agents = (ArrayNode) config.get("agents");
      for (final JsonNode agent : agents) {
         final AgentConfig agentConfig = new AgentConfig((ObjectNode) agent);
         final Agent newAgent =
            new AgentBuilder()
            .with(agentConfig)
            .build();
         System.out.println("Created agent:" + newAgent.getId());
      }
   }
}
{% endhighlight %}

The Maven configuration is completely standard for war projects, with the following pom.xml, nothing special here.
{% highlight xml %}
<project xmlns="http://maven.apache.org/POM/4.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
   xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
   <modelVersion>4.0.0</modelVersion>

   <groupId>com.almende.eve.example</groupId>
   <artifactId>war-example</artifactId>
   <version>3.0.0-SNAPSHOT</version>
   <packaging>war</packaging>

   <properties>
      <eve.version>3.0.0-SNAPSHOT</eve.version>
   </properties>

   <dependencies>
      <dependency>
         <groupId>com.almende.eve</groupId>
         <artifactId>eve-bundle-full</artifactId>
         <version>${eve.version}</version>
      </dependency>
   </dependencies>
</project>
{% endhighlight %}


## Android app {#android}

TODO:

- Example project layout
- Example service
- Configuration?

For now you can check out the demo application for PAAMS 2014 at: [ConferenceApp](https://github.com/almende/demoapps/tree/master/conferenceApp)


