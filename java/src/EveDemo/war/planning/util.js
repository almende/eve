
/**
 * Send a JSON-RPC 2.0 call to an agent
 * @param {Object} params  Object containing fields:<br>
 *                         - {String} url,<br>
 *                         - {String} method,<br>
 *                         - {Object} [params],<br>
 *                         - {function} [success],<br>
 *                         - {function} [error]<br>
 */
function jsonrpc(params) {
    if (!params.url) {
        throw Error('url missing');
    }
    if (!params.method) {
        throw Error('method missing');
    }
    var req = {
        'id': 1,
        'method': params.method,
        'params': params.params || {}
    };
    var success = params.success || function (result) {};
    var error = params.error || function (err) {
        console.log('error', err);
    };

    $.ajax({
        'type': 'POST',
        'url': params.url,
        'contentType': 'application/json',
        'data': JSON.stringify(req),
        'success': function (resp) {
            if (resp.error) {
                error(resp.error);
            }
            else {
                success(resp.result);
            }
        },
        'error': function (err) {
            error(err);
        }
    });
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
