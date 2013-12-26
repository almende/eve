package com.almende.eve.transport.http;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
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
import com.almende.eve.rpc.jsonrpc.JSONRPC;
import com.almende.eve.rpc.jsonrpc.JSONRPCException;
import com.almende.eve.rpc.jsonrpc.JSONResponse;
import com.almende.eve.rpc.jsonrpc.jackson.JOM;
import com.almende.util.StreamingUtil;
import com.almende.util.StringUtil;
import com.almende.util.tokens.TokenStore;
import com.almende.util.uuid.UUID;
import com.fasterxml.jackson.databind.node.ObjectNode;

@SuppressWarnings("serial")
public class AgentServlet extends HttpServlet {
	private static final Logger	LOG			= Logger.getLogger(AgentServlet.class
													.getSimpleName());
	
	private static final String	RESOURCES	= "/com/almende/eve/resources/";
	private static AgentHost	agentHost;
	private static HttpService	httpTransport;
	
	@Override
	public void init() {
		if (AgentHost.getInstance().getStateFactory() == null) {
			LOG.severe("DEPRECIATED SETUP: Please add com.almende.eve.transport.http.AgentListener as a Listener to your web.xml!");
			AgentListener.init(getServletContext());
		}
		agentHost = AgentHost.getInstance();
		
		String environment = Config.getEnvironment();
		String envParam = "environment." + environment + ".servlet_url";
		String globalParam = "servlet_url";
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
		httpTransport = new HttpService(agentHost,servletUrl);
		agentHost.addTransportService(httpTransport);
	}
	
	enum Handshake {
		OK, NAK, INVALID
	}
	
	private boolean handleHandShake(HttpServletRequest req,
			HttpServletResponse res) throws IOException {
		String time = req.getHeader("X-Eve-requestToken");
		
		if (time == null) {
			return false;
		}
		
		String token = TokenStore.get(time);
		if (token == null) {
			res.sendError(HttpServletResponse.SC_PRECONDITION_FAILED);
		} else {
			res.setHeader("X-Eve-replyToken", token);
			res.setStatus(HttpServletResponse.SC_OK);
			res.flushBuffer();
		}
		return true;
	}
	
	private Handshake doHandShake(HttpServletRequest req) {
		String tokenTupple = req.getHeader("X-Eve-Token");
		if (tokenTupple == null) {
			return Handshake.NAK;
		}
		
		try {
			String senderUrl = req.getHeader("X-Eve-SenderUrl");
			if (senderUrl != null && !senderUrl.equals("")) {
				ObjectNode tokenObj = (ObjectNode) JOM.getInstance().readTree(
						tokenTupple);
				HttpGet httpGet = new HttpGet(senderUrl);
				httpGet.setHeader("X-Eve-requestToken", tokenObj.get("time")
						.textValue());
				HttpResponse response = ApacheHttpClient.get().execute(httpGet);
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
		} catch (Exception e) {
			LOG.log(Level.WARNING, "", e);
		}
		
		return Handshake.INVALID;
	}
	
	private boolean handleSession(HttpServletRequest req,
			HttpServletResponse res) throws IOException {
		try {
			
			if (req.getSession(false) != null) {
				return true;
			}
			
			Handshake hs = doHandShake(req);
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
			Boolean doAuthentication = Boolean
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
		} catch (Exception e) {
			res.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
					"Exception running HandleSession:" + e.getMessage());
			LOG.log(Level.WARNING, "", e);
			return false;
		}
		return true;
	}
	
