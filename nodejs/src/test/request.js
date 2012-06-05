/**
 * https://github.com/mikeal/request
 */ 

var request = require('request');

request('http://www.google.com', function (error, response, body) {
  if (!error) {
    console.log(body);
  }
});
