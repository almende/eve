

angular.module('controller', ['ngResource']);


/**
 * Angular JS controller to control the page
 * @constructor Controller
 */
function Controller($scope, $resource) {
    // constants
    $scope.ORIGIN = window.location.protocol + '//' + window.location.host + '/';
    $scope.AUTH_SERVLET = $scope.ORIGIN + 'auth/google';
    $scope.AGENT_SERVLET = $scope.ORIGIN + 'agents/';
    $scope.MANAGEMENT_AGENT = $scope.AGENT_SERVLET + 'management/';
    $scope.CALLBACK_URI = window.location.href;
    $scope.CALENDARAGENT_TYPE = 'com.almende.eve.agent.google.GoogleCalendarAgent';
    $scope.MEETINGAGENT_TYPE = 'com.almende.eve.agent.MeetingAgent';

    // lists with agents
    $scope.calendarAgents = [];
    $scope.meetingAgents = [];

    $scope.newCalendarAgentId = '';
    $scope.newMeetingAgentId = '';

    /**
     * Authorize a calendar agent with the users Google Calendar
     * @param {Object} agent
     */
    $scope.authorize = function (agent) {
        // redirect to a url to authorize this agent
        window.location.href = $scope.AUTH_SERVLET +
            '?agentUrl=' + $scope.AGENT_SERVLET + agent.id +
            '&agentMethod=setAuthorization' +
            '&applicationCallback=' + $scope.CALLBACK_URI;
    };

    /**
     * Create a new calendar agent
     */
    $scope.createCalendarAgent = function () {
        var id = $scope.newCalendarAgentId;
        if (!id) {
            alert('Error: no id for the new CalendarAgent filled in');
            return;
        }
        $scope.newCalendarAgentId = '';

        jsonrpc({
            'url': $scope.MANAGEMENT_AGENT,
            'method': 'create',
            'params': {
                'id': id,
                'type': $scope.CALENDARAGENT_TYPE
            },
            'success': function () {
                $scope.getCalendarAgents();
                $scope.$apply();
            }
        });
    };

    /**
     * Delete a calendar agent and remove it from the list.
     * @param {Object} agent
     */
    $scope.deleteCalendarAgent = function (agent) {
        if (!confirm('Do you really want to delete CalendarAgent "' + agent.id + '"?')) {
            return;
        }

        jsonrpc({
            'url': $scope.MANAGEMENT_AGENT,
            'method': 'delete',
            'params': {
                'id': agent.id
            },
            'success': function () {
                $scope.getCalendarAgents();
                $scope.$apply();
            }
        });
    };

    /**
     * Get all calendar agents
     */
    $scope.getCalendarAgents = function () {
        jsonrpc({
            'url': $scope.MANAGEMENT_AGENT,
            'method': 'list',
            'params': {
                'type': $scope.CALENDARAGENT_TYPE
            },
            'success': function (resp) {
                $scope.calendarAgents = resp;
                $scope.calendarAgents.forEach(function (agent) {
                    $scope.getCalendarAgent(agent);
                });
                $scope.$apply();
            }
        });
    };

    /**
     * Get the email and username from the calendar agent
     * @param agent
     */
    $scope.getCalendarAgent = function (agent) {
        if (!agent || agent.id == undefined || agent.id == '') {
            return;
        }
        var url = $scope.AGENT_SERVLET + agent.id;

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
                    $scope.$apply();
                    $scope.save();
                }
            },
            'error': function (err) {
                if (emailUpdateSeq == agent.emailUpdateSeq) {
                    delete agent.emailUpdating;
                    $scope.$apply();
                    $scope.save();
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
                    $scope.$apply();
                    $scope.save();
                }
            },
            'error': function (err) {
                if (usernameUpdateSeq == agent.usernameUpdateSeq) {
                    delete agent.usernameUpdating;
                    $scope.$apply();
                    $scope.save();
                    console.log('error', err);
                }
            }
        });
    };

    /**
     * Create a new meeting agent
     */
    $scope.createMeetingAgent = function () {
        var id = $scope.newMeetingAgentId;
        if (!id) {
            alert('Error: no id for the new MeetingAgent filled in');
            return;
        }
        $scope.newMeetingAgentId = '';

        jsonrpc({
            'url': $scope.MANAGEMENT_AGENT,
            'method': 'create',
            'params': {
                'id': id,
                'type': $scope.MEETINGAGENT_TYPE
            },
            'success': function () {
                $scope.getMeetingAgents();
                $scope.$apply();
            }
        });
    };

    /**
     * Update the meeting agent. The agent will synchronize
     */
    $scope.updateMeetingAgent = function (agent) {
        agent.updating = true;

        // pre-process the activity
        $scope.activityHtmlToJson(agent.activity);

        jsonrpc({
            'url': $scope.AGENT_SERVLET + agent.id,
            'method': 'updateActivity',
            'params': {
                'activity': agent.activity
            },
            'success': function (activity) {
                delete agent.updating;

                // post process the activity
                $scope.activityJsonToHtml(activity);

                agent.activity = activity;

                $scope.$apply();
                $scope.save();
            },
            'error': function (err) {
                delete agent.updating;
                $scope.$apply();
                $scope.save();
                console.log('error', err);
            }
        });
    };

    /**
     * Delete the meeting agent.
     */
    $scope.deleteMeetingAgent = function (agent) {
        if (!confirm('Do you really want to delete MeetingAgent "' + agent.id + '"?')) {
            return;
        }

        agent.updating = true;
        jsonrpc({
            'url': $scope.MANAGEMENT_AGENT,
            'method': 'delete',
            'params': {
                'id': agent.id
            },
            'success': function () {
                $scope.getMeetingAgents();
                $scope.$apply();
            }
        });
    };

    /**
     * Get all calendar agents
     */
    $scope.getMeetingAgents = function () {
        jsonrpc({
            'url': $scope.MANAGEMENT_AGENT,
            'method': 'list',
            'params': {
                'type': $scope.MEETINGAGENT_TYPE
            },
            'success': function (resp) {
                $scope.meetingAgents = resp;
                $scope.meetingAgents.forEach(function (agent) {
                    $scope.getMeetingAgent(agent);
                });
                $scope.$apply();
            }
        });
    };

    /**
     * Retrieve the current planned activity of the meeting agent
     * @param {Object} agent
     */
    $scope.getMeetingAgent = function (agent) {
        // keep track on the updateSeq, to prevent simultaneous update requests
        // coming back in the wrong order.
        var updateSeq = agent.updateSeq ? agent.updateSeq + 1 : 1;
        agent.updateSeq = updateSeq;
        agent.updating = true;
        jsonrpc({
            'url': $scope.AGENT_SERVLET + agent.id,
            'method': 'getActivity',
            'params': {},
            'success': function (activity) {
                if (updateSeq == agent.updateSeq) {
                    delete agent.updating;

                    // post process the activity
                    $scope.activityJsonToHtml(activity);
                    agent.activity = activity;

                    $scope.$apply();
                    $scope.save();
                }
            },
            'error': function (err) {
                if (updateSeq == agent.updateSeq) {
                    delete agent.updating;
                    $scope.$apply();
                    $scope.save();
                    console.log('error', err);
                }
            }
        });
    };

    /**
     * update all attendee urls from the id's
     * @param {Object} activity
     */
    $scope.activityHtmlToJson = function (activity) {
        if (!activity.constraints) {
            activity.constraints = {};
        }

        // update urls from ids
        if (activity.constraints && activity.constraints.attendees) {
            var attendees = activity.constraints.attendees;
            $.each(attendees, function (index, attendee) {
                if (attendee.id) {
                    attendee.agent = $scope.AGENT_SERVLET + attendee.id + '/';
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
    $scope.activityJsonToHtml = function (activity) {
        if (!activity) {
            return;
        }

        // update agent ids from urls
        if (activity.constraints && activity.constraints.attendees) {
            var attendees = activity.constraints.attendees;
            $.each(attendees, function (index, attendee) {
                if (attendee.agent) {
                    var start = $scope.AGENT_SERVLET.length;
                    attendee.id = attendee.agent.substring(start, attendee.agent.length);
                    if (attendee.id[attendee.id.length-1] == '/') {
                        attendee.id = attendee.id.substring(0, attendee.id.length - 1);
                    }
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
     * @return {String} formattedDateTime
     */
    $scope.formatDateTime = function (dateTime) {
        if (dateTime) {
            var d = new Date(dateTime);
            return d.toISOString();
        }
        else {
            return '';
        }
    };

    /**
     * Build an agents url from its id
     * @param {Object} agent
     * @return {String} url
     */
    $scope.getUrl = function(agent) {
        return $scope.AGENT_SERVLET + agent.id + '/';
    };

    /**
     * Built the analysis url for a meeting agent
     * @param {Object} agent
     * @return {String}
     */
    $scope.getAnalysisUrl = function (agent) {
        return 'meetingagent.html?url=' + encodeURIComponent($scope.getUrl(agent));
    };

    /**
     * Add an attendee to an agent
     * @param agent
     */
    $scope.addAttendee = function (agent) {
        if (!agent.activity) {
            agent.activity ={};
        }
        if (!agent.activity.constraints) {
            agent.activity.constraints ={};
        }
        if (!agent.activity.constraints.attendees) {
            agent.activity.constraints.attendees = [];
        }
        agent.activity.constraints.attendees.push({});
        $scope.save();
    };

    /**
     * Remove an attendee from an agent
     * @param agent
     * @param attendee
     */
    $scope.removeAttendee = function (agent, attendee) {
        if (!agent.activity || ! agent.activity.constraints ||
                !agent.activity.constraints.attendees) {
            return;
        }
        var index = agent.activity.constraints.attendees.indexOf(attendee);
        if (index != -1) {
            agent.activity.constraints.attendees.splice(index, 1);
        }
        $scope.setUpdated(agent);
        $scope.save();
    };

    /**
     * Change the parameter optional of an attendee
     * @param agent
     * @param attendee
     */
    $scope.toggleAttendeeOptional = function (agent, attendee) {
        attendee.optional = attendee.optional ? false : true;
        $scope.setUpdated(agent);
        $scope.save();
    };

    /**
     * Set a meeting agent as updated
     * @param {Object} agent
     */
    $scope.setUpdated = function (agent) {
        if (!agent.activity) {
            agent.activity = {};
        }
        if (!agent.activity.status) {
            agent.activity.status = {};
        }
        agent.activity.status.updated = new Date();
        $scope.save();
    };

    /**
     * retrieve the emails from all listed calendar agents, and the status
     * of all meetingAgents
     */
    $scope.refreshAll = function () {
        $.each($scope.calendarAgents, function(index, agent) {
            $scope.getCalendarAgent(agent);
        });

        $.each($scope.meetingAgents, function(index, agent) {
            $scope.getMeetingAgent(agent);
        });
    };

    /**
     * Load agents from local storage
     */
    $scope.load = function() {
        $scope.getCalendarAgents();
        $scope.getMeetingAgents();
    };

    /**
     * Save agents to local storage
     */
    $scope.save = function () {
        localStorage['calendarAgents'] = JSON.stringify($scope.calendarAgents);
        localStorage['meetingAgents'] = JSON.stringify($scope.meetingAgents);
    };

    $scope.load();
    $scope.refreshAll();
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
