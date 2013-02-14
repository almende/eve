/**
 * Javascript for Agent web interface
 */

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
 * @constructor Ctrl
 * Angular JS controller to control the page
 */
function Ctrl() {
    var scope = this;

    var loadingText = '...';
    var url = document.location.href;
    var lastSlash = url.lastIndexOf('/');
    this.url         = url.substring(0, lastSlash + 1);
    this.urls        = loadingText;
    this.title       = loadingText;
    this.version     = loadingText;
    this.description = loadingText;
    this.type        = loadingText;
    this.id          = loadingText;

    // form
    this.methods = [{}];
    this.method = this.methods[0];
    this.result = '';
    this.formStatus = '';

    // json rpc
    this.request = undefined;
    this.response = undefined;
    this.rpcStatus = '';

    // event logs
    this.lastTimestamp = 0;
    this.pollingInterval = 10000;  // polling interval in milliseconds
    this.logs = [];
    this.enableEvents = true;

    /**
     * Change the currently selected method
     */
    this.setMethod = function () {
        for (var i = 0; i < this.methods.length; i++) {
            var method = this.methods[i];
            if (method.method == this.methodName) {
                this.method = method;
                break;
            }
        }
    };

    /**
     * Send a JSON-RPC request
     * @param {String} url        Url where to send the request
     * @param {JSON} request      A JSON-RPC 2.0 request, like
     *                            {"id":1,"method":"add","params":{"a":2,"b":3}}
     * @param {function} callback A callback method. The callback will be
     *                            called with a JSON-RPC response as
     *                            first argument (of type JSON), for example
     *                            {"jsonrpc":"2.0","id":1,"result":5}
     * @param {function} errback  Optional callback function in case of
     *                            an error
     */
    this.send = function (url, request, callback, errback) {
        $.ajax({
            'type': 'POST',
            'url': url,
            'contentType': 'application/json',
            'data': JSON.stringify(request),
            'success': callback,
            'error': function (err) {
                if (errback) {
                    errback(err);
                }
                else {
                    console.log(err);
                }
            }
        });
    };

    /**
     * Check whether a given type is a primitive type like 'string', 'long',
     * 'double', but not some complex type like 'Map<String, String>' or
     * 'Contact'.
     * @param {String} type   The name of a type
     * @return {boolean}      True if primitive, else false
     */
    this.isPrimitiveType = function (type) {
        var primitives = ['string', 'char', 'long', 'double', 'int',
            'number', 'float', 'byte', 'short', 'boolean'];
        return (primitives.indexOf(type.toLowerCase()) != -1);
    };

    /**
     * Format the given date as string
     * @param {Date | Number} date
     * @return {String} formattedDate
     */
    this.formatDate = function(date) {
        var d = new Date(date);
        return d.toISOString ? d.toISOString() : d.toString();
    };

    /**
     * Send an JSON-RPC request.
     * The request is built up from the current values in the form,
     * and the field result in the response is filled in in the field #result
     */
    this.sendForm = function () {
        var self = this;
        try {
            var request = {};
            request.id = 1;
            request.method = this.method.method;
            request.params = {};
            for (var i = 0; i < this.method.params.length; i++) {
                var param = this.method.params[i];
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
            this.formStatus = 'sending...';
            this.send(self.url, request, function (response) {
                var end = +new Date();
                var diff = (end - start);
                self.formStatus = 'ready in ' + diff + ' ms';

                if (response.error) {
                    self.result = 'Error: ' + JSON.stringify(response.error, null, 2);
                }
                else {
                    if (response.result instanceof Object) {
                        self.result = JSON.stringify(response.result, null, 2) || '';
                    }
                    else {
                        self.result = (response.result != undefined) ? String(response.result) : '';
                    }
                }
                self.$root.$eval();
                resize($('#result').get(0));
            }, function (err) {
                self.formStatus = 'failed. Error: ' + JSON.stringify(err);
                self.$root.$eval();
            });
        }
        catch (err) {
            self.formStatus = 'Error: ' + err;
        }
    };

    /**
     * Send a JSON-RPC request.
     * The request is read from the field #request, and the response is
     * filled in in the field #response
     */
    this.sendJsonRpc = function() {
        var self = this;
        try {
            var request = JSON.parse(this.request);
            this.request = JSON.stringify(request, null, 2);
            self.$root.$eval();
            resize($('#request').get(0));

            this.rpcStatus = 'sending...';
            var start = +new Date();
            this.send(self.url, request, function (response) {
                var end = +new Date();
                var diff = (end - start);
                self.response = JSON.stringify(response, null, 2);
                self.rpcStatus = 'ready in ' + diff + ' ms';
                self.$root.$eval();
                resize($('#response').get(0));
            }, function (err) {
                self.rpcStatus = 'failed. Error: ' + JSON.stringify(err);
                self.$root.$eval();
            });
        }
        catch (err) {
            self.rpcStatus = 'Error: ' + err;
        }
    };

    /**
     * Store the setting enableEvents
     */
    this.updateEnableEvents = function () {
        if (this.enableEvents == true) {
            // enableEvents==true is the default setting, do not store it
            delete localStorage['enableEvents'];
            this.startMonitoringEvents();
        }
        else {
            localStorage['enableEvents'] = false;
            this.stopMonitoringEvents();
            this.clearEvents();
        }
    };

    /**
     * Start monitoring the events of the agent
     */
    this.startMonitoringEvents = function () {
        scope.updateEvents();
    };

    /**
     * Stop monitoring the events of the agent
     */
    this.stopMonitoringEvents = function () {
        if (scope.updateEventsTimer) {
            clearTimeout(scope.updateEventsTimer);
            delete scope.updateEventsTimer;
        }
    };

    /**
     * Retrieve the latest event logs, and set a timeout for the next update
     */
    this.updateEvents = function () {
        scope.stopMonitoringEvents();

        $.ajax({
            'type': 'GET',
            'url': "events?since=" + scope.lastTimestamp,
            'contentType': 'application/json',
            'success': function (newLogs) {
                while (newLogs && newLogs.length) {
                    var newLog = newLogs.shift();
                    scope.lastTimestamp = newLog.timestamp;
                    scope.logs.push(newLog);
                }
                scope.lastUpdate = (new Date()).toISOString();
                scope.$root.$eval();

                // set a new timeout
                scope.updateEventsTimer = setTimeout(scope.updateEvents, scope.pollingInterval);
            },
            'error': function (err) {
                // set a new timeout
                scope.updateEventsTimer = setTimeout(scope.updateEvents, scope.pollingInterval);
            }
        });
    };

    /**
     * Clear the list with events
     */
    this.clearEvents = function () {
        scope.logs = [];
    };

    /**
     * Load information and data from the agent via JSON-RPC calls.
     * Retrieve the methods, type, id, description, etc.
     */
    this.load = function () {
        var self = this;

        // read settings from local storage
        if (localStorage['enableEvents'] != undefined) {
            this.enableEvents = localStorage['enableEvents'];
        }

        var reqs = [
            {
                'method': 'getUrls',
                'field': 'urls',
                'callback': function () {
                    self.updateEnableEvents();
                }
            },
            {
                'method': 'getType',
                'field': 'type',
                'callback': function () {
                    document.title = (self.type || 'Agent') + ' ' + (self.id || '');
                }
            },
            {
                'method': 'getId',
                'field': 'id',
                'callback': function () {
                    document.title = (self.type || 'Agent') + ' ' + (self.id || '');
                }
            },
            {'method': 'getDescription', 'field': 'description'},
            {'method': 'getVersion', 'field': 'version'},
            {
                'method': 'getMethods',
                'field': 'methods',
                'params': {'asJSON': true},
                'callback': function () {
                    if (self && self.methods && self.methods[0]) {
                        self.methodName = self.methods[0].method;
                        self.setMethod();
                        self.$root.$eval();

                        // update method select box
                        setTimeout(function () {
                            $(".chzn-select").chosen();
                        }, 15);
                    }
                }
            }
        ];

        var total = reqs.length;
        var left = total;
        var decrement = function () {
            left--;
            if (left > 0) {
                self.progress = Math.round((total - left) / total * 100) + '%';
            }
            else {
                self.loading = false;
            }
            self.$root.$eval();
        };
        for (var i = 0; i < reqs.length; i++) {
            (function (req) {
                var request = {
                    "id":1,
                    "method": req.method,
                    "params": req.params || {}
                };
                self.send(self.url, request, function(response) {
                    self[req.field] = response.result;
                    if (response.error) {
                        //self.error = JSON.stringify(response.error, null, 2);
                        var err = response.error;
                        self.error = 'Error ' + err.code + ': ' + err.message +
                            ((err.data && err.data.description) ? ', ' + err.data.description : '');
                    }
                    self.$root.$eval();
                    if (req.callback) {
                        req.callback(response.result);
                    }
                    decrement();
                }, function (err) {
                    decrement();
                    console.log(err);
                });
            })(reqs[i]);
        }
    };

    // fill in an initial JSON-RPC request
    var defaultRequest = {
        "id": 1,
        "method": "getMethods",
        "params": {
            "asJSON": false
        }
    };
    this.request = JSON.stringify(defaultRequest, null, 2);

    this.loading = true;
    this.load();
}
