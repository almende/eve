/*
 * Copyright: Almende B.V. (2014), Rotterdam, The Netherlands
 * License: The Apache Software License, Version 2.0
 */
package com.almende.eve.transport.http;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;

import com.almende.eve.agent.Agent;
import com.almende.eve.agent.AgentHost;
import com.almende.eve.agent.AgentSignal;
import com.almende.eve.agent.callback.CallbackInterface;
import com.almende.eve.agent.callback.SyncCallback;
import com.almende.eve.agent.log.Log;
import com.almende.eve.config.Config;
import com.almende.eve.rpc.jsonrpc.jackson.JOM;
import com.almende.util.StreamingUtil;
import com.almende.util.StringUtil;
import com.almende.util.tokens.TokenStore;
import com.almende.util.uuid.UUID;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * The Class AgentServlet.
 */
@SuppressWarnings("serial")
public class AgentServlet extends HttpServlet {
	
	private static final Logger	LOG			= Logger.getLogger(AgentServlet.class
													.getSimpleName());
	private static final String	RESOURCES	= "/com/almende/eve/resources/";
	private static AgentHost	host;
	private static HttpService	httpTransport;
	
	/*
	 * (non-Javadoc)
	 * 
	 * @see javax.servlet.GenericServlet#init()
	 */
	@Override
	public void init() {
		if (AgentHost.getInstance().getStateFactory() == null) {
			LOG.severe("DEPRECIATED SETUP: Please add com.almende.eve.transport.http.AgentListener as a Listener to your web.xml!");
			AgentListener.init(getServletContext());
		}
		host = AgentHost.getInstance();
		
		final String environment = Config.getEnvironment();
		final String envParam = "environment." + environment + ".servlet_url";
		final String globalParam = "servlet_url";
		String servletUrl = getInitParameter(envParam);
		if (servletUrl == null) {
			// if no environment specific servlet_url is defined, read
			// the global servlet_url
			servletUrl = getInitParameter(globalParam);
		}
		if (servletUrl == null) {
			LOG.severe("Cannot initialize HttpTransport: " + "Init Parameter '"
					+ globalParam + "' or '" + envParam + "' "
					+ "missing in context configuration web.xml.");
		}
		httpTransport = new HttpService(host, servletUrl);
		host.addTransportService(httpTransport);
	}
	
	/**
	 * The Enum Handshake.
	 */
	enum Handshake {
		
		/** The ok. */
		OK,
		/** The nak. */
		NAK,
		/** The invalid. */
		INVALID
	}
	
	/**
	 * Handle hand shake.
	 * 
	 * @param req
	 *            the req
	 * @param res
	 *            the res
	 * @return true, if successful
	 * @throws IOException
	 *             Signals that an I/O exception has occurred.
	 */
	private boolean handleHandShake(final HttpServletRequest req,
			final HttpServletResponse res) throws IOException {
		final String time = req.getHeader("X-Eve-requestToken");
		
		if (time == null) {
			return false;
		}
		
		final String token = TokenStore.get(time);
		if (token == null) {
			res.sendError(HttpServletResponse.SC_PRECONDITION_FAILED);
		} else {
			res.setHeader("X-Eve-replyToken", token);
			res.setStatus(HttpServletResponse.SC_OK);
			res.flushBuffer();
		}
		return true;
	}
	
	/**
	 * Do hand shake.
	 * 
	 * @param req
	 *            the req
	 * @return the handshake
	 */
	private Handshake doHandShake(final HttpServletRequest req) {
		final String tokenTupple = req.getHeader("X-Eve-Token");
		if (tokenTupple == null) {
			return Handshake.NAK;
		}
		
		try {
			final String senderUrl = req.getHeader("X-Eve-SenderUrl");
			if (senderUrl != null && !senderUrl.equals("")) {
				final ObjectNode tokenObj = (ObjectNode) JOM.getInstance()
						.readTree(tokenTupple);
				final HttpGet httpGet = new HttpGet(senderUrl);
				httpGet.setHeader("X-Eve-requestToken", tokenObj.get("time")
						.textValue());
				final HttpResponse response = ApacheHttpClient.get().execute(
						httpGet);
				if (response.getStatusLine().getStatusCode() == HttpServletResponse.SC_OK) {
					if (tokenObj
							.get("token")
							.textValue()
							.equals(response.getLastHeader("X-Eve-replyToken")
									.getValue())) {
						return Handshake.OK;
					}
				} else {
					LOG.log(Level.WARNING, "Failed to receive valid handshake:"
							+ response);
				}
			}
		} catch (final Exception e) {
			LOG.log(Level.WARNING, "", e);
		}
		
		return Handshake.INVALID;
	}
	
