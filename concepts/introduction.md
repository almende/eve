---
layout: default
title: Concepts Introduction
---

# Concepts Introduction {#top}

Eve is a multipurpose, web based agent platform, in which existing web technologies are used to provide an environment in which software agents can be developed. Eve is defined as an agent model and a communication protocol, which can be implemented in many programming languages en runtime infrastructures. This part of the documentation provides an introduction into these generic elements of Eve, with separate sections dedicated to the existing implementations.

This page has the following paragraphs:
- [Agent definition](#agentdefinition)
- [Protocol driven](#protocoldriven)
- [Multi-platform](#multiplatform)
- [Open Source](#opensource)


## "Agent" definition {#agentdefinition}

For a good understanding of Eve, it is important to look at it's concept "Agent". The basic definition of agent is: A software entity that represents existing, external, sometimes abstract, entities. Examples of such entities are: human beings, physical objects, abstract goals, etc. To be able to function, the agent needs to be able to run independently of the entity it represents. This autonomous behavior requires a basic set of features, which Eve provides for its agents. These features are: 
- **time independence**, own scheduling, independent of the represented entity.
- **memory**, the possibility to keep a model of the state of the world
- **communication**, a common language to communicate between agents

Eve provides these features as services to the agents, therefor the implementation of the agent can focus on the domain specific logic and data management.

<img src="/eve/img/eve_agent.png"
  style="margin-top: 30px;width:75%;margin-left:auto;margin-right:auto;display:block" 
  title="Eve agentmodel infograph">

The main reason for providing a separate memory service to the agents is that, in most implementations, Eve agents have a request based lifecycle. The agent is only instantiated to handle a single incoming request and is destroyed again as soon as the request has been handled. Only the externally stored state is kept between requests. The agent identity is formed by the address of its state, not by an in-memory, running instance of some class. The clock-scheduler service provides requests at scheduled times, thus instantiating the agent at that time.

<div class="highlight">
<span style="font-weight:bold">Implementation caveat</span><br>
The model has some design constrains that are important to understand: It is possible that multiple instances of the same agent are running simultaneously and in some cases it is even possible that multiple threads access the same instance in parallel. Besides the provided state service (and potential external databases) an agent should be stateless, without global or class level scoped variables. It is possible in some implementations, to cache bootstrap information in the agent instance, but there is no guarantee that the instance remains active, nor that the next request ends up in the same instance (or even in the same execution environment!). If cache structures are used, be certain to make them completely thread-safe!
</div>

This model mimics the way modern webservers handle servlets, allowing any servlet to become an Eve agent. Out of the box, a single agent (=single state) can therefor execute requests in parallel, multi-threaded, and (depending on the state-implementation) distributed among multiple servers. This model also allows for easier handling of asynchronous agent designs.


## Protocol driven {#protocoldriven}

The agent model is based on request driven instantiation. The requests, that trigger this instantiation, are JSON-RPC encoded. In the [Protocol](/eve/concepts/protocol.html) section, this protocol is further defined and described. Using JSON as its base, Eve is highly programming language independent; for most mainstream languages there are existing JSON handling libraries. Any implementation that adheres to the described protocol and basic agent model, is therefor considered an Eve implementation. If the implementation also provides a common transport service, its agents can work flawlessly together with agents in other implementations. For example, this allows a Java cloud agent to communicate with an in-browser Javascript agent, through JSON-RPC over an XMPP transport.

## Multi-platform {#multiplatfrom}
	Transport layers allow various environments
	Servlet, mobile, in browser

## Open source {#opensource}

Eve is an open platform, and it is encouraged to help extending the
platform by providing new libraries with agents, or create implementations in
other programming languages, or improve existing code. 
One of the key issues for the platform to be successful is that it needs to be
accessible, open, and easy to implement for any developer in any development
environment. 

Offering the software as open source is a logic result of the aims for 
openness and collaborative development.

