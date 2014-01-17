---
layout: default
title: Introduction
---

# Introduction

Eve has libraries available for Java. 
This makes it very easy to create and run your own agents.
The Eve libraries can be added to an existing Java project, 
and require almost no configuration.
Agents are regular Java classes decorated with some annotations. 
The agents can be hosted via a regular web servlet, or the 
Eve environment can run as a standalone application.

You can always find the latest changelog of releases here:
[changelog.txt](https://github.com/almende/eve/blob/master/java/eve-core/changelog.txt)


## Downloads {#downloads}

All available libraries can be downloaded on the 
[downloads page](downloads.html).


## Getting Started {#gettingstarted}

The page [Getting Started](getting_started.html) gives a detailed tutorial
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
[Libraries](libraries.html).


## Services {#services}

Eve agents can be accessed via various transport services.
Eve supports two services: HttpService and XmppService.

- HttpService exposes agents via a regular Java servlet.
  Agents can be invoked by sending a HTTP POST request to this servlet.
- XmppService allows to connect agents to an XMPP server.
  The agents can be invoked via XMPP.

A single Eve application can have multiple XmppServices and HttpServices configured.
This allows exposure of the agents via multiple transport services at the same time.
An agent can be accessible via both XMPP and HTTP at the same time.

More on services is explained at the page [Services](services.html).


## Agents {#agents}

An Eve agent is created as a regular Java class. 
Its methods will be exposed via JSON-RPC.
Agents themselves are stateless, and it is possible to have multiple
instances of the same agent running simultaneously in the cloud.
An agent can persist data in its state, which offers a simple
key/value storage.

The agents are explained in detail on the page 
[Agents](agents.html).


