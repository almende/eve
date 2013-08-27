---
layout: default
title: Getting Started
---

{% assign eve_nodejs_package = 'eve-nodejs-0.1' %}


# Getting Started

The Eve library for Node.js makes it easy to create software agents in
Javascript and host them using Node.js.

The tutorial contains the following steps:

- [Prerequisites](#prerequisites)
- [Usage](#usage)
- [Create your own agent](#create_your_own_agent)
- [Deployment](#deployment)


## Prerequisites {#prerequisites}

To run Eve on Node.js, Node.js must be installed on your system,
and the Eve library must be downloaded.

- Node.js can be downloaded from the website
  [http://nodejs.org/](http://nodejs.org/).
  There are installers available for Windows, Mac. To install on Linux,
  enter `sudo apt-get install nodejs` in a terminal.

- Download the Eve library [{{eve_nodejs_package}}.zip]({{eve_nodejs_package}}.zip).
  Unzip it somewhere on your system.


## Usage {#usage}

- Open a terminal, and go to the directory where you unzipped the the Eve library.

- Move to the folder `src`. Start Eve via

      node server.js

  Eve is now running at http://localhost:1337/. If needed, another port number
  can be specified on the command line.

- Agents can be accessed via an HTTP POST request. The url defines
  the location of the agent and looks like
  http://server/servlet/agentclass/agentid.
  The body of the post request must contain a JSON-RPC message.
  To execute HTTP requests you can use a REST client like
  [Postman](https://chrome.google.com/webstore/detail/fdmmgilgnpjigdojojpjoooidkmcomcm) in Chrome,
  [RESTClient](https://addons.mozilla.org/en-US/firefox/addon/restclient/?src=search) in Firefox,
  or with a tool like [cURL](http://curl.haxx.se/).

  Perform an HTTP POST request to the GoogleDirectionsAgent on the url

      http://localhost:1337/GoogleDirectionsAgent

  With request body:

      {
          "id":1,
          "method": "getDurationHuman",
          "params": {
              "origin": "Rotterdam",
              "destination": "Utrecht"
          }
      }

  This request will return the following response:

      {
        "jsonrpc": "2.0",
        "id": 1,
        "result": "52 mins",
        "error": null
      }


## Create your own agent {#create_your_own_agent}

Now, what you want of course is create your own agents.
This is quite easy: create a javascript file containing a constructor which
extends from the base class Agent,
and register your agent class in the server.js file.

- Create a Javascript file named MyFirstAgent.java, and store it in
  the folder src/agent (where the example agents like GoogleDirectionsAgent are
  located too). Put the following contents in the file:

      var Agent = require('./Agent.js');

      /**
       * @constructor MyFirstAgent
       */
      MyFirstAgent = function () {
          this.type = 'MyFirstAgent';
      };

      // inherit from base constructor Agent
      MyFirstAgent.prototype = new Agent();

      MyFirstAgent.prototype.getDescription = function (params, callback, errback) {
          callback("This is my first agent");
      };

      MyFirstAgent.prototype.echo = function (params, callback, errback) {
          callback(params.message);
      };

      MyFirstAgent.prototype.add = function (params, callback, errback) {
          callback(params.a + params.b);
      };

      // nodejs exports
      module.exports = MyFirstAgent;

  Each agent must specify it's type in the constructor, and must have a
  method getDescription.
  Each method contains three arguments: `params`, `callback`, and `errback`.
  - `params` contains the raw parameters provided in the JSON-RPC call.
  - `callback` is a callback function which needs to be executed on success,
    with the method result as parameter.
  - `errback` is a callback function which must be called in case of an error,
    with an error message as argument.

- In order to make this agent available, it has to be configured in the
  file src/server.js.

  - Add the file MyFirstAgent.js to the imports:

        ...
        CalcAgent             = require('./agent/CalcAgent.js'),
        GoogleDirectionsAgent = require('./agent/GoogleDirectionsAgent.js'),
        GoogleCalendarAgent   = require('./agent/GoogleCalendarAgent.js'),
        UserAgent             = require('./agent/UserAgent.js'),
        MyFirstAgent          = require('./agent/MyFirstAgent.js');
        ...

  - Add the agent constructor MyFirstAgent to the list with registered agents:

        ...
        eve.add(CalcAgent);
        eve.add(UserAgent);
        eve.add(GoogleDirectionsAgent);
        eve.add(GoogleCalendarAgent);
        eve.add(MyFirstAgent);
        ...

- Now you can (re)start the Node.js application,
  and perform an HTTP POST request to the url

      http://localhost:1337/MyFirstAgent

  With as request:

      {
          "id": 1,
          "method": "echo",
          "params": {
              "message": "Hello World"
          }
      }

  which returns:

      {
          "id": 1,
          "result": "Hello World",
          "error": null
      }

  or send the following request:

      {
          "id": 1,
          "method": "add",
          "params": {
              "a": 2.1,
              "b": 3.5
          }
      }

  which returns:

      {
          "id": 1,
          "result": 5.6,
          "error": null
      }


## Deployment {#deployment}

The Node.js application can be deployed on any of the available hosting
solutions for Node.js:

- [JoyentCloud](https://no.de/)
- [Nodejitsu](http://nodejitsu.com/)
- [Nodester](http://nodester.com/)
- [Windows Azure](http://www.windowsazure.com/en-us/develop/nodejs/)
- others...
