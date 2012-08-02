/**
 * Angular JS controller to control the page
 * @constructor Ctrl
 */
function Planning($scope) {
    // constants
    //var ORIGIN = window.location.origin + '/';
    var ORIGIN = window.location.protocol + '//' + window.location.host + '/';
    var AUTH_SERVLET = ORIGIN + 'auth/google';
    var AGENTS_SERVLET = ORIGIN + 'agents/';
    var CALENDAR_AGENT_URI = AGENTS_SERVLET + 'googlecalendaragent/';
    var MEETING_AGENT_URI = AGENTS_SERVLET + 'meetingagent/';
    var DIRECTORY_AGENT_URI = AGENTS_SERVLET + 'directoryagent/1/';
    var CALLBACK_URI = window.location.href;

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
        if (!$scope.calendarAgent) {
            $scope.authorized = false;
            return;
        }

        $scope.emailLoading = true;
        jsonrpc({
            'url': $scope.calendarAgent,
            'method': 'getEmail',
            'params': {},
            'success': function (email) {
                delete $scope.emailLoading;
                $scope.authorized = (email != null);
                $scope.email = email;
                $scope.$apply();
                console.log('email', email);

                $scope.registerCalendarAgent({
                    'url': $scope.calendarAgent,
                    'username': $scope.username,
                    'email': $scope.email
                });
            },
            'error': function (err) {
                $scope.authorized = false;
                delete $scope.emailLoading;
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
        if (!$scope.calendarAgent) {
            return;
        }

        $scope.startInterval(interval);

        // determine time interval
        var now = new Date();
        var nowPlus7 = new Date();
        nowPlus7.setDate(nowPlus7.getDate() + 7);

        $scope.updating = true;
        jsonrpc({
            'url': $scope.calendarAgent,
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
     * Authorize the current calendarAgent
     */
    $scope.authorize = function () {
        // redirect to a url to authorize this agent
        window.location.href = AUTH_SERVLET +
            '?agentUrl=' + $scope.calendarAgent +
            '&agentMethod=setAuthorization' +
            '&applicationCallback=' + CALLBACK_URI;
    };

    /**
     * Delete current calendarAgent
     */
    $scope.delete = function () {
        jsonrpc({
            'url': $scope.calendarAgent,
            'method': 'clear',
            'params': {}
        });

        this.unregisterCalendarAgent();

        $scope.authorized = false;
        delete $scope.email;
        delete $scope.username;
        delete $scope.calendarAgent;
        delete $scope.events;
        $scope.save();
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
        var url = MEETING_AGENT_URI + uuid;
        var activity = {
            'agent': url,
            'summary': 'new event',
            'constraints': {
                'time': {
                    'duration': 60 * 60 * 1000
                },
                'attendees': [
                    {
                        'agent': $scope.calendarAgent,
                        'username': $scope.username,
                        'email': $scope.email
                    }
                ]
            }
        };

        $scope.editor.edit(activity);
    };

    $scope.deleteEvent = function (agent) {
        jsonrpc({
            'url': agent,
            'method': 'clear',
            'params': {},
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

    $scope.registerCalendarAgent = function(params) {
        if (params.url && params.email) {
            // register the calendar agent when all fields are populated
            jsonrpc({
                'url': DIRECTORY_AGENT_URI,
                'method': 'register',
                'params': {
                    'url': params.url,
                    'type': 'calendaragent',
                    'username': params.username,
                    'email': params.email
                },
                'success': function () {
                    $scope.getRegistrations();
                }
            });
        }
    };

    $scope.unregisterCalendarAgent = function() {
        jsonrpc({
            'url': DIRECTORY_AGENT_URI,
            'method': 'unregister',
            'params': {
                'url': $scope.calendarAgent,
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
                $scope.registrations = registrations;
                $scope.$apply();
            }
        });
    };

    $scope.applyUsername = function () {
        $scope.authorized = false;
        $scope.usernameChanged = false;

        if ($scope.username && $scope.username.length > 0) {
            $scope.calendarAgent = CALENDAR_AGENT_URI + $scope.username;
        }
        else {
            delete $scope.calendarAgent;
        }
        console.log('username', $scope.username);
        console.log('calendarAgent', $scope.calendarAgent);

        $scope.getEmail();
        $scope.getEvents();
        $scope.getRegistrations();
    };

    $scope.renderSelectBoxes = function () {
        setTimeout(function () {
            $(".chzn-select").chosen();
        }, 1);
    };

    // update checkboxes in the editor after the editor is rendered
    $scope.$watch('editor.show', function() {
        setTimeout(function () {
            $scope.renderSelectBoxes();
        }, 1);
    });

    /**
     * Load stored settings from local storage
     */
    $scope.load = function() {
        $scope.username = localStorage['username'];
        $scope.applyUsername();
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
