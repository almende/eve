package com.almende.eve.service.http;

import java.io.IOException;
import java.io.InputStream;
import java.util.logging.Logger;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.almende.eve.agent.AgentFactory;
import com.almende.eve.config.Config;
import com.almende.eve.json.JSONRPCException;
import com.almende.eve.json.JSONRequest;
import com.almende.eve.json.JSONResponse;
import com.almende.util.StreamingUtil;
import com.almende.util.StringUtil;

@SuppressWarnings("serial")
public class MultiAgentServlet extends HttpServlet {
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
	
	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp) 
			throws ServletException, IOException {
		String uri = req.getRequestURI();
		String agentId = httpService.getAgentId(uri);
		String resource = httpService.getAgentResource(uri);
		
		// check if the agent exists
		try {
			if (agentId == null || agentId.isEmpty() || !agentFactory.hasAgent(agentId)) {
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
			if (agentId == null) {
				throw new Exception("Agent with id '" + agentId + "' not found.");
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
}
