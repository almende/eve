package com.almende.eve.servlet;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
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
		Map<String, String> params = agentFactory.getAgentParams(uri);
		String agentClass = params.get("class");
		String resource = params.get("resource");
		
		// check if the agent class is known
		if (agentFactory.getAgentClass(agentClass) == null) {
			throw new ServletException(
					"Unknown agent class '" + agentClass + "'");
		}
		
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
		try {
			// retrieve the agent url and the request body
			String body = StringUtil.streamToString(req.getInputStream());
			String agentUrl = req.getRequestURI();
			jsonRequest = new JSONRequest(body);
			
			// invoke the agent
			jsonResponse = agentFactory.invoke(agentUrl, jsonRequest);
		} catch (Exception err) {
			// generate JSON error response
			JSONRPCException jsonError = new JSONRPCException(
						JSONRPCException.CODE.INTERNAL_ERROR, err.getMessage());
			jsonResponse = new JSONResponse(jsonError);
			
			err.printStackTrace(); // TODO: remove printing stacktrace?
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
		agentFactory = new AgentFactory(config);	
	}
}
