

var http = require('http'),
    url = require('url'),
    request = require('request');

/**
 * @constructor Agent
 * Generic Agent prototype, containing functionality to send and receive
 * JSON-RPC requests.
 */
Agent = function () {
};


/**
 * config will contain configuration settings of Eve, such as the location of
 * the server
 */
Agent.prototype.config = {};


/**
 * Receive a JSON-RPC request, and return a JSON-RPC response via a callback
 * @param {string} request_     A string containing a JSON-RPC request
 * @param {function} callback  The JSON-RPC response is passed to the callback
 *                             function as a string.
 */
Agent.prototype._receive = function(request_, callback) {
    var id = 0;

    var getRequestId = function () {
        return id;
    };

    var agentCallback = function (result) {
        callback(JSON.stringify({
            "id": getRequestId(),
            "result": result || null,
            "error": null
        }));
    };

    var errorCallback = function (error) {
        callback(JSON.stringify({
            "id": getRequestId(),
            "result": null,
            "error": error || null
        }));
    };

    try {
        var req = JSON.parse(request_);
        id = req.id || 0;

        if (req.callback) {
            // directly return an (empty) response
            agentCallback(null);

            // create a new, async callback method
            var asyncCallback = function (error, result) {
                var agentRequest = {
                    "id": id,
                    "method": req.callback.method,
                    "params": {
                        "result": result || null,
                        "error": error || null
                    }
                };

                var options = {
                    "url" : req.callback.url,
                    "body" : JSON.stringify(agentRequest)
                };
                request.post(options, function (error, response, body) {
                    console.log(error || body);
                });
            };

            agentCallback = function (result) {
                asyncCallback(null, result);
            };
            errorCallback = function (error) {
                asyncCallback(error, null);
            }
        }

        if (this[req.method] === undefined) {
            throw {"message": "Unknown method " + (req.method || '') };
        }
        // TODO: check if the method is "public"
        this[req.method](req.params, agentCallback, errorCallback);
    }
    catch (err) {
        errorCallback(err.message);
    }
};

Agent.prototype.getId = function (params, callback, errback) {
    if (this._id === undefined) {
        throw {"message": "No id available, Agent is uninstantiated"};
    }
    callback(this._id);
};

Agent.prototype.getType = function (params, callback, errback) {
    if (this.type === undefined) {
        throw {"message": "No type available, Agent is uninstantiated"};
    }
    callback(this.type);
};

Agent.prototype.getUrl = function (params, callback, errback) {
    var location = this.config.location.href;
    callback( (location ? location : "") +
        "/" + this.type + "/" + (this._id ? this._id : "") || "");
}

Agent.prototype._send = function () {
    // TODO: implement send
};

/**
 * Check if all required parameters are present in params
 * @param {Object} params
 * @param {Array} requiredParams
 * @return {Object} error     Returns undefined if no parameters are missing,
 *                            or an object if a parameter is missing
 */
Agent.prototype._checkParams = function(params, requiredParams) {
    var errors;
    for (var i = 0, iMax = requiredParams.length; i < iMax; i++) {
        var name = requiredParams[i];
        if (params[name] === undefined) {
            if (!errors) {
                errors = [];
            }
            errors.push("Parameter '" + name + "' missing");
        }
    }

    if (errors) {
        return {"message" : (errors.length == 1) ? errors[0] : errors};
    }

    return undefined;
};

/**
 * Retrieve all available public methods of this agent
 * Methods are treated as public when they don't start with an underscore '_'
 */
Agent.prototype.getMethods = function (params, callback, errback) {
    var methods = [];
    for (var prop in this) {
        if (this.hasOwnProperty(prop)) {
            if (prop.charAt(0) != '_' && typeof(this[prop]) === 'function') {
                methods.push(prop);
            }
        }
    };
    callback(methods);
};

Agent.prototype.getDescription = function (params, callback, errback) {
    console.log("WARNING: Agent of type " + this.type +
        " has not implemented method getDescription");
    callback("Generic Agent");
};

/**
 * nodejs exports
 */
module.exports = Agent;
