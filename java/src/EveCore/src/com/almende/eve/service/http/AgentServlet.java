package com.almende.eve.service.http;

import java.io.IOException;
import java.io.InputStream;
import java.util.logging.Logger;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.almende.eve.agent.Agent;
import com.almende.eve.agent.AgentFactory;
import com.almende.eve.config.Config;
import com.almende.eve.json.JSONRPCException;
import com.almende.eve.json.JSONRequest;
import com.almende.eve.json.JSONResponse;
import com.almende.util.StreamingUtil;
import com.almende.util.StringUtil;

@SuppressWarnings("serial")
public class AgentServlet extends HttpServlet {
	private Logger logger = Logger.getLogger(this.getClass().getSimpleName());
	
	private static String RESOURCES = "/com/almende/eve/resources/";
	AgentFactory agentFactory = null;
	HttpService httpService = null;
	
	@Override
	public void init() {
		try {
			initAgentFactory();	
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
		String agentId = httpService.getAgentId(uri);
		String resource = httpService.getAgentResource(uri);

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
			
			// invoke the agent
			agentUrl = req.getRequestURI();
			agentId = httpService.getAgentId(agentUrl);
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
				err.printStackTrace(); // TODO: remove printing stacktrace?
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
		String agentId = httpService.getAgentId(agentUrl);
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
		String agentId = httpService.getAgentId(agentUrl);

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
		
		// TODO: this will not work with multiple http servlets
		httpService = (HttpService) agentFactory.getService("http");
	}
	
	
	/**
	 * Get a description on how to use this servlet
	 * @return info
	 */
	private String getServletDocs() {
		String servletUrl = httpService.getServletUrl();
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
