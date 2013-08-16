/**
 * Javascript for Agent web interface
 */


angular.module('controller', ['ngResource']);

/**
 * Adjust the height of given textarea to match its contents
 * @param {Element} elem HTML DOM Textarea element
 */
function resize (elem) {
    var scrollTop = document.body.scrollTop;

    elem.style.height = 'auto';
    elem.style.height = (elem.scrollHeight + 20) + 'px';

    document.body.scrollTop = scrollTop;  // restore the scroll top
}

/**
 * @constructor Controller
 * Angular JS controller to control the page
 */
function Controller($scope, $resource) {
    var loadingText = '...';
    var href = document.location.pathname;
    var lastSlash = href.lastIndexOf('/');
    var url = href.substring(0, lastSlash + 1);
    var urlParts = url.split('/');
    urlParts.pop(); // remove last empty entry
    var id = urlParts.pop();
    document.title = id;

    $scope.url         = url;
    $scope.id          = id;
    $scope.urls        = undefined;
    $scope.version     = undefined;
    $scope.description = undefined;
    $scope.type        = undefined;
    $scope.mode = 'form';

    // form
    $scope.methods = undefined;
    $scope.method = undefined;
    $scope.result = '';
    $scope.formStatus = '';

    // json rpc
    $scope.request = undefined;
    $scope.response = undefined;
    $scope.rpcStatus = '';

    // event logs
    $scope.lastTimestamp = 0;
    $scope.pollingInterval = 10000;  // polling interval in milliseconds
    $scope.logs = [];
    $scope.enableEvents = true;

    // define a RESTful resource
    var agent = $resource(url + ':resource', {}, {
        'post': {method: 'POST'},
        'events': {method: 'GET', params: {resource: 'events'}, isArray: true}
    });

    /**
     * Send a json rpc message
     * @param {String} method
     * @param {Object} params
     * @param {function} callback   called with parameters err, result
     */
    function send (method, params, callback) {
        var request = {
            'id': 1,
            'method': method,
            'params': params || {}
        };
        agent.post({}, request, function(response) {
            if (response.error) {
                var err = response.error;
                $scope.error = 'Error ' + err.code + ': ' + err.message +
                    ((err.data && err.data.description) ? ', ' + err.data.description : '');
            }
            callback(response.error, response.result);
        }, function (err) {
            callback(err, undefined);
            console.log(err);
        });
    }

    /**
     * Change the currently selected method
     */
    $scope.setMethod = function () {
        for (var i = 0; i < $scope.methods.length; i++) {
            var method = $scope.methods[i];
            if (method.method == $scope.methodName) {
                $scope.method = method;
                break;
            }
        }
    };

    /**
     * Check whether a given type is a primitive type like 'string', 'long',
     * 'double', but not some complex type like 'Map<String, String>' or
     * 'Contact'.
     * @param {String} type   The name of a type
     * @return {boolean}      True if primitive, else false
     */
    $scope.isPrimitiveType = function (type) {
        var primitives = ['string', 'char', 'long', 'double', 'int',
            'number', 'float', 'byte', 'short', 'boolean'];
        return (primitives.indexOf(type.toLowerCase()) != -1);
    };

    /**
     * Format the given date as string
     * @param {Date | Number} date
     * @return {String} formattedDate
     */
    $scope.formatDate = function(date) {
        var d = new Date(date);
        return d.toISOString ? d.toISOString() : d.toString();
    };

    /**
     * Send an JSON-RPC request.
     * The request is built up from the current values in the form,
     * and the field result in the response is filled in in the field #result
     */
    $scope.sendForm = function () {
        try {
            var request = {};
            request.id = 1;
            request.method = $scope.method.method;
            request.params = {};
            for (var i = 0; i < $scope.method.params.length; i++) {
                var param = $scope.method.params[i];
                if (param.required || (param.value && param.value.length > 0) ) {
                    if (param.type.toLowerCase() == 'string') {
                        request.params[param.name] = param.value;
                    }
                    else {
                        request.params[param.name] = JSON.parse(param.value);
                    }
                }
            }

            var start = +new Date();
            $scope.formStatus = 'sending...';
            agent.post({}, request, function (response) {
                var end = +new Date();
                var diff = (end - start);
                $scope.formStatus = 'ready in ' + diff + ' ms';

                if (response.error) {
                    $scope.result = 'Error: ' + JSON.stringify(response.error, null, 2);
                }
                else {
                    if (response.result instanceof Object) {
                        $scope.result = JSON.stringify(response.result, null, 2) || '';
                    }
                    else {
                        $scope.result = (response.result != undefined) ? String(response.result) : '';
                    }
                }

                $scope.resize(document.getElementById('result'));
            }, function (err) {
                $scope.formStatus = 'failed. Error: ' + JSON.stringify(err);
                $scope.result = '';
            });
        }
        catch (err) {
            $scope.formStatus = 'Error: ' + err;
            $scope.result = '';
        }
    };

    /**
     * Resize given element after a delay of 0ms
     * @param elem
     */
    $scope.resize = function (elem) {
        setTimeout(function () {
            resize(elem);
        }, 0);
    };

    /**
     * Send a JSON-RPC request.
     * The request is read from the field #request, and the response is
     * filled in in the field #response
     */
    $scope.sendJsonRpc = function() {
        try {
            var request = JSON.parse($scope.request);
            $scope.request = JSON.stringify(request, null, 2);
            resize(document.getElementById('request'));

            $scope.rpcStatus = 'sending...';
            var start = +new Date();
            agent.post({}, request, function (response) {
                var end = +new Date();
                var diff = (end - start);
                $scope.response = JSON.stringify(response, null, 2);
                $scope.rpcStatus = 'ready in ' + diff + ' ms';

                $scope.resize(document.getElementById('response'));
            }, function (err) {
                $scope.rpcStatus = 'failed. Error: ' + JSON.stringify(err);
                $scope.response = '';

            });
        }
        catch (err) {
            $scope.rpcStatus = 'Error: ' + err;
            $scope.response = ''
        }
    };

    /**
     * Store the setting enableEvents
     */
    $scope.updateEnableEvents = function () {
        if ($scope.enableEvents == true) {
            // enableEvents==true is the default setting, do not store it
            delete localStorage['enableEvents'];
            $scope.startMonitoringEvents();
        }
        else {
            localStorage['enableEvents'] = false;
            $scope.stopMonitoringEvents();
            $scope.clearEvents();
        }
    };

    /**
     * Start monitoring the events of the agent
     */
    $scope.startMonitoringEvents = function () {
        $scope.updateEvents();
    };

    /**
     * Stop monitoring the events of the agent
     */
    $scope.stopMonitoringEvents = function () {
        if ($scope.updateEventsTimer) {
            clearTimeout($scope.updateEventsTimer);
            delete $scope.updateEventsTimer;
        }
    };

    /**
     * Retrieve the latest event logs, and set a timeout for the next update
     */
    $scope.updateEvents = function () {
        $scope.stopMonitoringEvents();

        agent.events({since: $scope.lastTimestamp}, undefined, function (newLogs) {
            while (newLogs && newLogs.length) {
                var newLog = newLogs.shift();
                $scope.lastTimestamp = newLog.timestamp;
                $scope.logs.push(newLog);
            }
            $scope.lastUpdate = (new Date()).toISOString();

            // set a new timeout
            $scope.updateEventsTimer = setTimeout($scope.updateEvents, $scope.pollingInterval);
        }, function (err) {
            console.log(err);

            // set a new timeout
            $scope.updateEventsTimer = setTimeout($scope.updateEvents, $scope.pollingInterval);
        });
    };

    /**
     * Clear the list with events
     */
    $scope.clearEvents = function () {
        $scope.logs = [];
    };

    /**
     * Load information and data from the agent via JSON-RPC calls.
     * Retrieve the methods, type, id, description, etc.
     */
    $scope.load = function () {
        // read settings from local storage
        if (localStorage['enableEvents'] != undefined) {
            $scope.enableEvents = localStorage['enableEvents'];
        }
        $scope.updateEnableEvents();

        // get id
        send ('getId', {}, function (err, result) {
            if (!err) {
                $scope.id = result;
                document.title = result;
            }
        });

        // get urls
        send ('getUrls', {}, function (err, result) {
            if (!err) {
                $scope.urls = result;
            }
        });

        // get type
        send ('getType', {}, function (err, result) {
            if (!err) {
                $scope.type = result;
            }
        });

        // get description
        send ('getDescription', {}, function (err, result) {
            if (!err) {
                $scope.description = result;
            }
        });

        // get version
        send ('getVersion', {}, function (err, result) {
            if (!err) {
                $scope.version = result;
            }
        });

        // get methods
        send ('getMethods', {}, function (err, result) {
            if (!err) {
                $scope.methods = result;
                $scope.methodName = $scope.methods[0].method;
                $scope.setMethod();

                // update method select box
                setTimeout(function () {
                    new Chosen(document.getElementById('methods'));
                }, 15);
            }
        });
    };

    // fill in an initial JSON-RPC request
    var defaultRequest = {
        "id": 1,
        "method": "getMethods",
        "params": {
        }
    };
    $scope.request = JSON.stringify(defaultRequest, null, 2);

    $scope.loading = true;
    $scope.load();
}
