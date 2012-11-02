
var Agent = require('./Agent.js');

/**
 * @constructor CalcAgent
 * CalcAgent is an agent which can do some calculations
 */
CalcAgent = function () {
    this.type = "CalcAgent";
};
CalcAgent.prototype = new Agent();

CalcAgent.prototype.getDescription = function(params, callback, errback) {
    callback("CalcAgent is an agent which can do some amazing calculations.");
};

CalcAgent.prototype.getVersion = function(params, callback, errback) {
    callback("1.0");
};

CalcAgent.prototype.add = function (params, callback, errback) {
    callback(params.a + params.b);
};

CalcAgent.prototype.addCallback = function(params, callback, errback) {
    // TODO: cleanup this temporary function
    console.log("result=" + params);
};

CalcAgent.prototype.sum = function(params, callback, errback) {
    if (params.values === undefined) {
        errback({"message": "Parameter 'values' missing"});
        return;
    }
    if (!(params.values instanceof Array)) {
        errback({"message": "Parameter 'values' must be an Array"});
        return;
    }

    var total = 0;
    for (var p = 0, pMax = params.values.length; p < pMax; p++) {
        total += params.values[p];
    }
    callback(total);
};

CalcAgent.prototype.ping = function(params, callback, errback) {
    callback(params);
};

/**
 * nodejs exports
 */
module.exports = CalcAgent;
