
var Agent = require('./Agent.js'),
    qs = require('querystring'),
    request = require('request'), // npm install request, https://github.com/mikeal/request
    ISODate = require('isodate'); // npm install isodate, https://github.com/pvorb/isodate

// TODO: for performance optimization: the HTTP requests are redirected to an url including a sessionid.
//       make use of this session id to prevent redirect with every request

/**
 * Documentation:
 *   http://code.google.com/apis/gdata/articles/using_cURL.html
 *   http://code.google.com/apis/calendar/data/2.0/reference.html
 *   http://code.google.com/apis/gdata/docs/2.0/reference.html#Queries
 */

/**
 * Post a form via https
 * @param {String} url
 * @param {Object} fields key/values with the form fields
 * @param {function} callback A callback function
 */
function postForm(url, fields, callback, errback) {
    // built up multi part form data
    var boundary = Math.random();
    var body = '';
    for (var name in fields) {
        if (fields.hasOwnProperty(name)) {
            body += "--" + boundary + "\r\n";
            body += "Content-Disposition: form-data; name=\"" + name + "\"\r\n\r\n";
            body += fields[name] + "\r\n";
        }
    }

    var options = {
        'url': url,
        'body': body,
        'headers': {
            'Content-Type': 'multipart/form-data; boundary=' + boundary,
            'Content-Length': body.length
        }
    };

    request.post(options, function (error, response, body) {
        if (!error && response.statusCode == 200) {
            callback(body);
        }
        else {
            errback({"message" : error || body});
        }
    });
}



/**
 * @constructor GoogleCalendarAgent
 * GoogleCalendarAgent can interact with a Google Calendar
 */
GoogleCalendarAgent = function () {
    this.type = "GoogleCalendarAgent";

    this.username = undefined;
    this.authToken = undefined;
};
GoogleCalendarAgent.prototype = new Agent();


/**
 * Retrieve authentication token needed to access Google Calendar
 * @param {Object} params       Object containing parameters username and
 *                              password
 * @param {function} callback   will be called with the string authToken as
 *                              parameter
 * @param {function} errback    will be called when an error occurs,
 *                              with an error object as parameter
 */
GoogleCalendarAgent.prototype.authenticate = function(params, callback, errback) {
    // check if all required params are provided
    var error = this._checkParams(params, ["username", "password"]);
    if (error) {
        errback(error);
        return;
    }

    var url = 'https://www.google.com/accounts/ClientLogin';
    var fields = {
        'accountType': 'HOSTED_OR_GOOGLE',
        'Email': params.username,
        'Passwd': params.password,
        'service': 'cl', // http://code.google.com/apis/gdata/faq.html#clientlogin
        'source': 'Test'
    };

    var me = this;
    postForm(url, fields, function (data) {
        me.username = undefined;
        me.authToken = undefined;
        var lines = data.split('\n');
        for (var i in lines) {
            if (lines.hasOwnProperty(i)) {
                var keyvalue = lines[i].split('=');
                if (keyvalue[0] == 'Auth') {
                    me.username = params.username;
                    me.authToken = keyvalue[1];
                    callback("Authentication succesful");
                    return;
                }
            }
        }

        errback({"message" : "Auth not found in response"});
    });
};

/**
 * Get the username
 * @param {Object} params       Empty object
 * @param {function} callback   Callback function, will be called with
 *                              an array with calendars as parameter.
 * @param {function} errback    will be called when an error occurs,
 *                              with an error object as parameter
 */
GoogleCalendarAgent.prototype.getUsername = function(params, callback, errback) {
    callback(this.username);
};

/**
 * Retrieve all calendars
 * @param {Object} params       Empty object
 * @param {function} callback   Callback function, will be called with
 *                              an array with calendars as parameter.
 * @param {function} errback    will be called when an error occurs,
 *                              with an error object as parameter
 */
