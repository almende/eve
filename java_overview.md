---
layout: default
title: Overview
---

# Overview

Eve has libraries available for Java. 
This makes it very easy to create and run your own agents.
The Eve libraries can be added to an existing Java project, 
and require almost no configuration.
Agents are regular Java classes decorated with some annotations. 
The agents are hosted via a regular web servlet.


## Getting Started {#gettingstarted}

The page [Getting Started](java_gettingstarted.html) gives a detailed tutorial
on how to set up a Java project running your self created Eve agent in the cloud, 
using Google App Engine.


## Libraries {#libraries}

The Java version of Eve consists of a number of libraries, which can be used
as building blocks to build and run your own agent platform. 
The libraries are spread over three different layers:

- A core layer containing the required core functionality for creating and 
  hosting agents.
- A platform Layer, with code specific for different deployment platforms.
- An application layer containing the actual implementations of agents.

The libraries are explained in detail on the page 
[Libraries](java_libraries.html).


## Hosting {#hosting}

In Java, Eve agents are hosted via a web servlet. 
Eve comes with two ready-made servlets, one for hosting a single agent, and 
one for hosting multiple agents and agent classes.
It is possible to create a custom servlet when necessary.

The web servlets can be configured to host a number of agent classes, and to 
offer a specific type of persistent context and a scheduler for the agents. 
The type of context and scheduler depends on the platform where the web servlet
is hosted. That can be a java server such as Tomcat, or a cloud solution
such as Google App Engine.

More on hosting is explained at the page [Hosting](java_hosting.html).


## Agents {#agents}

An Eve agent is created as a regular Java class. 
Its methods will be exposed via JSON-RPC.
Agents themselves are stateless, and it is possible to have multiple
instances of the same agent running simultaneously in the cloud.
An agent can store its state in a context, which offers a simple 
key/value storage.

The agents are explained in detail on the page 
[Agents](java_agents.html).


