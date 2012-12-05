package com.almende.eve.service.http;

import java.io.IOException;
import java.io.InputStream;
import java.util.logging.Logger;

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

// FIXME: SingleAgentServlet does not yet work correctly
@SuppressWarnings("serial")
public class SingleAgentServlet extends HttpServlet {
	private Logger logger = Logger.getLogger(this.getClass().getSimpleName());
	
	private AgentFactory agentFactory = null;
	private HttpService httpService = null;
	private String agentId = "1"; // TODO: what to do with id?
	private static String RESOURCES = "/com/almende/eve/resources/";
	
	/**
	 * Initialize the agent factory and instantiate the agent on initialization
	 * of the servlet
	 */
	@Override
	public void init() {
		try {
			initAgentFactory();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	@Override
	public void doGet(HttpServletRequest request,
			HttpServletResponse response) throws IOException {
		String servletUrl = httpService.getServletUrl();
		String uri = httpService.getDomain(servletUrl) + request.getRequestURI();
		String resource = uri.substring(servletUrl.length());
		
		if (resource == null || resource.isEmpty()) {
			if (!uri.endsWith("/")) {
				String redirect = uri + "/";
				response.sendRedirect(redirect);
				return;
			}
			else {
				resource = "index.html";
			}
		}
		String extension = resource.substring(resource.lastIndexOf(".") + 1);
		
		// retrieve and stream the resource
		String mimetype = StreamingUtil.getMimeType(extension);
		String filename = RESOURCES + resource;
		InputStream is = this.getClass().getResourceAsStream(filename);
		StreamingUtil.streamBinaryData(is, mimetype, response);
	}	

	@Override
	public void doPost(HttpServletRequest req, HttpServletResponse resp)
			throws IOException {
		JSONRequest jsonRequest = null;
		JSONResponse jsonResponse = null;		
		try {
			// retrieve the request body
			String body = StringUtil.streamToString(req.getInputStream());
			jsonRequest = new JSONRequest(body);
			
			// invoke the agent
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
		
		/* TODO: use a shared agent factory?
		// TODO: be able to choose a different namespace 
		agentFactory = AgentFactory.getInstance();
		agentFactory.setConfig(config);
		*/
		agentFactory = new AgentFactory(config);

		httpService = (HttpService) agentFactory.getService("http");
	}
}
