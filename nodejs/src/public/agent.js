/**
 * Javascript for Agent web interface
 */

/**
 * Adjust the height of given textarea to match its contents
 * @param {Element} elem HTML DOM Textarea element
 */
function resize (elem) {
    elem.style.height = 'auto';
    elem.style.height = (elem.scrollHeight + 20) + 'px';
}

/**
 * @constructor Ctrl
 * Angular JS controller to control the page
 */
function Ctrl() {
    var scope = this;

    var loadingText = '...';
    this.url         = document.location.href;
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

    // logs
    this.lastTimestamp = 0;
    this.pollingInterval = 10000;  // polling interval in milliseconds
    this.timeToLive = 60*1000;       // time to live for the logagent
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
        var self = this;
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

    this.formatDate = function(date) {
        var d = new Date(date);
        return d.toISOString();
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
                        self.result = response.result || '';
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
     * Store the setting enableEvents,
     * and create/delete the logagent depending on the setting enableEvents
     */
    this.updateEnableEvents = function () {
        if (this.enableEvents == true) {
            // enableEvents==true is the default setting, do not store it
            delete localStorage['enableEvents'];
            this.logAgentDelete();
            this.logAgentCreate();
        }
        else {
            localStorage['enableEvents'] = false;
            this.logAgentDelete();
        }
    };

    /**
     * Create a log agent and start polling it for events
     */
    this.logAgentCreate = function () {
        var self = this;

        // clear any old logs
        this.logs = [];

        // built url for logAgent
        // TODO: this url splitting is kind of dangerous...
        var urlParts = this.url.split('/');
        urlParts.pop();
        var id = urlParts.pop();
        var clazz = urlParts.pop();
        urlParts.push('logagent');
        var logAgentId = clazz + '.' + id;
        //urlParts.push(String(this.logAgentId));
        urlParts.push(logAgentId);
        this.logAgentUrl = urlParts.join('/');
        this.$root.$eval();

        // register this agent to the logagent
        var request = {
            'id': 1,
            'method': 'addAgent',
            'params': {
                'url': self.url
            }
        };
        self.send(this.logAgentUrl, request, function (response) {
            self.logAgentUpdate();
        });

        // method for keeping the logagent alive during the session
        var timeToLive = (self.timeToLive > 60*1000) ? self.timeToLive : 60*1000;
        var setTimeToLive = function () {
            var request = {
                'id': 1,
                'method': 'setTimeToLive',
                'params': {
                    'interval': timeToLive
                }
            };
            self.send(self.logAgentUrl, request, function (response) {
                self.logAgentTTLTimer =
                    setTimeout(setTimeToLive, timeToLive - 10*1000); // set a new timeout
            }, function (err) {
                self.logAgentTTLTimer =
                    setTimeout(setTimeToLive, timeToLive - 10*1000); // set a new timeout
            });
        };
        setTimeToLive();
    };

    /**
     * Stop the log agent
     */
    this.logAgentDelete = function () {
        if (this.logAgentTimer) {
            clearTimeout(this.logAgentTimer);
            delete this.logAgentTimer;
        }
        if (this.logAgentTTLTimer) {
            clearTimeout(this.logAgentTTLTimer);
            delete this.logAgentTTLTimer;
        }
    };

    /**
     * Retrieve the latest logs from the logagent
     */
    this.logAgentUpdate = function () {
        if (this.logAgentTimer) {
            clearTimeout(this.logAgentTimer);
            delete this.logAgentTimer;
        }

        var request = {
            'id': 1,
            'method': 'getLogs',
            'params': {
                'since': scope.lastTimestamp,
                'url': scope.url
            }
        };
        scope.send(scope.logAgentUrl, request, function (response) {
            var newLogs = response.result;
            while (newLogs && newLogs.length) {
                var newLog = newLogs.shift();
                scope.lastTimestamp = newLog.timestamp;
                scope.logs.push(newLog);
            }
            scope.lastUpdate = (new Date()).toISOString();
            scope.$root.$eval();

            // set a new timeout
            scope.logAgentTimer = setTimeout(scope.logAgentUpdate, scope.pollingInterval);
        }, function (err) {
            // set a new timeout
            scope.logAgentTimer = setTimeout(scope.logAgentUpdate, scope.pollingInterval);
        });
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
                'method': 'getUrl',
                'field': 'url',
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
                'params': {},
                'callback': function () {
                    if (self && self.methods && self.methods[0]) {
                        self.methodName = self.methods[0].method;
                        self.setMethod();
                        self.$root.$eval();
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
        }
    };
    this.request = JSON.stringify(defaultRequest, null, 2);

    this.loading = true;
    this.load();
}
