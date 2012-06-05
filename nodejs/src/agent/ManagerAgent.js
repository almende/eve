
var Agent = require('./Agent.js');

/**
 * @constructor ManagerAgent
 * ManagerAgent can create and delete agents
 */
ManagerAgent = function () {
    this.type = "ManagerAgent";
};

ManagerAgent.prototype = new Agent();

ManagerAgent.prototype.getDescription = function (params, callback, errback) {
    callback("The ManagerAgent can create, delete, and list agents.");
};

ManagerAgent.prototype.create = function (params, callback, errback) {
    var error = this._checkParams(params, ["type"]);
    if (error) {
        errback(error);
        return;
    }

    var type = params.type;
    if (!this.config.agents[type]) {
        errback({"message": "Unknown type of agent '" + type + "'"});
        return;
    }

    if (!this.config.couchdb) {
        errback({"message": "No couch database found in configuration"});
        return;
    }

    var couchdb = this.config.couchdb;
    var agent = new this.config.agents[type]();

    couchdb.save(agent, function (err, doc) {
        if (err) {
            errback (err);
        }
        else {
            var url = agent.getUrl();
            callback (url);
        }
    });
};

ManagerAgent.prototype.list = function (params, callback, errback) {
    if (!this.config.couchdb) {
        errback({"message": "No couch database found in configuration"});
        return;
    }

    // TODO: view used here must be generated if it does not exist!!!

    var location = this.config.location.href;
    var couchdb = this.config.couchdb;
    var path = couchdb.uri.pathname + "/_design/agents/_view/agents";
    couchdb.view(path, undefined, function (err, res) {
        //couchdb.request("GET", couchdb.uri.pathname + "/_all_docs", function (err, res) {
        if (err) {
            errback (err);
        }
        else {
            var docs = res.rows;
            var urls = [];

            for (var i = 0, iMax = docs.length; i < iMax; i++) {
                var doc = docs[i];
                urls.push(location + "/" + doc.value.type + "/" + doc.id);
            }

            callback (urls);
        }
    });
};

ManagerAgent.prototype.delete = function (params, callback, errback) {
    var error = this._checkParams(params, ["url"]);
    if (error) {
        errback(error);
        return;
    }

    if (!this.config.couchdb) {
        errback({"message": "No couch database found in configuration"});
        return;
    }

    // get id from url
    var location = this.config.location.href;
    var url = params.url;
    if (url.substring(0, location.length) !== location) {
        errback({"message": "Url is not equal to server location"});
        return;
    }
    var id = url.substring(location.length + 1);

    var couchdb = this.config.couchdb;
    couchdb.remove(id, function (err, doc) {
        if (err) {
            errback (err);
        }
        else {
            callback ();
        }
    });
};

/**
 * nodejs exports
 */
module.exports = ManagerAgent;