	/**
	 * Get an agents web interface Usage: GET /servlet/{agentId}
	 */
	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp)
			throws ServletException, IOException {
		String uri = req.getRequestURI();
		String agentId = httpTransport.getAgentId(uri);
		String resource = httpTransport.getAgentResource(uri);
		
		// if no agentId is found, return generic information on servlet usage
		if (agentId == null || agentId.equals("")) {
			resp.getWriter().write(getServletDocs());
			resp.setContentType("text/plain");
			return;
		}
		
		// check if the agent exists
		try {
			if (!agentHost.hasAgent(agentId)) {
				resp.sendError(HttpServletResponse.SC_NOT_FOUND,
						"Agent with id '" + agentId + "' not found.");
				return;
			}
		} catch (Exception e) {
			throw new ServletException(e);
		}
		
		// If this is a handshake request, handle it.
		if (handleHandShake(req, resp)) {
			return;
		}
		
		try {
			if (JSONRPC.hasPrivate(agentHost.getAgent(agentId).getClass())
					&& !handleSession(req, resp)) {
				if (!resp.isCommitted()) {
					resp.sendError(HttpServletResponse.SC_UNAUTHORIZED);
				}
				return;
			}
		} catch (Exception e1) {
			LOG.log(Level.WARNING, "", e1);
		}
		// get the resource name from the end of the url
		if (resource == null || resource.equals("")) {
			if (!uri.endsWith("/") && !resp.isCommitted()) {
				String redirect = uri + "/";
				resp.sendRedirect(redirect);
				return;
			}
			resource = "index.html";
		}
		String extension = resource.substring(resource.lastIndexOf('.') + 1);
		
		if (resource.equals("events")) {
			// retrieve the agents logs
			String sinceStr = req.getParameter("since");
			Long since = null;
			if (sinceStr != null) {
				try {
					since = Long.valueOf(sinceStr);
				} catch (java.lang.NumberFormatException e) {
					LOG.warning("Couldn't parse 'since' parameter:'" + since
							+ "'");
				}
			}
			
			try {
				List<Log> logs = agentHost.getEventLogger().getLogs(agentId,
						since);
				resp.addHeader("Content-type", "application/json");
				JOM.getInstance().writer().writeValue(resp.getWriter(), logs);
			} catch (Exception e) {
				LOG.log(Level.WARNING, "", e);
				resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
						e.getMessage());
			}
		} else {
			// load the resource
			String mimetype = StreamingUtil.getMimeType(extension);
			String filename = RESOURCES + resource;
			InputStream is = this.getClass().getResourceAsStream(filename);
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
	 */
	@Override
	public void doPost(HttpServletRequest req, HttpServletResponse resp)
			throws IOException {
		JSONResponse jsonResponse = null;
		String body = null;
		String agentUrl = null;
		String agentId = null;
		try {
			// retrieve the agent url and the request body
			body = StringUtil.streamToString(req.getInputStream());
			
			agentUrl = req.getRequestURI();
			agentId = httpTransport.getAgentId(agentUrl);
			if (agentId == null || agentId.equals("")) {
				resp.sendError(HttpServletResponse.SC_BAD_REQUEST,
						"No agentId found in url.");
				return;
			}
			Agent agent = agentHost.getAgent(agentId);
			if (agent == null) {
				resp.sendError(HttpServletResponse.SC_BAD_REQUEST,
						"Agent not found at this host.");
				return;
			}
			
			if (JSONRPC.hasPrivate(agent.getClass())
					&& !handleSession(req, resp)) {
				if (!resp.isCommitted()) {
					resp.sendError(HttpServletResponse.SC_UNAUTHORIZED);
				}
				return;
			}
			
			// Attach the claimed senderId, or null if not given.
			String senderUrl = req.getHeader("X-Eve-SenderUrl");
			if (senderUrl == null || senderUrl.equals("")) {
				senderUrl = "web://" + req.getRemoteUser() + "@"
						+ req.getRemoteAddr();
			}
			String tag = new UUID().toString();
			SyncCallback<JSONResponse> callback = new SyncCallback<JSONResponse>();
			
			CallbackInterface callbacks = agentHost.getCallbackService("HttpTransport");
			callbacks.store(tag,callback);
			//TODO: on JSONResponse the callback will never be called and will timeout. What to do? These don't come in unless the other side is async i.s.o. tagged synced. 
			
			agentHost.receive(agentId, body,
					URI.create(senderUrl),tag);
			
			jsonResponse = callback.get();
		} catch (Exception err) {
			// generate JSON error response
			LOG.log(Level.WARNING, "", err);
			JSONRPCException jsonError = null;
			if (err instanceof JSONRPCException) {
				jsonError = (JSONRPCException) err;
			} else {
				jsonError = new JSONRPCException(
						JSONRPCException.CODE.INTERNAL_ERROR, err.getMessage());
				jsonError.setData(err);
			}
			jsonResponse = new JSONResponse(jsonError);
		}
		
		// return response
		resp.addHeader("Content-Type", "application/json");
		resp.getWriter().println(jsonResponse.toString());
		resp.getWriter().close();
	}
	
	/**
	 * Create a new agent Usage: PUT /servlet/{agentId}?type={agentType} Where
	 * agentType is the full class path of the agent. Returns a list with the
	 * urls of the created agent.
	 */
	@Override
	protected void doPut(HttpServletRequest req, HttpServletResponse resp)
			throws ServletException, IOException {
		String agentUrl = req.getRequestURI();
		String agentId = httpTransport.getAgentId(agentUrl);
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
			Agent agent = agentHost.createAgent(agentType, agentId);
			for (String url : agent.getUrls()) {
				resp.getWriter().println(url);
			}
			agent.signalAgent(new AgentSignal<Void>(AgentSignal.DESTROY, null));
		} catch (Exception e) {
			throw new ServletException(e);
		}
	}
	
	/**
	 * Delete an agent usage: DELETE /servlet/agentId
	 */
	@Override
	protected void doDelete(HttpServletRequest req, HttpServletResponse resp)
			throws ServletException, IOException {
		String agentUrl = req.getRequestURI();
		String agentId = httpTransport.getAgentId(agentUrl);
		
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
			agentHost.deleteAgent(agentId);
			resp.getWriter().write("Agent " + agentId + " deleted");
		} catch (Exception e) {
			throw new ServletException(e);
		}
	}
	
	/**
	 * Get a description on how to use this servlet
	 * 
	 * @return info
	 */
	protected String getServletDocs() {
		String servletUrl = httpTransport.getServletUrl();
		String info = "EVE AGENTS SERVLET\n" + "\n" + "Usage:\n" + "\n" +
		
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