GoogleCalendarAgent.prototype.getCalendars = function(params, callback, errback) {
    var options = {
        'url' : 'https://www.google.com/calendar/feeds/default/allcalendars/full?alt=json',
        headers: {
            'Authorization' : 'GoogleLogin auth=' + this.authToken
        }
    };

    request.get(options, function (error, response, body) {
        if (!error && response.statusCode == 200) {
            // retrieve title and link of the calendars from the response
            var feed = JSON.parse(body).feed,
                entries = feed.entry,
                calendars = [];

            for (var i in entries) {
                if (entries.hasOwnProperty(i)) {
                    var entry = entries[i];

                    calendars.push({
                        'title': entry.title.$t,
                        'link': entry.content.src
                    });
                }
            }

            callback(calendars);
        }
        else {
            errback(error || body);
        }
    });
};

/**
 * Retrieve all events from the calendar within given
 * @param {Object} params       Object containing parameters start and end,
 *                              both a Number representing a datetime
 * @param {function} callback   Callback function, will be called with
 *                              an array with events as parameter.
 * @param {function} errback    will be called when an error occurs,
 *                              with an error object as parameter
 */
GoogleCalendarAgent.prototype.getEvents = function (params, callback, errback) {
    // check if all required params are provided
    var error = this._checkParams(params, ["start", "end"]);
    if (error) {
        errback(error);
        return;
    }

    var url = "https://www.google.com/calendar/feeds/" + this.username +
        "/private/full" +
        "?start-min=" + qs.escape(new Date(params.start).toISO8061()) +
        "&start-max=" + qs.escape(new Date(params.end).toISO8061()) +
        "&alt=json";

    var options = {
        'url' : url,
        headers: {
            'Authorization' : 'GoogleLogin auth=' + this.authToken
        }
    };

    var me = this;
    request.get(options, function (error, response, body) {
        if (!error && response.statusCode == 200) {
            // retrieve title and link of the calendars from the response
            var entries = JSON.parse(body).feed.entry,
                events = [];

            for (var i in entries) {
                if (entries.hasOwnProperty(i)) {
                    var entry = entries[i];
                    var event = me._entryToEvent(entry);
                    events.push(event);
                }
            }
            callback(events);
        }
        else {
            errback(error || body);
        }
    });
};


/**
 * Retrieve all events from the calendar within given
 * @param {Object} params       Object containing parameters start and end,
 *                              both a Number representing a datetime
 * @param {function} callback   Callback function, will be called with
 *                              an array with events as parameter.
 * @param {function} errback    will be called when an error occurs,
 *                              with an error object as parameter
 */
GoogleCalendarAgent.prototype.getEvents = function (params, callback, errback) {
    // check if all required params are provided
    var error = this._checkParams(params, ["start", "end"]);
    if (error) {
        errback(error);
        return;
    }

    var url = "https://www.google.com/calendar/feeds/" + this.username +
        "/private/full" +
        "?start-min=" + qs.escape(new Date(params.start).toISO8061()) +
        "&start-max=" + qs.escape(new Date(params.end).toISO8061()) +
        "&alt=json";

    var options = {
        'url' : url,
        headers: {
            'Authorization' : 'GoogleLogin auth=' + this.authToken
        }
    };

    var me = this;
    request.get(options, function (error, response, body) {
        if (!error && response.statusCode == 200) {
            // retrieve title and link of the calendars from the response
            var entries = JSON.parse(body).feed.entry,
                events = [];

            for (var i in entries) {
                if (entries.hasOwnProperty(i)) {
                    var entry = entries[i];
                    var event = me._entryToEvent(entry);
                    events.push(event);
                }
            }
            callback(events);
        }
        else {
            errback(error || body);
        }
    });
};


/**
 * Retrieve all events from the calendar within given
 * @param {Object} params       Object containing parameter event,
 *                              which is a calendar event
 * @param {function} callback   Callback function, will be called with
 *                              an array with events as parameter.
 * @param {function} errback    will be called when an error occurs,
 *                              with an error object as parameter
 */
