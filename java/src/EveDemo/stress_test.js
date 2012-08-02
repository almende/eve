/**
 * This script tests concurrent requests to Eve
 */ 

var http = require('http'),
    url = require('url'),
    qs = require('querystring');

var SERVLET_URL = 'http://localhost:8888/agents/';
var CALCAGENT = SERVLET_URL + 'calcagent/1/';
var TESTAGENT = SERVLET_URL + 'testagent/1/';
var CALENDARAGENT = SERVLET_URL + 'googlecalendaragent/jos/';

/**
 * Execute an HTTP request
 * If data is provided, a POST request is performed. Else a GET request is 
 * performed.
 * @param {String} requestUrl   
 * @param {String or Object} data   Optional data
 * @param {function} callback   A callback function, with the response data
 *                              as parameter.
 */ 
function http_request(requestUrl, data, callback) {
    var u = url.parse(requestUrl);
    
    var options = {
        host: u.hostname,
        port: u.port || 80,
        path: u.pathname + (u.query ? '?' + u.query : ''),
        method: data ? 'POST' : 'GET'
    };
        
    var payload;
    switch( typeof(data) ) {
        case "string": 
          payload = data;
          break;
        case "object":
          payload = JSON.stringify(data);
          options.headers = options.headers || {};
          options.headers["Content-Type"] = "application/json";
          break;
        case "undefined":
          payload = undefined;
          break;
        default:
          payload = String(data);
          break;
    }
    
    if (payload) {
		options.headers = options.headers || {};
		options.headers['Content-Length'] = payload.length;
	}

    var req = http.request(options, function(res) {
        var data = "";
        res.setEncoding('utf8');
        res.on('data', function (chunk) {
            data += chunk;
        });
        res.on('end', function () {
            if (callback) {
                callback(data);
            }
        });
    });
    req.on('error', function(e) {
        throw e;
    });

    if (payload) {
        req.write(payload);
    }
    req.end();
}

var id = 0;
function getId() {
	id++;
	return id;
}

function JSONRPC(url, method, params, callback) {
	var body = {
		'id': getId(),
		'method': method,
		'params': params
	};
	http_request(url, JSON.stringify(body), function (resp) {
        console.log('req\n\turl  ' + url + '\n\tbody ' + JSON.stringify(body));
        console.log('resp\n\tbody ' + resp);
		callback(resp);
	});
}

/* Test calc agent
for (var i = 0; i < 100; i++) {
	JSONRPC(CALCAGENT, 'eval', {'expr': '2/3'}, function (resp) {});
}
*/

/*
// Test test agent
for (var i = 0; i < 1000; i++) {
	//JSONRPC(TESTAGENT, 'get', {'key': 'name'}, function (resp) {});
	//JSONRPC(TESTAGENT, 'put', {'key': 'name', 'value': 'Jos ' + i}, function (resp) {});
	JSONRPC(TESTAGENT, 'callMyself', {'method': 'put', 'params': {'key': 'name', 'value': 'Jos ' + i}}, function (resp) {});
	JSONRPC(TESTAGENT, 'callMyself', {'method': 'get', 'params': {'key': 'name'}}, function (resp) {});
}
*/

/*
// Test test agent
for (var i = 0; i < 1000; i++) {
	JSONRPC(TESTAGENT, 'cascade', {}, function (resp) {});
}
*/


// Test calendar agent
JSONRPC(CALENDARAGENT, 'createEventQuick', {'summary': 'Test X'},
    function (resp) {
        var event = JSON.parse(resp).result;
        for (var i = 0; i < 100; i++) {
            event.summary = 'Test ' + i;
            JSONRPC(CALENDARAGENT, 'getEvent', {'eventId': event.id}, function(resp) {});
            JSONRPC(CALENDARAGENT, 'updateEvent', {'event': event}, function(resp) {});
            JSONRPC(CALENDARAGENT, 'getEventsToday', {}, function (resp) {});
        }
    }
);


console.log('\ndone');
