---
layout: default
title: Communication protocol
---

# Communication Protocol

Eve agents communicate with each other using the [JSON-RPC](#Documentation) protocol. This is a simple and readable protocol, using JSON to format requests and responses. JSON (JavaScript Object Notation) is a lightweight, flexible data-interchange format. It is easy for humans to read and write,
and easy for machines to parse and generate. JSON-RPC version 2.0 is the minimally required version, because Eve uses named parameters. 

This page describes:

- [Protocol](#Protocol) describes the communication protocol for the agents.
- [Agent API](#Agent_API) describes the standard methods of an agent.
- [Documentation](#Documentation) links to resources related to the JSON-RPC
  protocol.

## Required methods

- getId()
- getMethods()
- getUrls()

## Management methods

- scheduler.getTasks()


## Interaction patterns

### Publish-subscribe model
- event.createSubscription()
- event.deleteSubscription()

### Result monitor model
- monitor.registerPush()
- monitor.unregisterPush()

## Documentation {#Documentation}

Documentation on the JSON-RPC protocol can be found via the following links:

- [http://www.json.org](http://www.json.org)
- [http://json-rpc.org](http://json-rpc.org)
- [http://jsonrpc.org](http://jsonrpc.org)
- [http://en.wikipedia.org/wiki/Json](http://en.wikipedia.org/wiki/Json)
- [http://en.wikipedia.org/wiki/JSON_RPC](http://en.wikipedia.org/wiki/JSON_RPC)


