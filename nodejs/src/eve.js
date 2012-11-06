
var http = require('http'),
    url = require('url'),
    couch_client = require('couch-client'),
    ManagerAgent = require('./agent/ManagerAgent.js');

// create namespace
var eve = {};

eve.location = {
    href: undefined   // will contain server location like "http://host:port"
};
eve.couchdb = couch_client("http://localhost:5984/eve");
eve.agents = {};


eve.add = function (agent) {
    var instance = new agent();
    eve.agents[instance.type] = agent;

    if (instance instanceof ManagerAgent) {
        agent.prototype.config = {
            "location": eve.location,
            "couchdb": eve.couchdb,
            "agents": eve.agents
        };
    }
    else {
        agent.prototype.config = {
            "location": eve.location
        };
    }
};

eve.add(ManagerAgent);


eve.processRequest = function (agentType, agentId, request, callback) {
    var id = -1;
    try {
        if (agentId) {
            // load the agent from database
            eve.couchdb.get(agentId, function (err, doc) {
                if (err) {
                    throw err;
                }

                // instantiate the agent
                var agent = new eve.agents[doc.type]();
                for (var prop in doc) {
                    if (doc.hasOwnProperty(prop)) {
                        agent[prop] = doc[prop];
                    }
                }

                var agentBefore = JSON.stringify(agent);

                // execute the request
                agent._receive(request, function (response) {
                    // TODO: checking if the agent changed by stringifying it is quite slow. Find a better solution for this.
                    var agentAfter = JSON.stringify(agent);
                    var changed = (agentBefore != agentAfter);
                    if (changed) {
                        // store the agent again
                        eve.couchdb.save(agent, function (err, doc) {
                            if (err) {
                                throw err;
                            }
                            callback(response);
                        });
                    }
                    else {
                        callback(response);
                    }
                });
            });
        }
        else if (agentType && eve.agents[agentType]) {
            // instantiate a new agent (not from database)
            var agent = new eve.agents[agentType]();
            agent._receive(request, callback);
        }
        else {
            throw {"message": "Unknown agent type '" + agentType + "'"};
        }
    }
    catch (err) {
        var response = {
            "id": id,
            "result": null,
            "error": err
        };
        callback(JSON.stringify(response));
    }
};

eve.handleRequest = function(req, res, next) {
    var data = "";
    if(req.method === 'POST') {
        // instantiate the correct type of agent, extract this type from the url
        var pathname = url.parse(req.url).pathname,
            parts = pathname.split('/'),
            type = parts[1],
            id = parts[2];
        req.on("data", function(chunk) {
            data += chunk;
        });

        req.on("end", function() {
            console.log(req.url);
            console.log(data);

            eve.processRequest(type, id, data, function(response) {
                console.log(response);
                res.writeHead(200, {'Content-Type': 'application/json'});
                res.end(response);
            });
        });
        return;
    } else if(req.method === 'GET') {

        res.sendfile(__dirname + '/public/index.html');
        return;
    }

    next();
};

/**
 * nodejs exports
 */
exports.handleRequest = eve.handleRequest;
exports.add = eve.add;
