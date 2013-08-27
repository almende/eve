---
layout: default
title: Introduction
---

{% assign version = '1.1.0' %}

#<center>Eve - the open agent platform</center>
Eve is a multipurpose, web based agent platform.
Eve envisions to be an open and dynamic environment where agents can live and
act anywhere: in the cloud, on smartphones, on desktops, in browsers, robots,
home automation devices, and others. The agents communicate with each other using simple, existing protocols
(JSON-RPC) over existing transport layers (HTTP, XMPP), offering a language and platform agnostic solution.
[Discover more](global_introduction.html) in the architecture section.

## Multiple variants

Currently there are two main implementations of Eve: A [Java](java_overview.html) version and a [Javascript](nodejs_overview.html) version. The Java version is more mature and has more features available. Ongoing work is done to get the Javascript implementation at the same level and provide other languages as well. Effectively any language that has good library support for JSON-RPC can be used to implement Eve.

Due to the current maturity of the Java implementation, this site focusses somewhat exclusively on this version. This is definitely going to change as other implementations getting more mature.

## Download

The easiest way to get Eve for Java is through Maven:

<div class="code"><pre><span class="nt">&lt;dependency&gt;</span>
    <span class="nt">&lt;groupId&gt;</span>com.almende.eve<span class="nt">&lt;/groupId&gt;</span>
    <span class="nt">&lt;artifactId&gt;</span>eve-core<span class="nt">&lt;/artifactId&gt;</span>
    <span class="nt">&lt;version&gt;</span>{{version}}<span class="nt">&lt;/version&gt;</span>
<span class="nt">&lt;/dependency&gt;</span>
</pre></div>

Eve is entirely open-source (Apache License, version 2.0) so you can also [fork](https://github.com/almende/eve) the project on github.
Full information on Java downloads can be found on the [Java downloads](java_downloads.html) section.

## Why?
<div class="highlight">
<span>Why software agents?</span><br>
Designing robust, distributed software systems is notoriously hard. At the same time, humans are pretty good at distributing work among themselves and forming robust organizations and/or teams. Designing and using software agents is inspired by such human organizations, making software designs fit closer to our collective experience, providing a boost in stability, scalability and maintainability of the software system.
</div>

Eve aims to make it very easy to use the concept of software agents in your projects. The main reasons for using software agents is to achieve various goals:

- Robustness: Get system robustness through loosely coupled, asynchronous, self-contained elements, 
- Flexibility: Flexibly add and remove system capabilities
- Reduced complexity: Reduce the complexity of designing, developing and managing a distributed software system.
- Scalability: By designing the application around the local "worldview" of the agents, no scalability limitations are imposed by the shared datastructures and locking requirements.

Through it's service-offering architecture, Eve offers agents that are very simple to develop. You can also turn your existing APIs into software agents, allowing your API to be accessed asynchronous, 


## About us

Eve is being developed by [Almende](http://www.almende.com),
a Dutch research company specialized in information and communication technologies.
At the core of all Almende solutions are hybrid agent networks: humans and computers working together.
Almende looks towards agent technology to develop smart software that truly supports people in organizing their own lives.



