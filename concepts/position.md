---
layout: default
title: Position
---

# Position

This page provides information to help understanding what Eve is, compared to existing agent platforms and architectures.

- [Comparison of approach](#comparison)
- [Note of FIPA compliancy](#fipa)
- [Existing agent platforms](#platforms)


## Comparison of approach {#comparison}

Eve is one of the many multi-agent platforms around.
What makes Eve different from other platforms?

### Traditional approach

Traditional agent platforms consist of a operating system or
simulation environment where agents can live and interact with each other.
Typically, this is a closed and controlled environment.
Agents and applications must be developed such that they can run inside this
environment.

The platform and its agents are normally deployed a single server.
To make the system scalable, the agent platforms implement functionality to
link multiple "sites" or "locations" together,
enabling interaction between agents residing on different sites.
Additionally, solutions for migrating agents from one to another site are
implemented, allowing load balancing of the configured sites.

All well known multi-agent platforms are developed in Java.
This is one of the most used programming languages around,
and allows running the platform on different operating systems (Linux, OS X,
Windows).


### Eve's approach

Eve's approach for multi agent platforms is quite the opposite of traditional
platforms.
The core of Eve is not delivering an a multi-agent platform environment.
The core is defining a standard way for agents to make themselves available
to others and to communicate with each other.

Eve is designed to offer agents a human-like, open world, instead of closed,
controlled laboratory environment.
Eve agents live on the World Wide Web. Each agent is accessible via a
unique url, and uses a prescribed communication protocol.
Eve does not dictate the programming language or the environment in
which an agent must be hosted.
Therefore, Eve agents can be built as a layer *on top* of existing
applications. There is no need to adapt applications in order to fit into Eve,
instead, it is enough to just add a simple layer on top of the application to
connect it to the "Eve world".

Because Eve is not running on a single, prepared server, but rather exists on
the web, the platform can take full advantage of the features of the web.
This automatically gives the platform full scalability.
Agents can be deployed in the cloud, using modern cloud solutions,
and agents can also run on self maintained server.
Although the network transport layer can contain single points of failure,
Eve itself is a distributed agent platform,
with no single point of failure by design.

## A note on FIPA compliancy {#fipa}

The FIPA standards form an important effort to align different agent platforms, aiming at making them interoperable without necessitating further coordination between the independent development teams. These standards were formulated over ten years ago and have since not led to widespread adoption. In their current form they are not useful for the purpose of Eve to be a generic, easy to adopt, domain independent agent platform. Eve has not taken any effort (yet) to adhere to them, although we do use them as inspiration and an important source for nomenclature.

## Platform descriptions {#platforms}

This section gives an overview of a number of known multi-agent platforms.
The platform descriptions are taken from the websites of the concerning
platforms themselves.


### AgentScape

Site: [http://www.agentscape.org/](http://www.agentscape.org/)

AgentScape is a middleware layer that supports large-scale agent systems.
The rationale behind the design decisions are 
(i) to provide a platform for large-scale agent systems, 
(ii) support multiple code bases and operating systems, and 
(iii) interoperability with other agent platforms.

The overall design philosophy is "less is more," that is, the AgentScape 
middleware should provide a minimal but sufficient support for agent 
applications, and "one size does not fit all," that is, the middleware 
should be adaptive or reconfigurable such that it can be tailored to a specific 
application (class) or operating system/hardware platform.
Agents and objects are basic entities in AgentScape. A location is a "place" at 
which agents and objects can reside (see Fig. 1). Agents are active entities in 
AgentScape that interact with each other by message-passing communication. 
Furthermore, agent migration in the form of weak mobility is supported.
Objects are passive entities that are only engaged into computations reactively
on an agent's initiative. Besides agents, objects, and locations, 
the AgentScape model also defines services. Services provide information or 
activities on behalf of agents or the AgentScape middleware.


### Eve

Site: [http://eve.almende.com/](http://eve.almende.com/)

Eve is a multipurpose, web based agent platform.
The project aims to develop an open protocol for communication between software
agents.
Eve is designed as a decentral, scalable system for autonomously acting agents.
Eve uses the existing world wide web as platform, and uses existing protocols
for data exchange (HTTP) and for messaging (JSON-RPC).


### Goal

Site: [https://mmi.tudelft.nl/trac/goal/wiki](https://mmi.tudelft.nl/trac/goal/wiki)

GOAL is an agent programming language for programming rational agents.
GOAL agents derive their choice of action from their beliefs and goals.
The language provides the basic building blocks to design and implement rational
agents. The language elements and features of GOAL allow and facilitate the
manipulation of an agent's beliefs and goals and to structure its
decision-making. The language provides an intuitive programming framework
based on common sense notions and basic practical reasoning.


### Jade

Site: [http://jade.tilab.com/](http://jade.tilab.com/)

JADE (Java Agent DEvelopment Framework) is a software framework to develop
agent-based applications in compliance with the FIPA specifications for 
interoperable intelligent multi-agent systems. The goal is to simplify the 
development while ensuring standard compliance through a comprehensive set of 
system services and agents. 
JADE can then be considered an agent middleware that implements an 
Agent Platform and a development framework. It deals with all those aspects 
that are not peculiar of the agent internals and that are independent of the 
applications, such as message transport, encoding and parsing, or agent 
life-cycle.


### Janus

Site: [http://www.janus-project.org/Home](http://www.janus-project.org/Home)

Janus is an enterprise-ready open-source multi-agent platform fully
implemented in Java 1.6. Janus enables developers to quickly create web,
enterprise and desktop multiagent-based applications. It provides a
comprehensive set of features to develop, run, display and monitor
multiagent-based applications. Janus-based applications can be
distributed across a network. Janus is built upon the CRIO organizational
metamodel and supports the implementation of the concepts of role and
organisation as first-class entities. It also natively manages the concept of
recursive agents or holon.


### Jason

Site: [http://jason.sourceforge.net/](http://jason.sourceforge.net/)

Jason is an interpreter for an extended version of AgentSpeak.
It implements the operational semantics of that language,
and provides a platform for the development of multi-agent systems,
with many user-customisable features. Jason is available Open Source,
and is distributed under GNU LGPL. See more in the Description page.


### MadKit

Site: [http://www.madkit.org/](http://www.madkit.org/)

MadKit is an open source modular and scalable multiagent platform written in
Java and built upon the AGR (Agent/Group/Role) organizational model:
MadKit agents play roles in groups and thus create artificial societies.
MadKit does not enforce any consideration about the internal structure of agents,
thus allowing a developer to freely implements its own agent architectures.

MadKit is a free software based on the GPL/LGPL license featuring:

- Artificial agents creation and life cycle management
- An organizational infrastructure for communication between agents
- High heterogeneity in agent architectures: No predefined agent model
- Multi-Agent based simulation and simulator authoring tools
- Multi-agent based distributed application authoring facilities


