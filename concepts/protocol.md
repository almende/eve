---
layout: default
title: Communication protocol
---

# Protocol

Eve defines an interaction protocol between agents. This protocol consists of a common RPC language and some expected behavioral patterns. In this section of the documentation this interaction protocol is described:

- [Communication Protocol](#protocol) describes the RPC language for the agents.
- [Required methods](#required) describes the standard methods of an agent.
- [Management methods](#management) describes optional methods that an implementation should make available to administrators and developers.
- [Interaction patterns](#patterns) describes common behavior that implementations should provide to agents, with associated methods.
- [Documentation](#Documentation) links to resources related to the JSON-RPC
  protocol.

## Communication Protocol {#protocol}

Eve agents communicate with each other using the [JSON-RPC](#Documentation) protocol. This is a simple and readable protocol, using JSON to format requests and responses. JSON (JavaScript Object Notation) is a lightweight, flexible data-interchange format. It is easy for humans to read and write,
and easy for machines to parse and generate. JSON-RPC version 2.0 is the minimally required version, because Eve uses named parameters. 

A request from Agent X to agent Y can look as follows.
Agent X addresses method "add" from agent Y, and provides two values
as parameters.
Agent Y executes the method with the provided parameters, and returns the result.


<table class="example" summary="Synchronous request">
<tr>
<th class="example">Url</th><td class="example"><pre class="example">http://myserver.com/agents/agent_y</pre></td>
</tr>
<tr>
<th class="example">Request</th><td class="example"><pre class="example">{
    "jsonrpc":"2.0",
    "id": 1,
    "method": "add",
    "params": {
        "a": 2.2,
        "b": 4.5
    }
}</pre></td>
</tr>
<tr>
<th class="example">Response</th><td class="example"><pre class="example">{
    "jsonrpc":"2.0",
    "id": 1,
    "result": 6.7,
    "error": null
}</pre></td>
</tr>
</table>

## Required methods {#required}

- getId()
- getMethods()
- getUrls()

## Management methods {#management}

- scheduler.getTasks()

## Interaction patterns {#patterns}

### Publish-subscribe model

- event.createSubscription()
- event.deleteSubscription()

### Result monitor model

- monitor.registerPush()
- monitor.unregisterPush()

## Documentation {#documentation}

Documentation on the JSON-RPC protocol can be found via the following links:

- [http://www.json.org](http://www.json.org)
- [http://json-rpc.org](http://json-rpc.org)
- [http://jsonrpc.org](http://jsonrpc.org)
- [http://en.wikipedia.org/wiki/Json](http://en.wikipedia.org/wiki/Json)
- [http://en.wikipedia.org/wiki/JSON_RPC](http://en.wikipedia.org/wiki/JSON_RPC)