GoogleCalendarAgent.prototype.updateEvent = function (params, callback, errback) {
    // check if all required params are provided
    var error = this._checkParams(params, ["event"]);
    if (error) {
        errback(error);
        return;
    }

    errback({"message": "Not yet implemented..."});

    /* TODO
     var event = params.event;

     var now = new Date();
     var startDate = new Date(now.getFullYear(), now.getMonth(), now.getDate());
     var endDate = new Date(now.getFullYear(), now.getMonth(), now.getDate() + 1);

     var url = "https://www.google.com/calendar/feeds/" + this.username +
     "/private/full" +
     "?start-min=" + qs.escape(startDate.toISO8061()) +
     "&start-max=" + qs.escape(endDate.toISO8061()) +
     "&alt=json";

     var options = {
     'url' : url,
     headers: {
     'Authorization' : 'GoogleLogin auth=' + this.authToken
     }
     };

     var me = this;
     request.get(options, function (error, response, body) {
     if (!error && response.statusCode == 200) {
     // retrieve title and link of the calendars from the response
     var entries = JSON.parse(body).feed.entry,
     events = [];

     for (var i in entries) {
     var entry = entries[i];

     entry.gd$where = [{
     "valueString": "Rotterdam, Westerstraat 50"
     }];

     var body = JSON.stringify(entry);
     var putOptions = {
     'url': entry.id.$t,
     'followRedirect': true,
     'body': "{}",
     headers: {
     'Authorization' : 'GoogleLogin auth=' + me.authToken,
     'Content-Type' : 'application/json',
     'Content-Length' : body.length
     }
     };

     request.put(putOptions, function (error, response, body) {
     if (response.statusCode == 302) {
     // moved temporarily
     console.log('redirecting...');
     var body = JSON.stringify(entry);
     var putOptions = {
     'url': entry.id.$t,
     'body': "{}",
     headers: {
     'Authorization' : 'GoogleLogin auth=' + me.authToken,
     'Content-Type' : 'application/json',
     'Content-Length' : body.length
     }
     };
     request.put(putOptions, function (error, response, body) {
     console.log('redirected', request, error, response, body);
     });
     }
     else {
     console.log(error, response, body)
     }
     });


     //var event = me._entryToEvent(entry);
     //events.push(event);
     }
     callback(events);
     }
     else {
     errback(error || body);
     }
     });
     */
}


/**
 * Convert a google calendar entry to a generic calendar event
 */
GoogleCalendarAgent.prototype._entryToEvent =  function (entry) {
    var event = {
        'url': entry.id.$t,
        'recurrence': undefined, // TODO: retrieve recurrence
        'updated': ISODate(entry.updated.$t).getTime(),
        'title': entry.title.$t,
        'when': [],
        'where': [],
        'who': []
    }

    // read when
    for (var i = 0, iMax = entry.gd$when.length; i < iMax; i++) {
        event.when.push({
            "start" : ISODate(entry.gd$when[i].startTime).getTime(),
            "end" : ISODate(entry.gd$when[i].endTime).getTime(),
        });
    }

    // read where
    for (var i = 0, iMax = entry.gd$where.length; i < iMax; i++) {
        event.where.push(entry.gd$where[i].valueString);
    }

    // read who
    for (var i = 0, iMax = entry.gd$who.length; i < iMax; i++) {
        event.who.push({
            "name" : "",  // TODO: retrieve name
            "email" : entry.gd$who[i].email,
        });
    }

    return event;
}


/**
 * Retrieve all events from the calendar from today
 * This is just a convenience method for easy testing.
 * @param {Object} params       Empty object
 * @param {function} callback   Callback function, will be called with
 *                              an array with events as parameter.
 * @param {function} errback    will be called when an error occurs,
 *                              with an error object as parameter
 */
GoogleCalendarAgent.prototype.getEventsToday =
    function (params, callback, errback) {
        var now = new Date(),
            startDate = new Date(now.getFullYear(), now.getMonth(), now.getDate()),
            endDate = new Date(now.getFullYear(), now.getMonth(), now.getDate() + 1),
            params = {
                "start" : startDate.getTime(),
                "end" : endDate.getTime()
            };

        this.getEvents(params, callback, errback);
    }


/**
 * nodejs exports
 */
module.exports = GoogleCalendarAgent;
