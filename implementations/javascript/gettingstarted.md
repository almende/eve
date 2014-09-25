---
layout: default
title: Getting Started
---

# Getting Started

With the Eve library for Node.js, one can create software agents in Javascript and run them in Node.js or in the browser.

This tutorial contains the following steps:

- [Prerequisites](#prerequisites)
- [Install](#install)
- [Use](#use)
- [HelloAgent](#helloagent)
- [Deployment](#deployment)


## Prerequisites {#prerequisites}

To run Eve on Node.js, Node.js must be installed on your system. Node.js can be downloaded from the website [http://nodejs.org/](http://nodejs.org/). There are installers available for Windows, Mac. 


## Install {#install}

Install the module via npm:

    npm install evejs


## Use {#use}

An agent basically has a methods `send`, `receive`, `connect` and `disconnect`.
An agent can be extended with modules like `pattern` and `request`. There is
a central configuration `eve.system` which can be used to load transports. 
The loaded transports can be used by agents to communicate with each other.

To set up a system with eve agents:

- Create an agent class extending `eve.Agent`. A template for a custom agent is:

  {% highlight javascript %}
  var eve = require('evejs');
  
  function MyAgent(id) {
    eve.Agent.call(this, id);
  
    // ...
  }
  
  MyAgent.prototype = Object.create(eve.Agent.prototype);
  MyAgent.prototype.constructor = MyAgent;
  
  MyAgent.prototype.receive = function (from, message) {
    // ...
  };
  
  module.exports = MyAgent;
  {% endhighlight %}

- Configure `eve.system`, load transports and other services.

  {% highlight javascript %}
  eve.system.load({
    transports: [
      {
        type: 'distribus'
      }
    ]
  });
  {% endhighlight %}

- Create an agent:

  {% highlight javascript %}
  var agent1 = new MyAgent('agent1');
  {% endhighlight %}

- Connect an agent to one or multiple transports. This is typically done in
  the agents constructor function:
  
  {% highlight javascript %}
  agent1.connect(eve.system.transports.getAll());
  {% endhighlight %}

- To send and receive messages, each agent has a method `send(to, message)` and `receive(from, message)`. A message can be send to and agent by specifying either the agents full url, or just the agents id. In the latter case, the agent will send the message via the transport marked as *default*.

  {% highlight javascript %}
  agent1.send('distribus://networkId/agent2', 'hello agent2!');
  agent1.send('agent2', 'hello agent2!'); // send via the default transport
  {% endhighlight %}
  
  The *networkId* of a transport can be found at `transport.networkId`.

### HelloAgent {#helloagent}

To create a simple agent class, create a file [**HelloAgent.js**](examples/agents/HelloAgent.js) with the 
following code:

{% highlight javascript %}
var eve = require('evejs');

function HelloAgent(id) {
  // execute super constructor
  eve.Agent.call(this, id);

  // connect to all transports configured by the system
  this.connect(eve.system.transports.getAll());
}

// extend the eve.Agent prototype
HelloAgent.prototype = Object.create(eve.Agent.prototype);
HelloAgent.prototype.constructor = HelloAgent;

HelloAgent.prototype.sayHello = function(to) {
  this.send(to, 'Hello ' + to + '!');
};

HelloAgent.prototype.receive = function(from, message) {
  console.log(from + ' said: ' + JSON.stringify(message));

  if (message.indexOf('Hello') === 0) {
    // reply to the greeting
    this.send(from, 'Hi ' + from + ', nice to meet you!');
  }
};

module.exports = HelloAgent;
{% endhighlight %}

This agent class can be used as follows. Note that the agents talk to each 
other via a `LocalTransport` which is instantiated in `eve.system` by default.

{% highlight javascript %}
var HelloAgent = require('./HelloAgent');

// create two agents
var agent1 = new HelloAgent('agent1');
var agent2 = new HelloAgent('agent2');

// send a message to agent1
agent2.send('agent1', 'Hello agent1!');
{% endhighlight %}


## Deployment {#deployment}

### Node.js

The Node.js application can be deployed on any of the available hosting
solutions for Node.js:

- [Heroku](http://heroku.com/)
- [JoyentCloud](https://joyent.com/)
- [Nodejitsu](http://nodejitsu.com/)
- [Nodester](http://nodester.com/)
- [Windows Azure](http://www.windowsazure.com/en-us/develop/nodejs/)
- others...

### Browser

To use `evejs` in the browser, one can [browserify](http://browserify.org) the library and load it into any browser.
