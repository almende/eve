---
layout: default
title: Libraries
---


# Libraries


The Java version of Eve consists of a number of libraries, which can be used
as building blocks to build and run your own agent platform. 

## Layers {#layers}

The Java libraries are spread over three different layers:

- **Core Layer**  
  There is one core library, named *eve-core.jar*. This is the basis of Eve, 
  and is always required.
  It contains abstract base classes for agents and persistency, 
  offers functionality for communication between agents, 
  and contains a number of ready-made servlets to host the agents. 
  The core is independent from the deployment platform.

- **Platform Layer**  
  The platform layer contains platform specific functionality for persistence
  and scheduling. The platform libraries offer an interface to the agents for 
  reading/writing data, and for scheduling tasks. 
  There are platform libraries for a regular java web application, for running
  an application on Google App Engine. In the future, libraries for 
  Windows Azure and Amazon will be added.
  To deploy an application on a another platform, all is needed is to swap 
  the used platform library.

- **Application Layer**  
  The application layer contains the actual implementations of agents.
  The agents are independent from the deployment platform.
  Eve will come with libraries containing agents that acting on the area of planning, 
  calendaring, and a number of agent that wrap existing web services. 
  

The image below shows a schematic overview of the layers and libraries. 

![img/java_libraries.png](img/java_libraries.png)

## Libraries {#libraries}

The following Java libraries are currently available.  
They can be downloaded at the
[downloads page](java_downloads.html).

- **eve-core.jar**  
  This is the basis of Eve, and is always required.
  It contains abstract base classes for agents and persistency, 
  offers functionality for communication between agents, 
  and contains a number of ready-made servlets to host the agents. 
  The core is independent from the deployment platform.
  
  *Dependencies: 
    jackson-databind-2.0.0.jar,
    jackson-core-2.0.0.jar, 
    jackson-annotations-2.0.0.jar,
    joda-time-2.1.jar,
    snakeyaml-1.10.jar. Optional (needed for XMPP support): smack.jar, smackx.jar.
    Also depending on servlet-api.jar, which is automatically included when
    the project is set up as a web project.
  *
  
- **eve-google-appengine.jar**  
  Library needed for hosting Eve agents on Google App Engine.
  This libary contains platform dependent functionallity:
   a persistent context for the agents (DatastoreContext), 
  and a schedular (AppEngineSchedular).

  *Dependencies: 
    eve-core.jar,
    twig-persist-2.0-beta4.jar,
    guava-11.0.2.jar.
    Also depending on the Google App Engine libraries, which are automatically
    included when the project is set up as a GAE project.
  *

- **eve-google-services.jar**  
  Contains wrapper agents to make all kind of Google services accessible 
  in the Eve world. The library contains agents such as GoogleCalculatorAgent
  and GoogleDirectionsAgent.

  *Dependencies: 
    eve-core.jar
  *

- **eve-planning.jar**  
  Contains agents acting on the domain of calendaring and planning. 
  Contains a GoogleCalendarAgent and a servlet for authorizing this agent.
  Will contain agents planning activities in a dynamic way.

  *Dependencies: 
    eve-core.jar
  *
