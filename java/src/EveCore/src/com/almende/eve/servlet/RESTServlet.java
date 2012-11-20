package com.almende.eve.servlet;

import java.io.IOException;
import java.util.Enumeration;
import java.util.logging.Logger;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.almende.eve.agent.Agent;
import com.almende.eve.agent.AgentFactory;
import com.almende.eve.config.Config;
import com.almende.eve.json.JSONRequest;
import com.almende.eve.json.JSONResponse;


@SuppressWarnings("serial")
public class RESTServlet extends HttpServlet {
	private Logger logger = Logger.getLogger(this.getClass().getSimpleName());
	private AgentFactory factory = null;
	
	@Override
	public void init() {
		try {
			initAgentFactory();	
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	@Override
	public void doGet(HttpServletRequest req, HttpServletResponse resp)
			throws IOException {
		try {
			// get method from url
			String uri = req.getRequestURI();
			String[] path = uri.split("\\/");
			String agentClass = (path.length > 3) ? path[path.length - 3] : null;
			String agentId = (path.length > 2) ? path[path.length - 2] : null;
			String method = (path.length > 1) ? path[path.length - 1] : null;

			// get query parameters
			JSONRequest request = new JSONRequest();
			request.setMethod(method);
			Enumeration<String> params = req.getParameterNames();
			while (params.hasMoreElements()) {
				String param = params.nextElement();
				request.putParam(param, req.getParameter(param));
			}

			// instantiate and invoke the agent
			Agent agent = factory.getAgent(agentClass, agentId);
			JSONResponse response = factory.invoke(agent, request);
			agent.destroy();
			
			// return response
			resp.addHeader("Content-Type", "application/json");
			resp.getWriter().println(response.getResult());
		} catch (Exception err) {
			resp.getWriter().println(err.getMessage());
		}
	}

	@Override
	public void doPost(HttpServletRequest req, HttpServletResponse resp)
			throws IOException {
		doGet(req, resp);
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
		factory = new AgentFactory(config);	
	}
}