	/**
	 * Handle session.
	 * 
	 * @param req
	 *            the req
	 * @param res
	 *            the res
	 * @return true, if successful
	 * @throws IOException
	 *             Signals that an I/O exception has occurred.
	 */
	private boolean handleSession(final HttpServletRequest req,
			final HttpServletResponse res) throws IOException {
		try {
			
			if (req.getSession(false) != null) {
				return true;
			}
			
			final Handshake hs = doHandShake(req);
			if (hs.equals(Handshake.INVALID)) {
				return false;
			}
			
			String doAuthenticationStr = AgentListener
					.getParam("eve_authentication");
			if (doAuthenticationStr == null) {
				// TODO: authentication param is deprecated since v2.0. Cleanup
				// some day
				doAuthenticationStr = AgentListener.getParam("authentication");
				if (doAuthenticationStr == null) {
					doAuthenticationStr = "true";
					LOG.warning("context-param \"eve_authentication\" not found. Using default value "
							+ doAuthenticationStr);
				} else {
					LOG.warning("context-param \"authentication\" is deprecated. Use \"eve_authentication\" instead.");
				}
			}
			final Boolean doAuthentication = Boolean
					.parseBoolean(doAuthenticationStr);
			
			if (hs.equals(Handshake.NAK) && doAuthentication) {
				if (!req.isSecure()) {
					res.sendError(HttpServletResponse.SC_BAD_REQUEST,
							"Request needs to be secured with SSL for session management!");
					return false;
				}
				if (!req.authenticate(res)) {
					return false;
				}
			}
			// generate new session:
			req.getSession(true);
		} catch (final Exception e) {
			res.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
					"Exception running HandleSession:" + e.getMessage());
			LOG.log(Level.WARNING, "", e);
			return false;
		}
		return true;
	}
	
	/**
	 * Get an agents web interface Usage: GET /servlet/{agentId}.
	 * 
	 * @param req
	 *            the req
	 * @param resp
	 *            the resp
	 * @throws ServletException
	 *             the servlet exception
	 * @throws IOException
	 *             Signals that an I/O exception has occurred.
	 */
	@Override
	protected void doGet(final HttpServletRequest req,
			final HttpServletResponse resp) throws ServletException,
			IOException {
		final String uri = req.getRequestURI();
		String agentId;
		try {
			agentId = httpTransport.getAgentId(new URI(uri));
		} catch (URISyntaxException e) {
			throw new ServletException(
					"AgentServlet has a strange URL, can't find agentId!");
		}
		String resource = httpTransport.getAgentResource(uri);
		
		// if no agentId is found, return generic information on servlet usage
		if (agentId == null || agentId.equals("")) {
			resp.getWriter().write(getServletDocs());
			resp.setContentType("text/plain");
			return;
		}
		
		// check if the agent exists
		try {
			if (!host.hasAgent(agentId)) {
				resp.sendError(HttpServletResponse.SC_NOT_FOUND,
						"Agent with id '" + agentId + "' not found.");
				return;
			}
		} catch (final Exception e) {
			throw new ServletException(e);
		}
		
		// If this is a handshake request, handle it.
		if (handleHandShake(req, resp)) {
			return;
		}
		
		try {
			if (host.getAgent(agentId).hasPrivate()
					&& !handleSession(req, resp)) {
				if (!resp.isCommitted()) {
					resp.sendError(HttpServletResponse.SC_UNAUTHORIZED);
				}
				return;
			}
		} catch (final Exception e1) {
			LOG.log(Level.WARNING, "", e1);
		}
		// get the resource name from the end of the url
		if (resource == null || resource.equals("")) {
			if (!uri.endsWith("/") && !resp.isCommitted()) {
				final String redirect = uri + "/";
				resp.sendRedirect(redirect);
				return;
			}
			resource = "index.html";
		}
		final String extension = resource
				.substring(resource.lastIndexOf('.') + 1);
		
		if (resource.equals("events")) {
			// retrieve the agents logs
			final String sinceStr = req.getParameter("since");
			Long since = null;
			if (sinceStr != null) {
				try {
					since = Long.valueOf(sinceStr);
				} catch (final java.lang.NumberFormatException e) {
					LOG.warning("Couldn't parse 'since' parameter:'" + since
							+ "'");
				}
			}
			
			try {
				final List<Log> logs = host.getEventLogger().getLogs(agentId,
						since);
				resp.addHeader("Content-type", "application/json");
				JOM.getInstance().writer().writeValue(resp.getWriter(), logs);
			} catch (final Exception e) {
				LOG.log(Level.WARNING, "", e);
				resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
						e.getMessage());
			}
		} else {
			// load the resource
			final String mimetype = StreamingUtil.getMimeType(extension);
			final String filename = RESOURCES + resource;
			final InputStream is = this.getClass()
					.getResourceAsStream(filename);
			if (is != null) {
				StreamingUtil.streamBinaryData(is, mimetype, resp);
			} else {
				throw new ServletException("Resource '" + resource
						+ "' not found");
			}
		}
	}
	
	/**
	 * Send a JSON-RPC message to an agent Usage: POST /servlet/{agentId} With a
	 * JSON-RPC request as body. Response will be a JSON-RPC response.
	 * 
	 * @param req
	 *            the req
	 * @param resp
	 *            the resp
	 * @throws IOException
	 *             Signals that an I/O exception has occurred.
	 * @throws ServletException
	 */
	@Override
	public void doPost(final HttpServletRequest req,
			final HttpServletResponse resp) throws IOException,
			ServletException {
		
		// retrieve the agent url and the request body
		final String body = StringUtil.streamToString(req.getInputStream());
		
		final String agentUrl = req.getRequestURI();
		String agentId;
		try {
			agentId = httpTransport.getAgentId(new URI(agentUrl));
		} catch (URISyntaxException e) {
			throw new ServletException(
					"AgentServlet has a strange URL, can't find agentId!");
		}
		if (agentId == null || agentId.equals("")) {
			resp.sendError(HttpServletResponse.SC_BAD_REQUEST,
					"No agentId found in url.");
			resp.flushBuffer();
			return;
		}
		Agent agent = null;
		try {
			agent = host.getAgent(agentId);
		} catch (final Exception e) {
			LOG.log(Level.WARNING, "Couldn't get agent:" + agentId, e);
		}
		if (agent == null) {
			resp.sendError(HttpServletResponse.SC_NOT_FOUND,
					"Agent not found at this host.");
			resp.flushBuffer();
			return;
		}
		
		if (agent.hasPrivate() && !handleSession(req, resp)) {
			if (!resp.isCommitted()) {
				resp.sendError(HttpServletResponse.SC_UNAUTHORIZED);
			}
			resp.flushBuffer();
			return;
		}
		
		// Attach the claimed senderId, or null if not given.
		String senderUrl = req.getHeader("X-Eve-SenderUrl");
		if (senderUrl == null || senderUrl.equals("")) {
			senderUrl = "web://" + req.getRemoteUser() + "@"
					+ req.getRemoteAddr();
		}
		final String tag = new UUID().toString();
		
		final SyncCallback<String> callback = new SyncCallback<String>();
		
		final CallbackInterface<String> callbacks = host.getCallbackService(
				"HttpTransport", String.class);
		callbacks.store(tag, callback);
		host.receive(agentId, body, URI.create(senderUrl), tag);
		
		try {
			final Object message = callback.get();
			// return response
			resp.addHeader("Content-Type", "application/json");
			resp.getWriter().println(message.toString());
			resp.getWriter().close();
		} catch (final Exception e) {
			LOG.log(Level.WARNING, "Sync receive raised exception.", e);
			resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
					"Receiver raised exception:" + e.getMessage());
		}
		resp.flushBuffer();
	}
	
	/**
	 * Create a new agent Usage: PUT /servlet/{agentId}?type={agentType} Where
	 * agentType is the full class path of the agent. Returns a list with the
	 * urls of the created agent.
	 * 
	 * @param req
	 *            the req
	 * @param resp
	 *            the resp
	 * @throws ServletException
	 *             the servlet exception
	 * @throws IOException
	 *             Signals that an I/O exception has occurred.
	 */
	@Override
	protected void doPut(final HttpServletRequest req,
			final HttpServletResponse resp) throws ServletException,
			IOException {
		final String agentUrl = req.getRequestURI();
		String agentId;
		try {
			agentId = httpTransport.getAgentId(new URI(agentUrl));
		} catch (URISyntaxException e) {
			throw new ServletException(
					"AgentServlet has a strange URL, can't find agentId!");
		}
		String agentType = req.getParameter("type");
		
		if (!handleSession(req, resp)) {
			if (!resp.isCommitted()) {
				resp.sendError(HttpServletResponse.SC_UNAUTHORIZED);
			}
			return;
		}
		if (agentType == null) {
			// TODO: class is deprecated since 2013-02-19. Remove this some day
			agentType = req.getParameter("class");
			LOG.warning("Query parameter 'class' is deprecated. Use 'type' instead.");
		}
		
		if (agentId == null) {
			resp.sendError(HttpServletResponse.SC_BAD_REQUEST,
					"No agentId found in url.");
			return;
		}
		if (agentType == null || agentType.equals("")) {
			resp.sendError(HttpServletResponse.SC_BAD_REQUEST,
					"Query parameter 'type' missing in url.");
			return;
		}
		
		try {
			final Agent agent = host.createAgent(agentType, agentId);
			for (final String url : agent.getUrls()) {
				resp.getWriter().println(url);
			}
			agent.signalAgent(new AgentSignal<Void>(AgentSignal.DESTROY, null));
		} catch (final Exception e) {
			throw new ServletException(e);
		}
	}
	
	/**
	 * Delete an agent usage: DELETE /servlet/agentId.
	 * 
	 * @param req
	 *            the req
	 * @param resp
	 *            the resp
	 * @throws ServletException
	 *             the servlet exception
	 * @throws IOException
	 *             Signals that an I/O exception has occurred.
	 */
	@Override
	protected void doDelete(final HttpServletRequest req,
			final HttpServletResponse resp) throws ServletException,
			IOException {
		final String agentUrl = req.getRequestURI();
		String agentId;
		try {
			agentId = httpTransport.getAgentId(new URI(agentUrl));
		} catch (URISyntaxException e) {
			throw new ServletException(
					"AgentServlet has a strange URL, can't find agentId!");
		}
		
		if (!handleSession(req, resp)) {
			if (!resp.isCommitted()) {
				resp.sendError(HttpServletResponse.SC_UNAUTHORIZED);
			}
			return;
		}
		if (agentId == null) {
			resp.sendError(HttpServletResponse.SC_BAD_REQUEST,
					"No agentId found in url.");
			return;
		}
		
		try {
			host.deleteAgent(agentId);
			resp.getWriter().write("Agent " + agentId + " deleted");
		} catch (final Exception e) {
			throw new ServletException(e);
		}
	}
	
	/**
	 * Get a description on how to use this servlet.
	 * 
	 * @return info
	 */
	protected String getServletDocs() {
		final String servletUrl = httpTransport.getServletUrl();
		final String info = "EVE AGENTS SERVLET\n" + "\n" + "Usage:\n" + "\n" +
		
		"GET "
				+ servletUrl
				+ "\n"
				+ "\n"
				+ "    Returns information on how to use this servlet.\n"
				+ "\n"
				+
				
				"GET "
				+ servletUrl
				+ "{agentId}\n"
				+ "\n"
				+ "    Returns an agents web interface, allowing for easy interaction\n"
				+ "    with the agent.\n"
				+ "    A 404 error will be returned when the agent does not exist.\n"
				+ "\n"
				+
				
				"POST "
				+ servletUrl
				+ "{agentId}\n"
				+ "\n"
				+ "    Send an RPC call to an agent.\n"
				+ "    The body of the request must contain a JSON-RPC request.\n"
				+ "    The addressed agent will execute the request and return a\n"
				+ "    JSON-RPC response. This response can contain the result or\n"
				+ "    an exception.\n"
				+ "    A 404 error will be returned when the agent does not exist.\n"
				+ "\n"
				+
				
				"PUT "
				+ servletUrl
				+ "{agentId}?type={agentType}\n"
				+ "\n"
				+ "    Create an agent. agentId can be any string. agentType must\n"
				+ "    be a full java class path of an Agent. A 500 error will be\n"
				+ "    thrown when an agent with this id already exists.\n"
				+ "\n" +
				
				"DELETE " + servletUrl + "{agentId}\n" + "\n"
				+ "    Delete an agent by its id.";
		
		return info;
	}
}
