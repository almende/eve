package com.almende.eve.transport.http;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.logging.Logger;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.almende.eve.agent.Agent;
import com.almende.eve.agent.AgentFactory;
import com.almende.eve.agent.log.Log;
import com.almende.eve.config.Config;
import com.almende.eve.rpc.jsonrpc.JSONRPCException;
import com.almende.eve.rpc.jsonrpc.JSONRequest;
import com.almende.eve.rpc.jsonrpc.JSONResponse;
import com.almende.eve.rpc.jsonrpc.jackson.JOM;
import com.almende.util.StreamingUtil;
import com.almende.util.StringUtil;

@SuppressWarnings("serial")
public class AgentServlet extends HttpServlet {
	private Logger logger = Logger.getLogger(this.getClass().getSimpleName());
	
	private static String RESOURCES = "/com/almende/eve/resources/";
	AgentFactory agentFactory = null;
	HttpService httpTransport = null;
	
	@Override
	public void init() {
		try {
			initAgentFactory();
			initHttpTransport();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * Get an agents web interface
	 * Usage: GET /servlet/{agentId}
	 */
	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp) 
			throws ServletException, IOException {
		String uri = req.getRequestURI();
		String agentId = httpTransport.getAgentId(uri);
		String resource = httpTransport.getAgentResource(uri);

		// if no agentId is found, return generic information on servlet usage
		if (agentId == null || agentId.isEmpty()) {
			resp.getWriter().write(getServletDocs());
			return;
		}
		
		// check if the agent exists
		try {
			if (!agentFactory.hasAgent(agentId)) {
				resp.sendError(404, "Agent with id '" + agentId + "' not found.");
				return;
			}
		} catch (Exception e) {
			throw new ServletException(e);
		}
		
		// get the resource name from the end of the url
		if (resource == null || resource.isEmpty()) {
			if (!uri.endsWith("/")) {
				String redirect = uri + "/";
				resp.sendRedirect(redirect);
				return;
			}
			else {
				resource = "index.html";
			}
		}
		String extension = resource.substring(resource.lastIndexOf(".") + 1);
		
		if (resource.equals("events")) {
			// retrieve the agents logs
			String sinceStr = req.getParameter("since");
			Long since = null;
			if (sinceStr != null) {
				since = Long.valueOf(sinceStr);
			}
			
			try {
				List<Log> logs = agentFactory.getEventLogger().getLogs(agentId, since);
				resp.addHeader("Content-type", "application/json");				
				JOM.getInstance().writer().writeValue(resp.getWriter(), logs);
			} catch (Exception e) {
				resp.sendError(500, e.getMessage());
			}
		}
		else {
			// load the resource
			String mimetype = StreamingUtil.getMimeType(extension);
			String filename = RESOURCES + resource;
			InputStream is = this.getClass().getResourceAsStream(filename);
			if (is != null) {
				StreamingUtil.streamBinaryData(is, mimetype, resp);
			}
			else {
				throw new ServletException("Resource '" + resource + "' not found");
			}
		}
	}

	/**
	 * Send a JSON-RPC message to an agent
	 * Usage: POST /servlet/{agentId}
	 *        With a JSON-RPC request as body.
	 *        Response will be a JSON-RPC response.
	 */
	@Override
	public void doPost(HttpServletRequest req, HttpServletResponse resp)
			throws IOException {
		JSONRequest jsonRequest = null;
		JSONResponse jsonResponse = null;
		String body = null;
		String agentUrl = null;
		String agentId = null;
		try {
			// retrieve the agent url and the request body
			body = StringUtil.streamToString(req.getInputStream());
			jsonRequest = new JSONRequest(body);
			
			// add the sender to the request parameters
			// TODO: retrieve authorized sender information
			jsonRequest.getParams().put("sender", (String) null);
			
			// invoke the agent
			agentUrl = req.getRequestURI();
			agentId = httpTransport.getAgentId(agentUrl);
			if (agentId == null || agentId.isEmpty()) {
				resp.sendError(400, "No agentId found in url.");
				return;
			}
			jsonResponse = agentFactory.invoke(agentId, jsonRequest);
		} catch (Exception err) {
			// generate JSON error response
			JSONRPCException jsonError = null;
			if (err instanceof JSONRPCException) {
				jsonError = (JSONRPCException) err;
			}
			else {
				jsonError = new JSONRPCException(
						JSONRPCException.CODE.INTERNAL_ERROR, err.getMessage());
				jsonError.setData(err.getStackTrace());
			}
			jsonResponse = new JSONResponse(jsonError);
		}

		// return response
		resp.addHeader("Content-Type", "application/json");
		resp.getWriter().println(jsonResponse.toString());
		resp.getWriter().close();
	}

	/**
	 * Create a new agent
	 * Usage: PUT /servlet/{agentId}?class={agentClass}
	 *        Where agentClass is the full class path of the agent.
	 *        Returns a list with the urls of the created agent.
	 */
	@Override
	protected void doPut(HttpServletRequest req, HttpServletResponse resp) 
			throws ServletException, IOException {
		String agentUrl = req.getRequestURI();
		String agentId = httpTransport.getAgentId(agentUrl);
		String agentClass = req.getParameter("class");
		
		if (agentId == null) {
			resp.sendError(400, "No agentId found in url.");
			return;
		}
		if (agentClass == null || agentClass.isEmpty()) {
			resp.sendError(400, "Query parameter 'class' missing in url.");
			return;
		}
		
		try {
			Agent agent = agentFactory.createAgent(agentClass, agentId);
			for (String url : agent.getUrls()) {
				resp.getWriter().println(url);
			}
			agent.destroy();
		} catch (Exception e) {
			throw new ServletException(e);
		}
	}

	/**
	 * Delete an agent
	 * usage: DELETE /servlet/agentId
	 */
	@Override
	protected void doDelete(HttpServletRequest req, HttpServletResponse resp) 
			throws ServletException, IOException {
		String agentUrl = req.getRequestURI();
		String agentId = httpTransport.getAgentId(agentUrl);

		if (agentId == null) {
			resp.sendError(400, "No agentId found in url.");
			return;
		}
		
		try {
			agentFactory.deleteAgent(agentId);
			resp.getWriter().write("Agent " + agentId + " deleted");
		} catch (Exception e) {
			throw new ServletException(e);
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
	 * Register this servlet at the agent factory
	 * @throws Exception 
	 */
	private void initHttpTransport () throws Exception {
		if (agentFactory == null) {
			throw new Exception(
					"Cannot initialize HttpTransport: no AgentFactory initialized.");
		}
		
		// TODO: one servlet must be able to support multiple servlet_urls
		
		// try to read servlet url from init parameter environment.<environment>.servlet_url
		String environment = agentFactory.getEnvironment();
		String envParam = "environment." + environment + ".servlet_url";
		String globalParam = "servlet_url";
		String servletUrl = getInitParameter(envParam);
		if (servletUrl == null) {
			// if no environment specific servlet_url is defined, read the global servlet_url
			servletUrl = getInitParameter("servlet_url");
		}
		if (servletUrl == null) {
			throw new Exception("Cannot initialize HttpTransport: " +
					"Init Parameter '" + globalParam + "' or '" + envParam + "' " + 
					"missing in servlet configuration web.xml.");
		}
		
		httpTransport = new HttpService(agentFactory); 
		httpTransport.init(servletUrl);
		agentFactory.addTransportService(httpTransport);
	}
	
	/**
	 * Get a description on how to use this servlet
	 * @return info
	 */
	private String getServletDocs() {
		String servletUrl = httpTransport.getServletUrl();
		String info = 
			"EVE AGENTS SERVLET\n" +
			"\n" +
			"Usage:\n" +
			"\n" +
			
			"GET " + servletUrl + "\n" +
			"\n" +
			"    Returns information on how to use this servlet.\n" +
			"\n" +
			
			"GET " + servletUrl + "{agentId}\n" +
			"\n" +
			"    Returns an agents web interface, allowing for easy interaction\n" +
			"    with the agent.\n" +
			"    A 404 error will be returned when the agent does not exist.\n" +
			"\n" +
			
			"POST " + servletUrl + "{agentId}\n" +
			"\n" +
			"    Send an RPC call to an agent.\n" +
			"    The body of the request must contain a JSON-RPC request.\n" +
			"    The addressed agent will execute the request and return a\n" +
			"    JSON-RPC response. This response can contain the result or\n" +
			"    an exception.\n" +
			"    A 404 error will be returned when the agent does not exist.\n" +
			"\n" +
			
			"PUT " + servletUrl + "{agentId}?class={agentClass}\n" +
			"\n" +
			"    Create an agent. agentId can be any string. agentClass must\n" +
			"    be a full java class path of an Agent. A 500 error will be\n" +
			"    thrown when an agent with this id already exists.\n" +
			"\n" +

			"DELETE " + servletUrl + "{agentId}\n" +
			"\n" +
			"    Delete an agent by its id.";

		return info;
	}
}
