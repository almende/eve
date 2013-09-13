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
  an application on Google App Engine or on Android. In the future, libraries for
  Windows Azure and Amazon will be added.
  To deploy an application on a another platform, all is needed is to switch
  the used platform library.

- **Application Layer**  
  The application layer contains the actual implementations of agents.
  The agents are independent from the deployment platform.
  Eve will come with libraries containing agents that acting on the area of planning, 
  calendaring, and a number of agent that wrap existing web services. 
  

The image below shows a schematic overview of the layers and libraries. 

![/eve/img/java_libraries.png](/eve/img/java_libraries.png)

## Libraries {#libraries}

The following Java libraries are currently available.  
They can be downloaded at the
[downloads page](downloads.html).

- **eve-core.jar**  
  This is the basis of Eve, and is always required.
  It contains abstract base classes for agents and persistency, 
  offers functionality for communication between agents, 
  and contains a number of ready-made servlets to host the agents. 
  The core is independent from the deployment platform.

- **eve-gae.jar**
  Library needed for hosting Eve agents on Google App Engine.
  This library contains platform dependent functionality:
   a persistent state for the agents (DatastoreStateFactory),
  and a scheduler (AppEngineSchedulerFactory).

- **eve-android.jar**
  Library making Eve available on Android.

- **eve-planning.jar**  
  Contains agents acting on the domain of calendaring and planning.
  Contains a set of agents using various Google API's such as calendar,
  directions, translate. Also contains a servlet for authorizing agents.
