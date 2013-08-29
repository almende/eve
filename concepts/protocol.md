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

<table>
<tr>
    <th>Method</th>
    <th>Parameters</th>
    <th>Result</th>
    <th>Description</th>
</tr>
<tr>
    <td>getId</td>
    <td>none</td>
    <td>String id</td>
    <td>Retrieve the agents id. An agent can have multiple urls, but always
    has one unique id. The id of the agent is not globally unique,
    agents running on different platforms may have the same id.</td>
</tr>
<tr>
    <td>getUrls</td>
    <td>none</td>
    <td>String[&nbsp;] urls</td>
    <td>Retrieve an array with the agents urls. An agent can have multiple urls
    for different transport services such as HTTP and XMPP.</td>
</tr>
<tr>
    <td>getMethods</td>
    <td>none</td>
    <td><a href="#MethodDescription">MethodDescription</a>[&nbsp;] methods</td>
    <td>Retrieve a list with all the available methods.</td>
</tr>
</table>

### MethodDescription {#MethodDescription}

The method `getMethods` of Eve agents returns an array with method descriptions.
The method descriptions have the following structure:

<table>
    <tr>
        <th>Field</th>
        <th>Type</th>
        <th>Description</th>
    </tr>
    <tr>
        <td>method</td>
        <td>String</td>
        <td>Method name</td>
    </tr>
    <tr>
        <td>params[ ]</td>
        <td>Array</td>
        <td>Array with parameters</td>
    </tr>
    <tr>
        <td>params[ ].name</td>
        <td>String</td>
        <td>Parameter name</td>
    </tr>
    <tr>
        <td>params[ ].type</td>
        <td>String</td>
        <td>Parameter type</td>
    </tr>
    <tr>
        <td>params[ ].required</td>
        <td>Boolean</td>
        <td>True if the parameter is required, else false.</td>
    </tr>
    <tr>
        <td>result.type</td>
        <td>String</td>
        <td>Method return type.</td>
    </tr>
</table>


For example a method `add(a,b)` can be described as:

    {
        "result": {
            "type": "Double"
        },
        "method": "add",
        "params": [
            {
                "name": "a",
                "required": true,
                "type": "Double"
            },
            {
                "name": "b",
                "required": true,
                "type": "Double"
            }
        ]
    }


## Management methods {#management}

- scheduler.getTasks()

## Interaction patterns {#patterns}

