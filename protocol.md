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

- [Protocol](#protocol) describes the communication protocol for the agents.
- [Resources](#resources) describes standardized resources such as calendar
  events and geolocations.
- [Documentation](#documentation) links to resources related to the communication
  protocol and defined resources.


## Protocol {#protocol}

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
<th class="example">Url</th><td class="example"><pre class="example">http://myserver.com/agent/Y</pre></td>
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


## Resources {#resources}

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

An example of an activity:

    {
        "summary": "Meeting on project X",
        "agent": "https://eveagents.appspot.com/agents/meetingagent/ac87c9b4c94f3d1da6d64bfc9d03ee7f/",
        "constraints": {
            "attendees": [
                {
                    "displayName": null,
                    "email": null,
                    "agent": "https://eveagents.appspot.com/agents/googlecalendaragent/ac87c9b4c94f3d1da6d64bfc9d03ee7f/",
                    "optional": null,
                    "responseStatus": null
                },
                {
                    "displayName": null,
                    "email": null,
                    "agent": "https://eveagents.appspot.com/agents/googlecalendaragent/6c646e0d79b48eac8ef766388a7afc93/",
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
                "periodEnd": null
            }
        },
        "status": {
            "description": null,
            "attendees": [],
            "location": null,
            "start": "2012-07-02T14:00:00+02:00",
            "end": "2012-07-02T15:00:00+02:00",
            "error": null,
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
        "agent": "https://eveagents.appspot.com/agents/googlecalendaragent/025ddd36e86fedacc612fe570f369950",
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
It schedules the request and executes it lateron.
The result will be send to the provided callback url and method, with the
parameters `result` and `error`.

Example callback:

    {
        "url": "http://myserver.com/agentX",
        "method": "addCallback"
    }


In the folowing example, Agent X performs an asynchronous request to agent Y.
It calls method “add” and provides two values as parameters, `a` and `b`,
and an additional `callback` parameter containing a url and method.
When agent Y receives the request,
it puts the request in its task queue and returns an empty response.

<table class="example" summary="Asynchronous request 1/2">
<tr>
<th class="example">Url</th><td class="example"><pre class="example">http://myserver.com/agent/Y</pre></td>
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
<th class="example">Url</th><td class="example"><pre class="example">http://myserver.com/agent/X</pre></td>
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

## Documentation

Documentation on the JSON-RPC protocol can be found via the following links:

- [http://www.json.org](http://www.json.org)
- [http://json-rpc.org](http://json-rpc.org)
- [http://jsonrpc.org](http://jsonrpc.org)
- [http://en.wikipedia.org/wiki/Json](http://en.wikipedia.org/wiki/Json)
- [http://en.wikipedia.org/wiki/JSON_RPC](http://en.wikipedia.org/wiki/JSON_RPC)

Documentation on the Google Calendar Events can be found at:

- [https://developers.google.com/google-apps/calendar/v3/reference/events#resource](https://developers.google.com/google-apps/calendar/v3/reference/events#resource)


