
var Agent = require('./Agent.js');

/**
 * @constructor UserAgent
 * UserAgent represents a user
 */
UserAgent = function () {
    this.type = "UserAgent";
    this.username = undefined;
};
UserAgent.prototype = new Agent();

UserAgent.prototype.setUsername = function(params, callback, errback) {
    var error = this._checkParams(params, ["username"]);
    if (error) {
        errback(error);
        return;
    }

    this.username = params.username;
    callback();
};

UserAgent.prototype.getUsername = function(params, callback, errback) {
    callback(this.username);
};


/**
 * nodejs exports
 */
module.exports = UserAgent;