This part of the protocol description defines expected capabilities of Eve agents. These capabilities are optional, but are highly advisable as they offer better system-wide robustness and usability. It may be expected from the various Eve implementations to provide these out-of-the-box.
Currently the following patterns are described:
- [Publish-subscribe model](#pubsub)
- [Result monitor model](#resmon)

### Publish-subscribe model {#pubsub}

#### Description
<div class="highlight">An event is identified as a label. The set of events that an agent can trigger is part of the application that is written, Eve does not define any specific events. Subscribing to an event does not guarantee the existence of such an event, nor if the event will ever be triggered by the agent.</div>

Through the event subscription model, agents can request to be informed when a certain event is triggered. The two agents involved (subscriber and publisher) should share a common list of events. When the publisher triggers the event, it will call the provided callback method of the subscriber. 


#### Public methods
<table>
<tr>
    <th>Method</th>
    <th>Parameters</th>
    <th>Result</th>
    <th>Description</th>
</tr>
<tr>
   <td>onSubscribe</td>
    <td>
        String&nbsp;event,<br>
        String&nbsp;callbackUrl,<br>
        String&nbsp;callbackMethod<br>
    </td>
    <td>String subscriptionId</td>
    <td>Subscribe to an event of this Agent.
    The provided callback url and method will be invoked when the event is
    triggered.
    The callback method is called with the following parameters:
    <table>
        <tr>
            <th>Parameters</th>
            <th>Description</th>
        </tr>
        <tr>
            <td>String subscriptionId</td>
            <td>The id of the subscription</td>
        </tr>
        <tr>
            <td>String event</td>
            <td>Name of the triggered event</td>
        </tr>
        <tr>
            <td>String agent</td>
            <td>Url of the triggered agent</td>
        </tr>
        <tr>
            <td>Object params</td>
            <td>Event specific parameters</td>
        </tr>
    </table>
    </td>
</tr>
<tr>
    <td>onUnsubscribe</td>
    <td>
    String&nbsp;subscriptionId (optional),<br>
    String&nbsp;event (optional),<br>
    String&nbsp;callbackUrl (optional),<br>
    String&nbsp;callbackMethod (optional)<br>
    </td>
    <td>none</td>
    <td>
    Unsubscribe from one of this agents events. All parameters are optional:
    <ul>
        <li>
            If <code>subscriptionId</code> is provided, the subscription with
            this id will be deleted. All other parameters are ignored.
        </li>
        <li>
            If <code>callbackUrl</code> is provided,
            all subscriptions with matching parameters will be deleted.
            If the parameters <code>event</code> and/or
            <code>callbackMethod</code> are provided, subscriptions will be
            filtered by these parameters,
            else, all subscriptions from this agent will be deleted.
        </li>
    </ul>
    </td>
</tr>
</table>


### Result monitor model {#resmon}

#### Description
The result monitor model offers robust, unidirectional synchronization of data. It basically allows an agent to keep up-to-date with the result value of a remote method. Traditionally this is achieved through an event subscription, indicating changed data, followed by a call to the specific method. Care must be taken to make sure the event is delivered to the agent, even in potentially unreliable network environments. A more robust, but less efficient solution would be to poll the method at a fixed interval. The result monitor model combines these solutions into one framework: it allows polling, event subscription and requesting a remote agent to push the result value at a fixed interval. Effectively it makes use of the time autonomy of the agents, making each responsible for trying to keep the data synchronized.

#### Agent behavior
To participate in the result monitor model, the agent should be able to repetitive request a remote method (poll), repetitively push the result of the method to another agent, push the result at a specific event (see [publish-subscribe model](#pubsub)) and potentially cache the result locally for future reference. Implementations are advised to bundle these three elements in one monitor API. As the polling and caching are locally organized, the only public methods are related to requesting the push behavior. The internal interface of this model will be documented in the implementation specific sections.

#### Public methods
<table>
<tr>
    <th>Method</th>
    <th>Parameters</th>
    <th>Result</th>
    <th>Description</th>
</tr>
<tr>
	<td>monitor.registerPush</td>
    <td>
        String&nbsp;pushId,<br>
        ObjectNode&nbsp;pushParams<br>
    </td>
	<td>none</td>
	<td>Request the agent to start pushing the result of the method call, as described in the pushParams. This data should be delivered through a specified callback method.<br>
		The given pushId is used as a key to facilitate the communication, it is chosen by the requesting agent. It can be assumed to be unique per requester. Multiple calls with the same pushId may replace the old push.<br>
		Structure of the pushParams:<br>
		<table><tr><th>field</th><th>description</th></tr>
			<tr><td>String&nbsp;method</td><td>The method to use for obtaining the data to push</td></tr>
			<tr><td>JsonNode&nbsp;params</td><td>The parameters for the method call used for obtaining the data to push</td></tr>
			<tr><td>String&nbsp;callback</td><td>The method where the data should be pushed to. This method should at least have the following parameters:<br>
				String&nbsp;pushId&nbsp;:The original pushId<br>
				JsonNode&nbsp;result&nbsp;:The result data<br>
				Specific implementations can choose to send back more information, like for example the original pushParams and/or event trigger params.
			</td></tr>
			<tr><td>Number&nbsp;interval</td><td>Interval in milliseconds at which the data should be pushed</td></tr>
			<tr><td>String&nbsp;event</td><td>When set, the agent should subscribe this push to the given event. If the event is triggered, the method is called and the result send to the callback.</td></tr>
			<tr><td>Boolean&nbsp;onChange</td><td>When true, the agent should organize some way to only push when the data changes, e.g. through a event or by locally polling and comparing.</td></tr>
		</table>
    </td>
</tr>
<tr>
	<td>monitor.unregisterPush</td>
    <td>
        String&nbsp;pushId
    </td>
	<td>none</td>
	<td>
		This method is called to teardown all setup regarding the push.
	</td>
</tr>
</table>

## Documentation {#documentation}

Documentation on the JSON-RPC protocol can be found via the following links:

- [http://www.json.org](http://www.json.org)
- [http://json-rpc.org](http://json-rpc.org)
- [http://jsonrpc.org](http://jsonrpc.org)
- [http://en.wikipedia.org/wiki/Json](http://en.wikipedia.org/wiki/Json)
- [http://en.wikipedia.org/wiki/JSON_RPC](http://en.wikipedia.org/wiki/JSON_RPC)


