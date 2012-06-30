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
    scope.calendarAgents = [{}];
    scope.meetingAgents = [{}];

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
    scope.authorize = function (agent) {
        // redirect to a url to authorize this agent
        window.location.href = scope.AUTH_SERVLET +
            '?agentUrl=' + scope.CALENDAR_AGENT_URI + agent.id +
            '&agentMethod=setAuthorization' +
            '&applicationCallback=' + scope.CALLBACK_URI;
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
     * Get the email from the calendar agent
     * @param agent
     */
    scope.getCalendarAgent = function (agent) {
        if (!agent || agent.id == undefined || agent.id == '') {
            return;
        }
        var url = scope.CALENDAR_AGENT_URI + agent.id;

        // retrieve email
        var emailUpdateSeq = agent.emailUpdateSeq ? agent.emailUpdateSeq + 1 : 1;
        agent.emailUpdateSeq = emailUpdateSeq;
        agent.emailUpdating = true;
        jsonrpc({
            'url': url,
            'method': 'getEmail',
            'params': {},
            'success': function (email) {
                if (emailUpdateSeq == agent.emailUpdateSeq) {
                    delete agent.emailUpdating;
                    agent.email = email;
                    scope.$root.$eval();
                    scope.save();
                }
            },
            'error': function (err) {
                if (emailUpdateSeq == agent.emailUpdateSeq) {
                    delete agent.emailUpdating;
                    scope.$root.$eval();
                    scope.save();
                    console.log('error', err);
                }
            }
        });

        // retrieve username
        var usernameUpdateSeq = agent.usernameUpdateSeq ? agent.usernameUpdateSeq + 1 : 1;
        agent.usernameUpdateSeq = usernameUpdateSeq;
        agent.usernameUpdating = true;
        jsonrpc({
            'url': url,
            'method': 'getUsername',
            'params': {},
            'success': function (username) {
                if (usernameUpdateSeq == agent.usernameUpdateSeq) {
                    delete agent.usernameUpdating;
                    agent.username = username;
                    scope.$root.$eval();
                    scope.save();
                }
            },
            'error': function (err) {
                if (usernameUpdateSeq == agent.usernameUpdateSeq) {
                    delete agent.usernameUpdating;
                    scope.$root.$eval();
                    scope.save();
                    console.log('error', err);
                }
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
     * Update the meeting agent. The agent will synchronize
     */
    scope.updateMeetingAgent = function (agent) {
        agent.updating = true;

        // pre-process the activity
        scope.activityHtmlToJson(agent.activity);

        jsonrpc({
            'url': scope.MEETING_AGENT_URI + agent.id,
            'method': 'updateActivity',
            'params': {
                'activity': agent.activity
            },
            'success': function (activity) {
                delete agent.updating;

                // post process the activity
                scope.activityJsonToHtml(activity);

                agent.activity = activity;

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
     * Delete the meeting agent.
     */
    scope.deleteMeetingAgent = function (agent) {
        agent.updating = true;

        // pre-process the activity
        scope.activityHtmlToJson(agent.activity);

        jsonrpc({
            'url': scope.MEETING_AGENT_URI + agent.id,
            'method': 'clear',
            'success': function () {
                delete agent.updating;
                scope.removeMeetingAgent(agent);
                scope.$root.$eval();
            },
            'error': function (err) {
                delete agent.updating;
                scope.removeMeetingAgent(agent);
                scope.$root.$eval();
                console.log('error', err);
            }
        });
    };

    /**
     * Retrieve the current planned activity of the meeting agent
     * @param {Object} agent
     */
    scope.getMeetingAgent = function (agent) {
        // keep track on the updateSeq, to prevent simultaneous update requests
        // coming back in the wrong order.
        var updateSeq = agent.updateSeq ? agent.updateSeq + 1 : 1;
        agent.updateSeq = updateSeq;
        agent.updating = true;
        jsonrpc({
            'url': scope.MEETING_AGENT_URI + agent.id,
            'method': 'getActivity',
            'params': {},
            'success': function (activity) {
                if (updateSeq == agent.updateSeq) {
                    delete agent.updating;

                    // post process the activity
                    scope.activityJsonToHtml(activity);
                    agent.activity = activity;

                    scope.$root.$eval();
                    scope.save();
                }
            },
            'error': function (err) {
                if (updateSeq == agent.updateSeq) {
                    delete agent.updating;
                    scope.$root.$eval();
                    scope.save();
                    console.log('error', err);
                }
            }
        });
    };

    /**
     * update all attendee urls from the id's
     * @param {Object} activity
     */
    scope.activityHtmlToJson = function (activity) {
        if (!activity.constraints) {
            activity.constraints = {};
        }

        // update urls from ids
        if (activity.constraints && activity.constraints.attendees) {
            var attendees = activity.constraints.attendees;
            $.each(attendees, function (index, attendee) {
                if (attendee.id) {
                    attendee.agent = scope.CALENDAR_AGENT_URI + attendee.id;
                }
            });
        }

        // update duration
        if (activity.constraints && activity.constraints.time &&
                activity.constraints.time.durationMinutes != undefined) {
            activity.constraints.time.duration =
                activity.constraints.time.durationMinutes * 60 * 1000;
        }
    };

    /**
     * update all attendee ids from the urls
     * @param {Object} activity
     */
    scope.activityJsonToHtml = function (activity) {
        if (!activity) {
            return;
        }

        // update agent ids from urls
        if (activity.constraints && activity.constraints.attendees) {
            var attendees = activity.constraints.attendees;
            $.each(attendees, function (index, attendee) {
                if (attendee.agent) {
                    var start = scope.CALENDAR_AGENT_URI.length;
                    attendee.id = attendee.agent.substring(start, attendee.agent.length);
                }
            });
        }

        // update duration
        if (activity.constraints && activity.constraints.time &&
                activity.constraints.time.duration != undefined) {
            if (activity.constraints.time.duration > 0) {
                activity.constraints.time.durationMinutes =
                    activity.constraints.time.duration / 60 / 1000;
            }
            else {
                activity.constraints.time.durationMinutes = 0;
            }
        }
    };

    /**
     * Format a string containing a datetime
     * @param {String} dateTime
     * @return {Date}
     */
    scope.formatDateTime = function (dateTime) {
        var d = new Date(dateTime);
        return d.toLocaleDateString() + ', ' + d.toLocaleTimeString();
    };

    /**
     * Add an attendee to an agent
     * @param agent
     */
    scope.addAttendee = function (agent) {
        if (!agent.activity.constraints.attendees) {
            agent.activity.constraints.attendees = [];
        }
        agent.activity.constraints.attendees.push({});
        scope.save();
    };

    /**
     * Remove an attendee from an agent
     * @param agent
     * @param attendee
     */
    scope.removeAttendee = function (agent, attendee) {
        if (!agent.activity.constraints.attendees) {
            return;
        }
        var index = agent.activity.constraints.attendees.indexOf(attendee);
        if (index != -1) {
            agent.activity.constraints.attendees.splice(index, 1);
        }
        scope.setUpdated(agent);
        scope.save();
    };

    /**
     * Set a meeting agent as updated
     * @param {Object} agent
     */
    scope.setUpdated = function (agent) {
        if (!agent.activity) {
            agent.activity = {};
        }
        if (!agent.activity.status) {
            agent.activity.status = {};
        }
        agent.activity.status.updated = new Date();
        scope.save();
    };

    /**
     * retrieve the emails from all listed calendar agents
     */
    scope.updateAll = function () {
        $.each(scope.calendarAgents, function(index, agent) {
            scope.getCalendarAgent(agent);
        });

        $.each(scope.meetingAgents, function(index, agent) {
            scope.getMeetingAgent(agent);
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
