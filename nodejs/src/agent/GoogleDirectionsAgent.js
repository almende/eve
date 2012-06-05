
var Agent = require('./Agent.js'),
    qs = require('querystring'),
    request = require('request');

/**
 * @constructor GoogleDirectionsAgent
 * GoogleDirectionsAgent can retrieve directions information
 */
GoogleDirectionsAgent = function () {
    this.type = "GoogleDirectionsAgent";
};
GoogleDirectionsAgent.prototype = new Agent();

// constants
GoogleDirectionsAgent.prototype.DIRECTIONS_SERVICE =
    "http://maps.googleapis.com/maps/api/directions/json";
GoogleDirectionsAgent.prototype.KEY_STRING =
    "ABQIAAAAQOJzPEiBDTDlB2oHxRVmTxRSrjmNg-hdT5E1_a3uQ7J2AKkR7hTFenoJvK-F_h8dho7B4VXJZx1pdg";

GoogleDirectionsAgent.prototype.getDescription = function(params, callback, errback) {
    callback("GoogleDirectionsAgent can retrieve travel information like " +
        "duration and distance between an origin and a destination. " +
        "the agent uses the Google Directions API.");
};

GoogleDirectionsAgent.prototype.getDirections = function(params, callback, errback) {
    // check if all required params are provided
    var error = this._checkParams(params, ["origin", "destination"]);
    if (error) {
        errback(error);
        return;
    }

    // built up request url
    var url = GoogleDirectionsAgent.prototype.DIRECTIONS_SERVICE +
        "?origin="      + qs.escape(params.origin) +
        "&destination=" + qs.escape(params.destination) +
        // "&mode=driving" +   // driving, walking, or bicycling
        // "&language=nl" +     // nl, en, ...
        "&sensor=false" ;
    //"&key=" + GoogleDirectionsAgent.KEY_STRING;
    // TODO: key needed again when deploying somewhere on a server?

    request.get(url, function(error, response, body) {
        if (!error && response.statusCode == 200) {
            var directions = JSON.parse(body);
            if (directions.status && directions.status === "OK") {
                callback(directions);
            }
            else {
                errback ({"message" : (directions.status || "Unknown error")});
            }
        }
        else {
            errback(error || body);
        }
    });
};

GoogleDirectionsAgent.prototype.getDuration =
    function(params, callback, errback) {
        var subback = function (directions) {
            var duration = directions.routes[0].legs[0].duration.value;
            callback(duration);
        };
        this.getDirections(params, subback, errback);
    };

GoogleDirectionsAgent.prototype.getDurationHuman =
    function(params, callback, errback) {
        var subback = function (directions) {
            var duration = directions.routes[0].legs[0].duration.text;
            callback(duration);
        };
        this.getDirections(params, subback, errback);
    };

GoogleDirectionsAgent.prototype.getDistance =
    function(params, callback, errback) {
        var subback = function (directions) {
            var distance = directions.routes[0].legs[0].distance.value;
            callback(distance);
        };
        this.getDirections(params, subback, errback);
    };

GoogleDirectionsAgent.prototype.getDistanceHuman =
    function(params, callback, errback) {
        var subback = function (directions) {
            var distance = directions.routes[0].legs[0].distance.text;
            callback(distance);
        };
        this.getDirections(params, subback, errback);
    };


/**
 * nodejs exports
 */
module.exports = GoogleDirectionsAgent;
