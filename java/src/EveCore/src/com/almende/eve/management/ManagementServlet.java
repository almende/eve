package com.almende.eve.management;

import java.io.IOException;
import java.util.logging.Logger;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.almende.eve.agent.Agent;
import com.almende.eve.agent.AgentFactory;
import com.almende.eve.config.Config;

@SuppressWarnings("serial")
public class ManagementServlet extends HttpServlet {
	@Override
	public void init () {
		try {
			initAgentFactory();	
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * Get an agent by its id.
	 * Will return the urls of the agent if existing.
	 * usage: GET /servlet/agentId 
	 */
	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp) 
			throws ServletException, IOException {		
		String agentId = getAgentId(req);

		if (agentId == null) {
			resp.getWriter().print(getUsageDescription());
			return;
		}
		
		try {
			Agent agent = agentFactory.getAgent(agentId);
			if (agent != null) {
				resp.getWriter().print(getInfo(agent));	
				agent.destroy();
			}
			else {
				resp.sendError(404, "Agent with id '" + agentId + "' not found");
			}
		} catch (Exception e) {
			throw new ServletException(e);
		}
	}
	
	/**
	 * Create a new agent
	 * usage: PUT /servlet/agentId?class=com.almende.eve.agent.example.EchoAgent
	 */
	@Override
	protected void doPut(HttpServletRequest req, HttpServletResponse resp) 
			throws ServletException, IOException {
		String agentId = getAgentId(req);
		String agentClass = req.getParameter("class");
		
		if (agentId == null) {
			resp.sendError(400, "No agentId found in uri.");
			return;
		}
		if (agentClass == null || agentClass.isEmpty()) {
			resp.sendError(400, "Missing query parameter 'class'.");
			return;
		}
		
		try {
			Agent agent = agentFactory.createAgent(agentClass, agentId);
			resp.getWriter().print(getInfo(agent));
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
		String agentId = getAgentId(req);
		if (agentId == null) {
			resp.sendError(400, "No agentId found in uri.");
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
	 * Get a description on how to use the AgentManagementServlet
	 * @return info
	 */
	private String getUsageDescription() {
		String info = 
				"AGENT MANAGEMENT SERVLET\n" +
				"\n" +
				"Usage:\n" +
				"\n" +
				"GET /management/{agentId}\n" +
				"    Get general information about an agent: id, class, urls.\n" +
				"\n" +
				"PUT /management/{agentId}?class={agentClass}\n" +
				"    Create a new agent with given class.\n" +
				"    The class must be a full java class path.\n" +
				"\n" +
				"DELETE /management/{agentId}\n" +
				"    Delete an agent by its id.";

		return info.toString();
	}
	
	/**
	 * Get information on an agents id, class, and urls
	 * @param agent
	 * @return info
	 */
	private String getInfo(Agent agent) {
		StringBuilder info = new StringBuilder();
		info.append("ID\n" + agent.getId() + "\n\n");
		info.append("CLASS\n" + agent.getClass().getName() + "\n\n");
		info.append("URLS\n");
		info.append(getUrls(agent));

		return info.toString();
	}
	
	/**
	 * Stringify the list with urls of the agent
	 * @param agent
	 * @return
	 */
	private String getUrls(Agent agent) {
		StringBuilder urls = new StringBuilder();
		for (String url : agent.getUrls()) {
			urls.append(url);
			urls.append("\n");
		}
		return urls.toString();
	}
	
	/**
	 * Extract the agentId from a servlet request. Returns null if no id is
	 * found in the request url.
	 * @param req
	 * @return agentId
	 */
	private String getAgentId(HttpServletRequest req) {
		String uri = req != null ? req.getRequestURI() : null;
		if (uri != null) {
			// remove context path
			String contextPath = req.getContextPath();
			if (contextPath != null && uri.startsWith(contextPath)) {
				uri = uri.substring(contextPath.length());
			}

			// remove servletPath
			String servletPath = req.getServletPath();
			if (servletPath != null && uri.startsWith(servletPath)) {
				uri = uri.substring(servletPath.length());
			}

			String[] path = uri.split("/");
			String agentId = path.length > 0 ? path[path.length - 1] : null;
			if (agentId != null && !agentId.isEmpty()) {
				return agentId;
			}			
		}
		return null;
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

	AgentFactory agentFactory = null;
	private Logger logger = Logger.getLogger(this.getClass().getSimpleName());
}
