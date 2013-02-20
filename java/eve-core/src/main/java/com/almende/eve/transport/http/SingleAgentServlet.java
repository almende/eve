package com.almende.eve.transport.http;

import java.io.IOException;
import java.io.InputStream;
import java.util.logging.Logger;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.almende.eve.agent.Agent;
import com.almende.eve.agent.AgentFactory;
import com.almende.eve.agent.annotation.Sender;
import com.almende.eve.config.Config;
import com.almende.eve.rpc.RequestParams;
import com.almende.eve.rpc.jsonrpc.JSONRPCException;
import com.almende.eve.rpc.jsonrpc.JSONRequest;
import com.almende.eve.rpc.jsonrpc.JSONResponse;
import com.almende.util.StreamingUtil;
import com.almende.util.StringUtil;

@SuppressWarnings("serial")
public class SingleAgentServlet extends HttpServlet {
	private Logger logger = Logger.getLogger(this.getClass().getSimpleName());
	
	private AgentFactory agentFactory = null;
	private HttpService httpTransport = null;
	private String agentId = null;
	private static String RESOURCES = "/com/almende/eve/resources/";
	
	/**
	 * Initialize the agent factory and instantiate the agent on initialization
	 * of the servlet
	 */
	@Override
	public void init() {
		try {
			initAgentFactory();
			initHttpTransport();
			initAgent();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	@Override
	public void doGet(HttpServletRequest request,
			HttpServletResponse response) throws IOException {
		String servletUrl = httpTransport.getServletUrl();
		String uri = httpTransport.getDomain(servletUrl) + request.getRequestURI();
		String resource  = null;
		if (uri.length() > servletUrl.length()) {
			resource = uri.substring(servletUrl.length());
		}
		
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
			
			// TODO: append authorized sender url to the request parameters
			RequestParams requestParams = new RequestParams();
			requestParams.put(Sender.class, null);

			// invoke the agent
			jsonResponse = agentFactory.invoke(agentId, jsonRequest, requestParams);
		} catch (Exception err) {
			// generate JSON error response
			JSONRPCException jsonError = null;
			if (err instanceof JSONRPCException) {
				jsonError = (JSONRPCException) err;
			}
			else {
				jsonError = new JSONRPCException(
						JSONRPCException.CODE.INTERNAL_ERROR, err.getMessage());				
				jsonError.setData(err);
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
		
		// try to read servlet url from init parameter environment.<environment>.servlet_url
		String environment = agentFactory.getEnvironment();
		String envParam = "environment." + environment + ".servlet_url";
		String globalParam = "servlet_url";
		String servletUrl = getInitParameter(envParam);
		if (servletUrl == null) {
			// if no environment specific servlet_url is defined, read the global servlet_url
			servletUrl = getInitParameter(globalParam);
		}
		if (servletUrl == null) {
			throw new Exception("Cannot initialize HttpTransport: " +
					"Init Parameter '" + globalParam + "' or '" + envParam + "' " + 
					"missing in servlet configuration web.xml.");
		}
		
		httpTransport = new HttpService(servletUrl); 
		agentFactory.addTransportService(httpTransport);
	}
	
	/**
	 * Register this servlet at the agent factory
	 * @throws Exception 
	 */
	private void initAgent () throws Exception {
		// retrieve the agents id
		agentId = getInitParameter("agentId");
		if (agentId == null) {
			throw new Exception("Cannot initialize agent: " +
					"Init Parameter 'agentId' missing in servlet configuration web.xml.");
		}
		
		// create the agent if it does not yet exist
		Agent agent = agentFactory.getAgent(agentId);
		if (agent == null) {
			String agentType = getInitParameter("agentType");
			if (agentId == null) {
				throw new Exception("Cannot create agent: " +
						"Init Parameter 'agentClass' missing in servlet configuration web.xml.");
			}
			
			agent = agentFactory.createAgent(agentType, agentId);
			logger.info("Agent created: " + agent.toString());
		}

		logger.info("Agent initialized: " + agent.toString());
	}
}
