---
layout: default
title: Introduction
---

{% assign version = '3.0.0' %}

# Introduction

<div class="Evehighlight">
<span>Toolkit of capabilities</span><br>
Eve in Java has evolved significantly since its original creation. 
Starting from release 3.0 Eve is a toolkit to add agent 
capabilities to your POJOs. There is no longer a host-environment which needs to be configured,
nor are you forced to extend the Agent class. You can easily pick capabilities
that suit your particular need, the capabilities carry no interdependencies, nor 
assumptions about the structure of your agents. However, for ease of use, there is
still the option of extending the Agent class, but this is no longer a requirement.
</div>

This is the home of the Java implementation of Eve. Through Eve you can either develop agent solutions from scratch or add agent capabilities to your existing Java application. Eve consists of a series of Java library projects, that provide agent capabilities to your POJOs. For dedicated agent applications, there are also various standard Agent classes, which act like templates to the capabilities.<br>

Our build environment is managed through Maven and we are deployed to [Maven Central](#Maven). Another possibility to obtain 
Eve is through a code checkout from [Github](https://github.com/almende/eve).

The documentation consists of a couple of sections:

* [Getting started](getting_started.html) - A tutorial into using Eve in your projects
* [Code structure](#Structure) - A structural overview of the available code in the Git repository
* [Agents](agents.html) - The out-of-the-box agent classes of Eve. These agents combine a basic set of capabilities, in a standardized manner.
* [Capabilities](capabilities.html) - A reference/code example overview of the available capabilities.

Besides this documentation, the best place to get familiar with Eve code is by looking through the ["/tests/src/test/java/com/almende/eve/test/*"](https://github.com/almende/eve/tree/development/tests/src/test/java/com/almende/eve/test) code.

## Maven {#Maven}

To add the Eve libraries to a maven project, add one of the following dependencies to the projects
pom.xml file. Although it is possible to add the libraries independently, it is highly advisable to
use one of the bundle packages.

<div id="tabs">
	<ul>
		<li><a href="#tabs-1">Full</a></li>
		<li><a href="#tabs-2">Full Embed</a></li>
		<li><a href="#tabs-3">Android</a></li>
		<li><a href="#tabs-4">Android ws</a></li>
	</ul>
	<div id="tabs-1">
This is a bundle incorporating all Eve libraries, aimed to be included in an existing webapplication.

{% highlight xml %}
<dependency>
    <groupId>com.almende.eve</groupId>
    <artifactId>eve-bundle-full</artifactId>
    <version>{{version}}</version>
</dependency>
{% endhighlight %}
	</div>
	<div id="tabs-2">
This is a bundle incorporating all Eve libraries, including an embedded Jetty setup.
This bundle is aimed to provide a fully standalone setup.

{% highlight xml %}
<dependency>
    <groupId>com.almende.eve</groupId>
    <artifactId>eve-bundle-full-embed</artifactId>
    <version>{{version}}</version>
</dependency>
{% endhighlight %}
	</div>

	<div id="tabs-3">
This is a bundle for using Eve on Android devices, using XMPP as the transport.

{% highlight xml %}
<dependency>
    <groupId>com.almende.eve</groupId>
    <artifactId>eve-bundle-android</artifactId>
    <version>{{version}}</version>
</dependency>
{% endhighlight %}
	</div>

	<div id="tabs-4">
This is a bundle for using Eve on Android devices, using Websockets as the transport.

{% highlight xml %}
<dependency>
    <groupId>com.almende.eve</groupId>
    <artifactId>eve-bundle-android-ws</artifactId>
    <version>{{version}}</version>
</dependency>
{% endhighlight %}
	</div>

</div>

## Library structure {#Structure}

Eve is structured around a common Capability model. Various agent capabilities are provided: State storage, scheduling, transports, agent lifecycle, etc. These capabilities are provided in Java libraries.

The libraries have the following top-level structure:
<ul>
	<li><b>eve_parent</b> - Parent project, compile the entire project from this folder</li>
	<li><b>eve_common</b> - Capability model, utilities</li>
	<li><b>states</b>
		<ul>
			<li><b>eve_state</b> - State interfaces, Memory and File State implementations</li>
			<li><b>eve_state_couch</b> - CouchDB State implementation</li>
			<li><b>eve_state_mongo</b> - MongoDB State implementation</li>
 		</ul>
	</li>
	<li><b>transports</b>
		<ul>
			<li><b>eve_transport</b> - Transport interfaces, Local transport</li>
			<li><b>eve_transport_common</b> - Common code for HTTP and Websocket transports</li>
			<li><b>eve_transport_http</b> - HTTP transport, contains o.a. EveServlet</li>
			<li><b>eve_transport_ws</b> - WebSocket transport, JSR 356</li>
			<li><b>eve_transport_http_jetty</b> - Embedded Jetty setup</li>
			<li><b>eve_transport_xmpp</b> - XMPP transport implementation</li>
			<li><b>eve_transport_zmq</b> - ZMQ transport implementation, only works correctly on Linux 64bit, due to dependencies</li>
		</ul>
	</li>
	<li><b>scheduling</b>
		<ul>
			<li><b>eve_scheduling</b> - Scheduler interfaces, Non-persistent scheduler</li>
			<li><b>eve_persistent_scheduler</b> - Persistent scheduler, remembers tasks in some state storage</li>
		</ul>
	</li>
	<li><b>transforms</b>
		<ul>
			<li><b>eve_transform</b> - Transform interfaces</li>
			<li><b>eve_transform_rpc</b> - JSON-RPC engine</li>
		</ul>
	</li>
	<li><b>eve_wake</b> - Framework to let agents wake on requests</li>
	<li><b>eve_agents</b> - Agent implementations, can be extended as base of your own agents</li>
	<li><b>tests</b> - Coverage tests and example code</li>
	<li><b>demos</b>
		<ul>
			<li><b>eve_gol_demo</b> - Conway's game of live demo, using Eve agents</li>
		</ul>
	</li>
	<li><b>bundles</b> - Repackaged library bundles, for simpler Maven configuration
		<ul>
			<li><b>eve_full</b> - All Eve libraries combined, exept for the embedded Jetty and the ZMQ transport</li>
			<li><b>eve_full_embed</b> - All Eve libraries combined, including the embedded Jetty</li>
			<li><b>eve_android</b> - Eve libraries for usage on Android, contains the XMPP transport with dependencies</li>
			<li><b>eve_android_ws</b> - Eve libraries for usage on Android, contains the websocket transport</li>
		</ul>
	</li>
</ul>







