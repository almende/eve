package com.almende.eve.transport.xmpp.google;

import java.io.IOException;
import java.util.logging.Logger;

import javax.servlet.http.*;

import com.almende.eve.agent.AgentFactory;
import com.almende.eve.agent.annotation.Sender;
import com.almende.eve.config.Config;
import com.almende.eve.rpc.RequestParams;
import com.almende.eve.rpc.jsonrpc.JSONRPCException;
import com.almende.eve.rpc.jsonrpc.JSONRequest;
import com.almende.eve.rpc.jsonrpc.JSONResponse;
import com.almende.eve.rpc.jsonrpc.jackson.JOM;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.appengine.api.xmpp.JID;
import com.google.appengine.api.xmpp.Message;
import com.google.appengine.api.xmpp.MessageBuilder;
import com.google.appengine.api.xmpp.XMPPService;
import com.google.appengine.api.xmpp.XMPPServiceFactory;

@SuppressWarnings("serial")
public class GaeXmppServlet extends HttpServlet {
	protected AgentFactory agentFactory = null;
	protected GaeXmppService xmppService = null;

	private Logger logger = Logger.getLogger(this.getClass().getSimpleName());

	/**
	 * Handle a post request containing an xmpp chat message
	 * @param req
	 * @param res
	 */
    public void doPost(HttpServletRequest req, HttpServletResponse res)
          throws IOException {
        XMPPService xmpp = XMPPServiceFactory.getXMPPService();

        // receive message
        Message message = xmpp.parseMessage(req);
        JID from = message.getFromJid();
        JID[] recipients = message.getRecipientJids();
        JID to = (recipients.length > 0) ? recipients[0] : null;
        
        String body = message.getBody();
		if (body != null && body.startsWith("{") || body.trim().startsWith("{")) {
			// the body contains a JSON object
			ObjectNode json = null;
			try {
				String agentUrl = "xmpp:" + to.getId();
				String agentId = xmppService != null ? xmppService.getAgentId(agentUrl) : null;
				
				json = JOM.getInstance().readValue(body, ObjectNode.class);
				if (isResponse(json)) {
					// TODO: handle response
				}
				else if (isRequest(json)) {
					// this is a request
					
					// append the sender to the request parameters
					RequestParams params = new RequestParams();
					params.put(Sender.class, from.getId());

					// TODO: cleanup logger info
					logger.info("request agentUrl =" + agentUrl + ", agentId=" + agentId + " request=" + json + ", sender=" + from.getId());

					// invoke the agent
					JSONRequest request = new JSONRequest(json);
					JSONResponse response = agentFactory.invoke(agentId, request, params);

					// reply to message
			        Message msg = new MessageBuilder()
				        .withRecipientJids(from)
				        .withFromJid(to)
				        .withBody(response.toString())
				        .build();
			        xmpp.sendMessage(msg);	
				}
				else {
					throw new Exception("Request does not contain a valid JSON-RPC request or response");
				}
			}
			catch (Exception err) {
				// generate JSON error response
				JSONRPCException jsonError = new JSONRPCException(
						JSONRPCException.CODE.INTERNAL_ERROR, err.getMessage());
				JSONResponse response = new JSONResponse(jsonError);
				
				// send exception as response
		        Message msg = new MessageBuilder()
			        .withRecipientJids(from)
			        .withFromJid(to)
			        .withBody(response.toString())
			        .build();
		        xmpp.sendMessage(msg);
			}
		}
    }

	/**
	 * Check if given json object contains all fields required for a 
	 * json-rpc request (id, method, params)
	 * @param json
	 * @return
	 */
	private boolean isRequest(ObjectNode json) {
		return json.has("method");
	}

	/**
	 * Check if given json object contains all fields required for a 
	 * json-rpc response (id, result or error)
	 * @param json
	 * @return
	 */
	private boolean isResponse(ObjectNode json) {
		return (json.has("result") || json.has("error"));
	}
	
	@Override
	public void init() {
		try {
			initAgentFactory();
			initTransportService();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * initialize the agent factory
	 * @throws Exception 
	 */
	private void initAgentFactory() throws Exception {
		// TODO: be able to choose a different namespace 
		agentFactory = AgentFactory.getInstance();
		if (agentFactory == null) {
			// if the agent factory is not yet loaded, load it from config
			String filename = getInitParameter("config");
			if (filename == null) {
				filename = "eve.yaml";
				logger.warning(
					"Init parameter 'config' missing in servlet configuration web.xml. " +
					"Trying default filename '" + filename + "'.");
			}
			String fullname = "/WEB-INF/" + filename;
			logger.info("loading configuration file '" + 
					getServletContext().getRealPath(fullname) + "'...");
			Config config = new Config(getServletContext().getResourceAsStream(fullname));
			
			agentFactory = AgentFactory.createInstance(config);
		}
	}
	
	
	/**
	 * Register a new gae xmpp service at the agent factory
	 * @throws Exception
	 */
	private void initTransportService() throws Exception {
		if (agentFactory == null) {
			throw new Exception(
					"Cannot initialize GaeXmppService: no AgentFactory initialized.");
		}

		xmppService = new GaeXmppService(); 
		agentFactory.addTransportService(xmppService);
	}
}
