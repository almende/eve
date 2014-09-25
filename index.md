---
layout: default
title: Introduction
---

#Eve - a web-based agent platform


<div style="float: right;">
    <div id="dropdown-1" class="dropdown dropdown-tip">
        <ul class="dropdown-menu">
            <li><a href="implementations/java/introduction.html#Maven">Eve Java</a></li>
            <li><a href="implementations/javascript/introduction.html#install">Eve JavaScript</a></li>
        </ul>
    </div>
    <a class="download" href="#" title="Download Eve" data-dropdown="#dropdown-1">Download &nbsp; &#x25BC;</a>
</div>

Eve is a multipurpose, web-based agent platform.
Eve envisions to be an open and dynamic environment where agents can live and
act anywhere: in the cloud, on smartphones, on desktops, in browsers, robots,
home automation devices, and others. The agents communicate with each other using simple, existing protocols
(JSON-RPC) over existing transport layers (HTTP, XMPP), offering a language and platform agnostic solution.
Discover more in the [concepts section](concepts/introduction.html).

<div style="text-align: center;">
    <a href="img/eve_overview.png" data-lightbox="eve_overview" target="_blank">
        <img src="img/eve_overview_small.png" alt="schematic overview" title="Click for a larger view"
            style="border: 1px solid lightgray;">
        <br>
        <i>(click for a larger overview)</i>
    </a>

</div>

## Implementations

Effectively, Eve can be implemented in any language that has good library support for JSON-RPC.
Currently there are two main implementations of Eve:

- A mature and feature rich [Java](implementations/java/introduction.html) version.
- A [Javascript](implementations/javascript/introduction.html) version in early stage.
- In the future, implementations in other languages will follow.

There is extensive documentation for each of the implementations available on this website.

## Why?
<div class="Evehighlight">
<span>Why software agents?</span><br>
Designing robust, distributed software systems is notoriously hard. At the same time, humans are pretty good at distributing work among themselves and forming robust organizations and/or teams. Designing and using software agents is inspired by such human organizations, making software designs fit closer to our collective experience, providing a boost in stability, scalability and maintainability of the software system.
</div>

Eve aims to make it very easy to use the concept of software agents in your projects. The main reasons for using software agents is to achieve various goals:

- Robustness: Get system robustness through loosely coupled, asynchronous, self-contained elements, 
- Flexibility: Flexibly add and remove system capabilities
- Reduced complexity: Reduce the complexity of designing, developing and managing a distributed software system.
- Scalability: By designing the application around the local \"worldview\" of the agents, no scalability limitations are imposed by the shared datastructures and locking requirements.

Through it\'s service-offering architecture, Eve offers agents that are very simple to develop. You can also turn your existing APIs into software agents, allowing your API to be accessed asynchronous, 


## Open source {#opensource}

Eve is an open platform, and it is encouraged to help extending the
platform by providing new libraries with agents, or create implementations in
other programming languages, or improve existing code.
One of the key issues for the platform to be successful is that it needs to be
accessible, open, and easy to implement for any developer in any development
environment.

Offering the software as open source is a logic result of the aims for
openness and collaborative development.


## About us {#about_us}

Eve is being developed by [Almende](http://www.almende.com),
a Dutch research company specialized in information and communication technologies.
At the core of all Almende solutions are hybrid agent networks: humans and computers working together.
Almende looks towards agent technology to develop smart software that truly supports people in organizing their own lives.



