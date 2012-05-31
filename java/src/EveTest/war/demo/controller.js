/**
 * Angular JS controller to control the page
 * @constructor Ctrl
 */
function Ctrl() {
    var scope = this;

    // constants
    scope.AUTH_SERVLET = 'http://eveagents.appspot.com/auth/google';
    scope.AGENTS_URI = 'http://eveagents.appspot.com/agents/';
    scope.CALENDAR_AGENT_URI = scope.AGENTS_URI + 'googlecalendaragent/';
    scope.MEETING_AGENT_URI = scope.AGENTS_URI + 'meetingagent/';
    scope.CALLBACK_URI = window.location.href;

    // lists with agents
    scope.calendarAgents = [];
    scope.meetingAgents = [];

    /**
     * Add a calendar agent
     */
    scope.addCalendarAgent = function () {
        scope.calendarAgents.push({});
        scope.save();
    };

    /**
     * Authorize a calendar agent with the users Google Calendar
     * @param {Object} agent
     */
    scope.authorizeCalendarAgent = function (agent) {
        // redirect to a url to authorize this agent
        window.location.href = scope.AUTH_SERVLET +
            '?agent=' + scope.CALENDAR_AGENT_URI + agent.id +
            '&callback=' + scope.CALLBACK_URI;
    };

    /**
     * remove a calendar agent from the list. The agent itself will not be deleted
     * @param {Object} agent
     */
    scope.removeCalendarAgent = function (agent) {
        var index = scope.calendarAgents.indexOf(agent);
        if (index != -1) {
            scope.calendarAgents.splice(index, 1);
        }
        scope.save();
    };

    /**
     * Delete a calendar agent and remove it from the list.
     * @param {Object} agent
     */
    scope.deleteCalendarAgent = function (agent) {
        jsonrpc({
            'url': scope.CALENDAR_AGENT_URI + agent.id,
            'method': 'clear',
            'params': {},
            'success': function (resp) {
                scope.removeCalendarAgent(agent);
                scope.$root.$eval();

            }
        });
    };

    /**
     * Update the email from the agent
     * @param agent
     */
    scope.updateCalendarAgent = function (agent) {
        if (!agent || agent.id == undefined || agent.id == '') {
            return;
        }
        var url = scope.CALENDAR_AGENT_URI + agent.id;

        // retrieve email
        agent.emailUpdating = true;
        jsonrpc({
            'url': url,
            'method': 'getEmail',
            'params': {},
            'success': function (email) {
                delete agent.emailUpdating;
                agent.email = email;
                scope.$root.$eval();
                scope.save();
            },
            'error': function (err) {
                delete agent.emailUpdating;
                scope.$root.$eval();
                scope.save();
                console.log('error', err);
            }
        });

        // retrieve username
        agent.usernameUpdating = true;
        jsonrpc({
            'url': url,
            'method': 'getUsername',
            'params': {},
            'success': function (username) {
                delete agent.usernameUpdating;
                agent.username = username;
                scope.$root.$eval();
                scope.save();
            },
            'error': function (err) {
                delete agent.usernameUpdating;
                scope.$root.$eval();
                scope.save();
                console.log('error', err);
            }
        });
    };

    /**
     * Add a meeting agent
     */
    scope.addMeetingAgent = function () {
        scope.meetingAgents.push({});
        scope.save();
    };

    /**
     * Remove a meeting agent
     * @param {Object} agent
     */
    scope.removeMeetingAgent = function (agent) {
        var index = scope.meetingAgents.indexOf(agent);
        if (index != -1) {
            scope.meetingAgents.splice(index, 1);
        }
        scope.save();
    };

    /**
     * Save a meeting agent. Send the event settings to the meeting agent
     */
    scope.setActivityMeetingAgent = function (agent) {
        scope.save();

        // create array with attendee urls
        var attendees = [];
        if (agent.attendees) {
            $.each(agent.attendees, function (index, attendee) {
               attendees.push(scope.CALENDAR_AGENT_URI + attendee.id);
            });
        }

        // create event parameters
        var params = {
            'summary': agent.summary || '',
            'location': agent.location || '',
            'duration': Number(agent.duration), // minutes
            'attendees': attendees
        };

        agent.saving = true;
        jsonrpc({
            'url': scope.MEETING_AGENT_URI + agent.id,
            'method': 'setActivity',
            'params': params,
            'success': function (email) {
                delete agent.saving;
                agent.email = email;
                scope.$root.$eval();
                scope.save();
            },
            'error': function (err) {
                delete agent.saving;
                scope.$root.$eval();
                scope.save();
                console.log('error', err);
            }
        });
    };

    /**
     * Update the meeting agent. The agent will synchronize
     */
    scope.updateMeetingAgent = function (agent) {
        agent.updating = true;
        jsonrpc({
            'url': scope.MEETING_AGENT_URI + agent.id,
            'method': 'update',
            'params': {},
            'success': function (email) {
                delete agent.updating;
                agent.email = email;

                scope.getActivityMeetingAgent(agent);

                scope.$root.$eval();
                scope.save();
            },
            'error': function (err) {
                delete agent.updating;
                scope.$root.$eval();
                scope.save();
                console.log('error', err);
            }
        });
    };

    /**
     * Retrieve the current planned activity of the meeting agent
     * @param {Object} agent
     */
    scope.getActivityMeetingAgent = function (agent) {
        agent.updating = true;
        jsonrpc({
            'url': scope.MEETING_AGENT_URI + agent.id,
            'method': 'getActivity',
            'params': {},
            'success': function (activity) {
                delete agent.updating;
                agent.summary = activity.summary;
                if (activity.constraints && activity.constraints.locations) {
                    agent.location = activity.constraints.locations[0];
                }
                else {
                    agent.location = '';
                }
                if (activity.status && activity.status.start && activity.status.end) {
                    var start = new Date(activity.status.start);
                    var end = new Date(activity.status.end);
                    agent.start = start.toLocaleDateString() + ' ' + start.toLocaleTimeString();
                    agent.end = end.toLocaleDateString() + ' ' + end.toLocaleTimeString();
                    agent.duration = (end.valueOf() - start.valueOf()) / 1000 / 60; // minutes
                }
                else {
                    agent.start = undefined;
                    agent.end = undefined;
                    // leave agent.duration untouched
                }

                scope.$root.$eval();
                scope.save();
            },
            'error': function (err) {
                delete agent.updating;
                scope.$root.$eval();
                scope.save();
                console.log('error', err);
            }
        });
    };

    /**
     * Add an attendee to an agent
     * @param agent
     */
    scope.addAttendee = function (agent) {
        if (!agent.attendees) {
            agent.attendees = [];
        }
        agent.attendees.push({});
        scope.save();
    };

    /**
     * Remove an attendee from an agent
     * @param agent
     * @param attendee
     */
    scope.removeAttendee = function (agent, attendee) {
        if (!agent.attendees) {
            return;
        }
        var index = agent.attendees.indexOf(attendee);
        if (index != -1) {
            agent.attendees.splice(index, 1);
        }
        scope.save();
    };

    /**
     * retrieve the emails from all listed calendar agents
     */
    scope.updateAll = function () {
        $.each(scope.calendarAgents, function(index, agent) {
            scope.updateCalendarAgent(agent);
        });
    };

    /**
     * Load agents from local storage
     */
    scope.load = function() {
        var calendarAgents = localStorage['calendarAgents'];
        if (calendarAgents) {
            scope.calendarAgents = JSON.parse(calendarAgents);
        }
        var meetingAgents = localStorage['meetingAgents'];
        if (meetingAgents) {
            scope.meetingAgents = JSON.parse(meetingAgents);
        }
    };

    /**
     * Save agents to local storage
     */
    scope.save = function () {
        localStorage['calendarAgents'] = JSON.stringify(scope.calendarAgents);
        localStorage['meetingAgents'] = JSON.stringify(scope.meetingAgents);
    };

    scope.load();
    scope.updateAll();
}

/**
 * Send a JSON-RPC 2.0 call to an agent
 * @param {Object} params  Object containing fields:<br>
 *                         - {String} url,<br>
 *                         - {String} method,<br>
 *                         - {Object} [params],<br>
 *                         - {function} [success],<br>
 *                         - {function} [error]<br>
 */
function jsonrpc(params) {
    if (!params.url) {
        throw Error('url missing');
    }
    if (!params.method) {
        throw Error('method missing');
    }
    var req = {
        'id': 1,
        'method': params.method,
        'params': params.params || {}
    };
    var success = params.success || function (result) {
        console.log('result', result);
    };
    var error = params.error || function (err) {
        console.log('error', err);
    };

    $.ajax({
        'type': 'POST',
        'url': params.url,
        'contentType': 'application/json',
        'data': JSON.stringify(req),
        'success': function (resp) {
            if (resp.error) {
                error(resp.error);
            }
            else {
                success(resp.result);
            }
        },
        'error': function (err) {
            error(err);
        }
    });
}