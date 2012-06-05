/**
 * Test authentication and access to a Google Calendar from Server-side Javascript 
 * 
 * nodejs appliation
 * 
 * uses the request library, install this via 
 *     npm install request
 * 
 * to run this demo, enter
 *     node get_google_calendars.js
 * 
 * http://code.google.com/apis/gdata/articles/using_cURL.html
 * http://en.wikipedia.org/wiki/MIME#Multipart%5Fmessages
 * http://onteria.wordpress.com/2011/05/30/multipartform-data-uploads-using-node-js-and-http-request/
 */

var request = require('request');

/**
 * Post a form via https
 * @param {String} url 
 * @param {Object} fields key/values with the form fields
 * @param {function} callback A callback function
 */ 
function postForm(url, fields, callback) {
  // built up multi part form data
  var boundary = Math.random();
  var body = '';  
  for (var name in fields) {
    body += "--" + boundary + "\r\n";
    body += "Content-Disposition: form-data; name=\"" + name + "\"\r\n\r\n";
    body += fields[name] + "\r\n";
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
      console.log(error || body);
    }
  });
}

/**
 * Retrieve authentication token needed to access Google Calendar
 * @param {String} username
 * @param {String} password
 * @param {function} callback  will be called with the string authToken as 
 *                             parameter
 */ 
function getAuthToken(username, password, callback) {
  var url = 'https://www.google.com/accounts/ClientLogin';
  var fields = {
    'accountType': 'HOSTED_OR_GOOGLE',
    'Email': username,
    'Passwd': password,
    'service': 'cl', // http://code.google.com/apis/gdata/faq.html#clientlogin
    'source': 'Test'    
  };

  postForm(url, fields, function (data) {
    var authToken = undefined;
    var lines = data.split('\n');
    for (var i in lines) {
      var keyvalue = lines[i].split('=');
      if (keyvalue[0] == 'Auth') {
        authToken = keyvalue[1];
        break;
      }
    }
    
    callback(authToken);
  });  
};

/**
 * Retrieve all calendars 
 * @param {String} authToken    Authentication token retrieved via getAuthToken
 * @param {function} callback   Callback function, will be called with
 *                              an array with calendars as parameter.
 */
function getCalendars(authToken, callback) {
  var options = {
    'url' : 'https://www.google.com/calendar/feeds/default/allcalendars/full?alt=json',
    headers: {
      'Authorization' : 'GoogleLogin auth=' + authToken
    }
  };
  
  request.get(options, function (error, response, body) {
    if (!error && response.statusCode == 200) {
      // retrieve title and link of the calendars from the response
      var feed = JSON.parse(body).feed,
        entries = feed.entry,
        calendars = [];

      for (var i in entries) {
        var entry = entries[i],
          title = entry.title.$t,
          link  = entry.content.src;
        
        calendars.push({
          'title': title, 
          'link': link
        });        
      }

      callback(calendars);
    }
    else {
      console.log(error || body);
    }
  });
}


console.log("Test authentication to Google Calendar");

var username = 'EMAIL';
var password = 'PASSWORD';

getAuthToken(username, password, function (authToken) {
  getCalendars(authToken, function (calendars) {
    console.log(calendars);
  });
});

