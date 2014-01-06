package com.almende.eve.transport.xmpp.google;

import java.io.IOException;
import java.util.logging.Logger;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.almende.eve.agent.AgentHost;
import com.almende.eve.config.Config;
import com.almende.eve.rpc.jsonrpc.JSONRPCException;
import com.almende.eve.rpc.jsonrpc.JSONResponse;
import com.google.appengine.api.xmpp.JID;
import com.google.appengine.api.xmpp.Message;
import com.google.appengine.api.xmpp.MessageBuilder;
import com.google.appengine.api.xmpp.XMPPService;
import com.google.appengine.api.xmpp.XMPPServiceFactory;

@SuppressWarnings("serial")
public class GaeXmppServlet extends HttpServlet {
	protected AgentHost			host		= null;
	protected GaeXmppService	xmppService	= null;
	
	private Logger				logger		= Logger.getLogger(this.getClass()
													.getSimpleName());
	
	/**
	 * Handle a post request containing an xmpp chat message
	 * 
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
			try {
				String agentUrl = "xmpp:" + to.getId();
				String agentId = xmppService != null ? xmppService
						.getAgentId(agentUrl) : null;
				
				logger.info("request agentUrl =" + agentUrl + ", agentId="
						+ agentId + " request=" + body + ", sender="
						+ from.getId());
				
				// invoke the agent
				host.receive(agentId, body, from.getId(), null);
				
			} catch (Exception err) {
				// generate JSON error response
				JSONRPCException jsonError = new JSONRPCException(
						JSONRPCException.CODE.INTERNAL_ERROR, err.getMessage());
				JSONResponse response = new JSONResponse(jsonError);
				
				// send exception as response
				Message msg = new MessageBuilder().withRecipientJids(from)
						.withFromJid(to).withBody(response.toString()).build();
				xmpp.sendMessage(msg);
			}
		}
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
	 * 
	 * @throws Exception
	 */
	private void initAgentFactory() throws Exception {
		// TODO: be able to choose a different namespace
		host = AgentHost.getInstance();
		
		// if the agent factory is not configured, load it from config
		if (host.getConfig() == null) {
			String filename = getInitParameter("config");
			if (filename == null) {
				filename = "eve.yaml";
				logger.warning("Init parameter 'config' missing in servlet configuration web.xml. "
						+ "Trying default filename '" + filename + "'.");
			}
			String fullname = "/WEB-INF/" + filename;
			logger.info("loading configuration file '"
					+ getServletContext().getRealPath(fullname) + "'...");
			Config config = new Config(getServletContext().getResourceAsStream(
					fullname));
			host = AgentHost.getInstance();
			host.loadConfig(config);
		}
	}
	
	/**
	 * Register a new gae xmpp service at the agent factory
	 * 
	 * @throws Exception
	 */
	private void initTransportService() throws Exception {
		if (host == null) {
			throw new Exception(
					"Cannot initialize GaeXmppService: no AgentFactory initialized.");
		}
		
		xmppService = new GaeXmppService();
		host.addTransportService(xmppService);
	}
}
