---
layout: default
title: Communication protocol
---

# Communication Protocol


Each Eve agent has its own, unique url.
The agents communicate via regular HTTP POST requests,
using the [JSON-RPC](http://en.wikipedia.org/wiki/JSON_RPC) protocol.
This is a simple and readable protocol, using JSON to format requests and responses.
[JSON](http://www.json.org/) (JavaScript Object Notation)
is a lightweight, flexible data-interchange format.
It is easy for humans to read and write,
and  easy for machines to parse and generate.

This page describes:

- [Protocol](#Protocol) describes the communication protocol for the agents.
- [Agent API](#Agent_API) describes the standard methods of an agent.
- [AgentFactory API](#AgentFactory_API) describes an AgentFactory's REST API.
- [Resources](#Resources) describes standardized resources such as calendar
  events and geolocations.
- [Documentation](#Documentation) links to resources related to the communication
  protocol and defined resources.


## Protocol {#Protocol}

Eve agents communicate with each other via the
[JSON-RPC](http://en.wikipedia.org/wiki/JSON_RPC) protocol.
Note that only JSON-RPC 2.0 is supported.
In JSON-RPC 2.0, method parameters are defined as an object with *named* parameters,
unlike JSON-RPC 1.0 where method parameters are defined as an array with *unnamed*
parameters, which is much more ambiguous.

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
    "id": 1,
    "result": 6.7,
    "error": null
}</pre></td>
</tr>
</table>


## Agent API {#Agent_API}

All Eve agents have a set of standard methods available.

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
    <td>getType</td>
    <td>none</td>
    <td>String type</td>
    <td>Retrieve the agents type. This is typically the class name of the agent.</td>
</tr>
<tr>
    <td>getVersion</td>
    <td>none</td>
    <td>String version</td>
    <td>Retrieve the agents version number.</td>
</tr>
<tr>
    <td>getDescription</td>
    <td>none</td>
    <td>String description</td>
    <td>Retrieve a description of the agents functionality.</td>
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

## AgentFactory API {#AgentFactory_API}

Eve applications can use an AgentFactory to manage (create, delete, invoke)
Eve agents. An AgentFactory can be exposed as a REST service.
An AgentFactory's REST interface has the following API:

- `GET /agents/`

  Returns information on how to use the REST API.

- `GET /agents/{agentId}`

  Returns an agents web interface, allowing easy interaction with the agent.
  A 404 error will be returned when the agent does not exist.

- `POST /agents/{agentId}`

  Send an RPC call to an agent.
  The body of the request must contain a JSON-RPC request.
  The addressed agent will execute the request and return a
  JSON-RPC response. This response can contain the result or
  an exception.
  A 404 error will be returned when the agent does not exist.

- `PUT /agents/{agentId}?type={agentType}`

  Create an agent. `agentId` can be any string. `agentType` describes the
  type of the agent, for example a Java class path. A 500 error will be
  thrown when an agent with this id already exists.

- `DELETE /agents/{agentId}`

  Delete an agent by its id.



## Resources {#Resources}

The JSON-RPC protocol does define a mechanism for invoking an agents methods.
It does not define the structure of any resources.
To get interoperability between the agents,
it is important to use a single data-structure for resources
such as calendar events, geolocations, and others.

The standardized resources for Eve agents are described in this section.


### Activity

An activity is a base entity which can describe different types of activities:
meetings, appointments, tasks, and more.

An activity consists of two parts:

- Constraints, containing a description of boundary conditions of people,
  resources, time, and location.
- Status, describing the current status. The interpretation itself
  (such as a recurring event, a group composition, or a geolocation)
  is not processed by the activity itself, but by a calendaring system or
  a software agent which manages the activity.
  Note that the status of an Activity is compatible with a Calendar Event
  description.

An example of an activity:

    {
        "summary": "Meeting on project X",
        "description": "Extensive description of the meeting",
        "agent": "https://eveagents.appspot.com/agents/meetingagent_1/",
        "constraints": {
            "attendees": [
                {
                    "displayName": null,
                    "email": null,
                    "agent": "https://eveagents.appspot.com/agents/googlecalendaragent_2/",
                    "optional": null,
                    "responseStatus": null
                },
                {
                    "displayName": null,
                    "email": null,
                    "agent": "https://eveagents.appspot.com/agents/googlecalendaragent_3/",
                    "optional": null,
                    "responseStatus": null
                }
            ],
            "location": {
                "summary": null,
                "lat": null,
                "lng": null
            },
            "time": {
                "duration": 3600000,
                "durationMin": null,
                "durationMax": null,
                "periodStart": null,
                "periodEnd": null,
                "preferences": [
                    {
                        "start": null,
                        "end": null,
                        "weight": null   // positive for preferred intervals, negative for undesirable intervals
                    }
                ]
            }
        },
        "status": {
            "activityStatus": "planned", // "progress", "planned", "executed", "error"
            "attendees": [
                {
                    "displayName": null,
                    "email": null,
                    "agent": "https://eveagents.appspot.com/agents/googlecalendaragent_2/",
                    "optional": null,
                    "responseStatus": null   // "needsAction", "declined", "tentative", "accepted"
                },
                {
                    "displayName": null,
                    "email": null,
                    "agent": "https://eveagents.appspot.com/agents/googlecalendaragent_3/",
                    "optional": null,
                    "responseStatus": null
                }
            ],
            "location": {
                "summary": null,
                "lat": null,
                "lng": null
            },
            "start": "2012-07-02T14:00:00+02:00",
            "end": "2012-07-02T15:00:00+02:00",
            "updated": "2012-07-02T13:21:56.000Z"
        }
    }


### Calendar Event

For handling calendar events, an event is defined as a subset of the Google
Calendar Event. The definition of a Google Calendar Event is described at

[https://developers.google.com/google-apps/calendar/v3/reference/events#resource](https://developers.google.com/google-apps/calendar/v3/reference/events#resource)

There are slight changes. For example, there a field `agent` which can contain
the url of a GoogleCalendarAgent managing this particular event. Other calendar
agents can use this url to start negotiating with an agent for a timeslot in
a calendar.

An example event looks like:

    {
        "kind": "calendar#event",
        "etag": "\"DtwDXDyBz5ZZP0Bus85WBYkv-64/Q1BqTjhPUHlKaEVBQUFBQUFBQUFBQT09\"",
        "id": "1u924fot5dda30tn6h23hbt6tg",
        "agent": "https://eveagents.appspot.com/agents/googlecalendaragent_1",
        "status": "confirmed",
        "htmlLink": "https://www.google.com/calendar/event?eid=MXU5MjRmb3Q1ZGRhMzB0bjZoMjNoYnQ2dGcgam9zQGFsbWVuZGUub3Jn",
        "created": "2012-05-08T12:42:18.000Z",
        "updated": "2012-05-08T12:46:03.000Z",
        "summary": "Presentation on Eve",
        "description": "Explain the basics of Eve",
        "location": "Rotterdam, Almende B.V.",
        "creator": {
            "email": "jos@almende.org",
            "displayName": "Jos de Jong"
        },
        "organizer": {
            "email": "jos@almende.org",
            "displayName": "Jos de Jong"
        },
        "start": {
            "dateTime": "2012-05-11T15:00:00+02:00"
        },
        "end": {
            "dateTime": "2012-05-11T17:00:00+02:00"
        },
        "iCalUID": "1u924fot5dda30tn6h23hbt6tg@google.com",
        "sequence": 0,
        "attendees": [
            {
                "email": "ludo@almende.org",
                "responseStatus": "needsAction"
            },
            {
                "email": "andries@almende.org",
                "responseStatus": "needsAction"
            },
            {
                "email": "jos@almende.org",
                "displayName": "Jos de Jong",
                "organizer": true,
                "self": true,
                "responseStatus": "accepted"
            }
        ]
    }


### Geolocation

A geolocation is described by a latitude and longitude.

Example location (Rotterdam, the Netherlands):

    {
        "lat": 51.92298,
        "lng": 4.48287
    }


### Callback

A Callback is used for asynchronous requests.
This is useful for requests which can take a long time to complete,
possibly resulting in request timeouts.

In case of an asynchronous request,
an agent can schedule the request and return an empty response immediately.
It schedules the request and executes it later on.
The result will be send to the provided callback url and method, with the
parameters `result` and `error`.

Example callback:

    {
        "url": "http://myserver.com/agents/agent_x",
        "method": "addCallback"
    }


In the folowing example, Agent X performs an asynchronous request to agent Y.
It calls method “add” and provides two values as parameters, `a` and `b`,
and an additional `callback` parameter containing a url and method.
When agent Y receives the request,
it puts the request in its task queue and returns an empty response.

<table class="example" summary="Asynchronous request 1/2">
<tr>
<th class="example">Url</th><td class="example"><pre class="example">http://myserver.com/agents/agent_y</pre></td>
</tr>
<tr>
<th class="example">Request</th><td class="example"><pre class="example">{
    "id": 1,
    "method": "add",
    "params": {
        "a": 2.2,
        "b": 4.5,
        "callback", {
            "url": "http://myserver.com/agentX",
            "method": "addCallback"
        }
    }
}</pre></td>
</tr>
<tr>
<th class="example">Response</th><td class="example"><pre class="example">{
    "id": 1,
    "result": null
}</pre></td>
</tr>
</table>


Because agent X is not waiting for the response with the result,
there is no problem when execution of the method takes a long time.
As soon as agent Y has executed the task from the queue, it returns the result
via a new request, adressing the callback url and method of agent X:

<table class="example" summary="Asynchronous request 2/2">
<tr>
<th class="example">Url</th><td class="example"><pre class="example">http://myserver.com/agents/agent_x</pre></td>
</tr>
<tr>
<th class="example">Request</th><td class="example"><pre class="example">{
    "id": 1,
    "method": "addCallback",
    "params": {
        "result: 6.7,
        "error": null
    }
}</pre></td>
</tr>
<tr>
<th class="example">Response</th><td class="example"><pre class="example">{
    "id": 1,
    "result": null,
    "error": null
}</pre></td>
</tr>
</table>


<!-- TODO: describe authentication

## Authentication

(to be documented)


To send an authentication token with a request, an HTTP Header "Authorization"
must be provided with the HTTP POST request. The value of this header must
start with "AgentLogin", followed by a space and a valid
authentication token.
The HTTP header typically looks like this:

    Content-Type: application/json
    **Authorization: AgentLogin 93cd3a24-f429-4bf9-a7d5-ad3a2a3ad227**


Agents automatically send the correct authentication token when making a
request to another agent, when they have a valid authentication token for this
agent.
-->

## Documentation {#Documentation}

Documentation on the JSON-RPC protocol can be found via the following links:

- [http://www.json.org](http://www.json.org)
- [http://json-rpc.org](http://json-rpc.org)
- [http://jsonrpc.org](http://jsonrpc.org)
- [http://en.wikipedia.org/wiki/Json](http://en.wikipedia.org/wiki/Json)
- [http://en.wikipedia.org/wiki/JSON_RPC](http://en.wikipedia.org/wiki/JSON_RPC)

Documentation on the Google Calendar Events can be found at:

- [https://developers.google.com/google-apps/calendar/v3/reference/events#resource](https://developers.google.com/google-apps/calendar/v3/reference/events#resource)


