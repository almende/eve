/**
 * Angular JS controller to control the page
 * @constructor Ctrl
 */
function Planning($scope) {
    // constants
    var ORIGIN = window.location.protocol + '//' + window.location.host + '/';
    var AUTH_SERVLET = ORIGIN + 'auth/google';
    var AGENT_SERVLET = ORIGIN + 'agents/';
    //var PERSONAL_AGENT_URI = AGENT_SERVLET + 'googlecalendaragent/'; // TODO: cleanup
    //var MEETING_AGENT_URI = AGENT_SERVLET + 'meetingagent/'; // TODO: cleanup
    var DIRECTORY_AGENT_URI = AGENT_SERVLET + 'directoryagent/';
    var CALLBACK_URI = window.location.href;
    var MEETINGAGENT_TYPE = 'com.almende.eve.agent.MeetingAgent';

    var SECOND = 1000;
    var MINUTE = 60 * SECOND;
    var HOUR = 60 * MINUTE;
    var INTERVAL_MIN = 10 * SECOND;
    var INTERVAL_MAX = HOUR;

    $scope.$watch('username', function() {
        $scope.save();
    });

    /**
     * Get the email from the calendar agent
     */
    $scope.getEmail = function () {
        if (!$scope.personalAgent) {
            $scope.authorized = false;
            return;
        }

        $scope.authorizing = true;
        jsonrpc({
            'url': $scope.personalAgent,
            'method': 'getEmail',
            'params': {},
            'success': function (email) {
                delete $scope.authorizing;
                $scope.authorized = (email != null);
                $scope.email = email;
                $scope.$apply();
                console.log('email', email);

                $scope.registerAgent({
                    'agent': $scope.personalAgent,
                    'username': $scope.username,
                    'email': $scope.email
                });
            },
            'error': function (err) {
                $scope.authorized = false;
                delete $scope.authorizing;
                delete $scope.email;
                $scope.$apply();
                console.log('error', err);
            }
        });
    };

    $scope.getEventClass = function (event) {
        return (event && event.agent) ? 'event event-editable' : 'event';
    };

    $scope.getEventStart = function (event) {
        if (event && event.start) {
            if (event.start.dateTime) {
                return new Date(event.start.dateTime).valueOf();
            }
            if (event.start.date) {
                return new Date(event.start.date).valueOf();
            }
        }
        return 0;
    };

    $scope.getEventStartDate = function (event) {
        if (event && event.start) {
            if (event.start.dateTime) {
                return new Date(event.start.dateTime);
            }
            if (event.start.date) {
                return new Date(event.start.date);
            }
        }
        return new Date(0);
    };

    $scope.getEventFormattedDate = function (event) {
        var str = '';

        if (event && event.start) {
            var start = new Date(event.start.dateTime || event.start.date);
            str += start.format('dddd mmm d, HH:MM');
        }
        str += ' - ';
        if (event && event.end) {
            var end = new Date(event.end.dateTime || event.end.date);
            str += end.format('HH:MM');
        }

        return str;
    };

    // create a watch to split the events in three groups: Today, Tomorrow, Upcoming
    $scope.days = {};
    $scope.events = [];
    $scope.$watch('events', function() {
        var dateToday = new Date();
        var dateTomorrow = new Date();
        dateTomorrow.setDate(dateTomorrow.getDate() + 1);
        var today = dateToday.format('yyyy-mm-dd');
        var tomorrow = dateTomorrow.format('yyyy-mm-dd');

        var eventsToday = [];
        var eventsTomorrow = [];
        var eventsUpcoming = [];
        var count = $scope.events ? $scope.events.length : 0;
        for (var i = 0; i < count; i++) {
            var event = $scope.events[i];
            var startDate = new Date(event.start.dateTime || event.start.date);
            var endDate = new Date(event.end.dateTime || event.end.date);
            var start = startDate.format('yyyy-mm-dd');
            var formattedDate = '';
            if (start == today) {
                eventsToday.push(event);
            }
            else if (start == tomorrow) {
                eventsTomorrow.push(event);
            }
            else {
                eventsUpcoming.push(event);
                formattedDate += startDate.format('dddd mmm d, ');
            }

            formattedDate += startDate.format('HH:MM');
            formattedDate += ' - ';
            formattedDate += endDate.format('HH:MM');

            event.formattedDate = formattedDate;
        }

        $scope.days = {};
        $scope.days['Today - ' + dateToday.format('dddd mmm d') + ''] = eventsToday;
        $scope.days['Tomorrow - ' + dateTomorrow.format('dddd mmm d') + ''] = eventsTomorrow;
        $scope.days['Upcoming'] = eventsUpcoming;
    });

    /**
     * Get the email from the calendar agent
     */
    $scope.getEvents = function (interval) {
        if (!$scope.personalAgent) {
            return;
        }

        $scope.startInterval(interval);

        // determine time interval
        var now = new Date();
        var nowPlus7 = new Date();
        nowPlus7.setDate(nowPlus7.getDate() + 7);

        $scope.updating = true;
        jsonrpc({
            'url': $scope.personalAgent,
            'method': 'getEvents',
            'params': {
                'timeMin': now.format('yyyy-mm-dd'),
                'timeMax': nowPlus7.format('yyyy-mm-dd')
            },
            'success': function (events) {
                $scope.events = events;
                $scope.updating = false;
                $scope.$apply();
                console.log('events', events);
            },
            'error': function (err) {
                $scope.updating = false;
                $scope.$apply();
                console.log('error', err);
            }
        });
    };

    // TODO: reset the interval on mouse move (= user activity) ?
    $scope.timer = undefined;
    $scope.interval = undefined;
    $scope.startInterval = function (interval) {
        // interval is dynamic between 10 sec and 1 hour
        if (interval == undefined) {
            $scope.interval = INTERVAL_MIN; // ms
        }
        else {
            $scope.interval *= 2;
        }
        if ($scope.interval < INTERVAL_MIN) {
            $scope.interval = INTERVAL_MIN;
        }
        if ($scope.interval > INTERVAL_MAX) {
            $scope.interval = INTERVAL_MAX;
        }
        console.log('interval', $scope.interval);

        if ($scope.timer) {
            clearTimeout($scope.timer);
            delete $scope.timer;
        }
        $scope.timer = setTimeout(function () {
            $scope.getEvents($scope.interval);
            $scope.getRegistrations();
        }, $scope.interval);
    };

    // as soon as the window gets focus, retrieve the events again
    $(window).bind('focus', function () {
        $scope.getEvents();
    });

    // when there is mouse activity over the window, ensure the events
    // are refreshed with the shortest allowed interval
    $(window).bind('mousemove', function () {
        if ($scope.interval > INTERVAL_MIN) {
            $scope.startInterval();
        }
    });

    /**
     * Authorize the current personalAgent
     */
    $scope.authorize = function () {
        // redirect to a url to authorize this agent
        window.location.href = AUTH_SERVLET +
            '?agentUrl=' + $scope.personalAgent +
            '&agentMethod=setAuthorization' +
            '&applicationCallback=' + CALLBACK_URI;
    };

    $scope.addAttendee = function (activity, attendee) {
        if (!activity.constraints) {
            activity.constraints = {};
        }
        if (!activity.constraints.attendees) {
            activity.constraints.attendees = [];
        }
        activity.constraints.attendees.push(attendee || {});

        $scope.renderSelectBoxes();
    };

    $scope.removeAttendee = function (activity, attendee) {
        if (!activity.constraints || !activity.constraints.attendees) {
            return;
        }
        var index = activity.constraints.attendees.indexOf(attendee);
        if (index != -1) {
            activity.constraints.attendees.splice(index, 1);
        }
    };

    $scope.createEvent = function () {
        var uuid = UUID.randomUUID();
        var agent = AGENT_SERVLET + uuid + '/?type=' + MEETINGAGENT_TYPE;

        $.ajax({
            'type': 'PUT',
            'url': agent,
            'success': function () {
                var activity = {
                    'agent': agent,
                    'summary': 'new event',
                    'constraints': {
                        'time': {
                            'duration': 60 * 60 * 1000
                        },
                        'attendees': [
                            {
                                'agent': $scope.personalAgent,
                                'username': $scope.username,
                                'email': $scope.email
                            }
                        ]
                    }
                };

                $scope.editor.edit(activity);
                $scope.$apply();
            }
        });
    };

    $scope.deleteEvent = function (agent) {
        // TODO: directly remove the event from the list? Or disable it?
        $.ajax({
            'type': 'DELETE',
            'url': agent,
            'success': function () {
                $scope.getEvents();
            }
        });
    };

    $scope.editor = {};

    $scope.editor.edit = function (activity) {
        $scope.editor.activity = activity;
        $scope.editor.show = true;
        $scope.editor.loading = false;
        $scope.editor.saving = false;

    };

    $scope.editor.load = function (agent) {
        if (!agent) {
            return;
        }

        $scope.editor.activity = {};
        $scope.editor.loading = true;
        $scope.editor.show = true;
        jsonrpc({
            'url': agent,
            'method': 'getActivity',
            'params': {},
            'success': function (activity) {
                $scope.editor.loading = false;
                $scope.editor.activity = activity;
                $scope.$apply();
                $scope.renderSelectBoxes();
            }
        });
    };

    $scope.editor.cancel = function () {
        $scope.editor.activity = {};
        $scope.editor.show = false;
        $scope.editor.loading = false;
        $scope.editor.saving = false;
    };

    $scope.editor.update = function () {
        $scope.editor.saving = true;
        var activity = $scope.editor.activity;
        if (!activity.status) {
            activity.status = {};
        }
        activity.status.updated = (new Date()).toISOString();
        jsonrpc({
            'url': activity.agent,
            'method': 'updateActivity',
            'params': {
                'activity': activity
            },
            'success': function () {
                $scope.editor.activity = undefined;
                $scope.editor.saving = false;
                $scope.editor.show = false;
                $scope.$apply();
                $scope.getEvents();
            },
            'error': function (err) {
                console.log('error', err);
                $scope.editor.saving = false;
                $scope.$apply();
            }
        });
    };

    $scope.registerAgent = function(params) {
        if (params.agent && params.email) {
            // register the calendar agent when all fields are populated
            jsonrpc({
                'url': DIRECTORY_AGENT_URI,
                'method': 'register',
                'params': {
                    'agent': params.agent,
                    'type': 'personalagent',
                    'username': params.username,
                    'email': params.email
                },
                'success': function () {
                    $scope.getRegistrations();
                }
            });
        }
    };

    $scope.unregisterAgent = function() {
        jsonrpc({
            'url': DIRECTORY_AGENT_URI,
            'method': 'unregister',
            'params': {
                'agent': $scope.personalAgent,
                'username': $scope.username,
                'email': $scope.email
            },
            'success': function () {
                $scope.getRegistrations();
            }
        });
    };

    $scope.getRegistrations = function() {
        jsonrpc({
            'url': DIRECTORY_AGENT_URI,
            'method': 'find',
            'params': {},
            'success' : function (registrations) {
                console.log('registrations', registrations);
                $scope.registrations = registrations;
                $scope.$apply();
            }
        });
    };

    $scope.login = function () {
        // TODO: create agent
        $scope.authorized = false;
        $scope.usernameChanged = false;

        if ($scope.username && $scope.username.length > 0) {
            $scope.personalAgent = AGENT_SERVLET + $scope.username + '/';
        }
        else {
            delete $scope.personalAgent;
        }
        console.log('username', $scope.username);
        console.log('personalagent', $scope.personalAgent);

        $scope.getEmail();
        $scope.getEvents();
        $scope.getRegistrations();
    };

    $scope.logout = function () {
        $scope.authorized = false;
        delete $scope.email;
        delete $scope.username;
        delete $scope.personalAgent;
        delete $scope.events;
        $scope.save();
    };

    /**
     * Delete current personalAgent
     */
    $scope.delete = function () {
        $.ajax({
            'type': 'DELETE',
            'url': $scope.personalAgent
        });

        this.unregisterAgent();
        this.logout();
    };

    $scope.renderSelectBoxes = function () {
        setTimeout(function () {
            $(".chzn-select").chosen();
        }, 1);
    };

    // update checkboxes in the editor after the editor is rendered
    $scope.$watch('editor.show', function() {
        $scope.renderSelectBoxes();
    });

    /**
     * Load stored settings from local storage
     */
    $scope.load = function() {
        $scope.username = localStorage['username'];
        $scope.login();
    };

    /**
     * Save stored settings to local storage
     */
    $scope.save = function () {
        if ($scope.username) {
            localStorage['username'] = $scope.username;
        }
        else {
            delete localStorage['username'];
        }
    };

    $scope.load();

}
