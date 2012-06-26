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

// TODO: implement a real UUID solution
// http://stackoverflow.com/questions/105034/how-to-create-a-guid-uuid-in-javascript
var UUID = {
    'randomUUID': function (withSeparators) {
        var S4 = function() {
            return (((1+Math.random())*0x10000)|0).toString(16).substring(1);
        };
        if (withSeparators) {
            return (S4()+S4()+"-"+S4()+"-"+S4()+"-"+S4()+"-"+S4()+S4()+S4());
        }
        else {
            return (S4()+S4()+S4()+S4()+S4()+S4()+S4()+S4());
        }
    }
};

/**
 * @constructor Ctrl
 * Angular JS controller to control the page
 */
function Ctrl() {
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
    this.logAgentId = UUID.randomUUID();

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
            var self = this;
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
                self.self.formStatus = 'failed. Error: ' + JSON.stringify(err);
                self.$root.$eval();
            });
        }
        catch (err) {
            this.formStatus = 'Error: ' + err;
        }
    };

    /**
     * Send an JSON-RPC request.
     * The request is built up from the current values in the form,
     * and the field result in the response is filled in in the field #result
     */
    this.sendForm = function () {
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
            var self = this;
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
                self.self.formStatus = 'failed. Error: ' + JSON.stringify(err);
                self.$root.$eval();
            });
        }
        catch (err) {
            this.formStatus = 'Error: ' + err;
        }
    };

    /**
     * Send a JSON-RPC request.
     * The request is read from the field #request, and the response is
     * filled in in the field #response
     */
    this.sendJsonRpc = function() {
        try {
            var self = this;
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
            this.rpcStatus = 'Error: ' + err;
        }
    };

    /**
     * Get and validate the polling interval
     * @return {Number} interval in milliseconds
     */
    this.getPollingInterval = function () {
        var interval = Number(this.pollingInterval);
        if (interval <= 0) {
            interval = 1;
            this.pollingInterval = interval;
        }
        if (interval > 10000) {
            interval = 10000;
            this.pollingInterval = interval;
        }
        return interval;
    };

    /**
     * Create a log agent and start polling it for events
     */
    this.createLogAgent = function () {
        var self = this;

        // built url for logAgent
        // TODO: this url splitting is kind of dangerous...
        var urlParts = this.url.split('/');
        urlParts.pop();
        urlParts.pop();
        urlParts.pop();
        urlParts.push('logagent');
        urlParts.push(String(this.logAgentId));
        this.logAgentUrl = urlParts.join('/');
        this.$root.$eval();

        // method for retrieving the logs
        var getLogs = function () {
            var request = {
                'id': 1,
                'method': 'getLogs',
                'params': {
                    'since': self.lastTimestamp,
                    'url': self.url
                }
            };
            self.send(self.logAgentUrl, request, function (response) {
                var newLogs = response.result;
                while (newLogs && newLogs.length) {
                    var newLog = newLogs.shift();
                    self.lastTimestamp = newLog.timestamp;
                    self.logs.push(newLog);
                }
                self.$root.$eval();

                // set a new timeout
                self.logAgentTimer = setTimeout(getLogs, self.getPollingInterval());
            }, function (err) {
                // set a new timeout
                self.logAgentTimer = setTimeout(getLogs, self.getPollingInterval());
            });
        };

        // register this agent to the logagent
        var request = {
            'id': 1,
            'method': 'addAgent',
            'params': {
                'url': self.url
            }
        };
        self.send(this.logAgentUrl, request, function (response) {
            getLogs();
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
                setTimeout(setTimeToLive, timeToLive - 10*1000); // set a new timeout
            }, function (err) {
                setTimeout(setTimeToLive, timeToLive - 10*1000); // set a new timeout
            });
        };
        setTimeToLive();
    };

    /**
     * Load information and data from the agent via JSON-RPC calls.
     * Retrieve the methods, type, id, description, etc.
     */
    this.load = function () {
        var self = this;
        var reqs = [
            {
                'method': 'getUrl',
                'field': 'url',
                'callback': function () {
                    self.createLogAgent();
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
