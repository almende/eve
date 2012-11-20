package com.almende.eve.servlet;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.List;
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

// FIXME: the agents getUrl does not work well in SingleAgentServlet, and
//         also LogAgent does not work
@SuppressWarnings("serial")
public class SingleAgentServlet extends HttpServlet {
	private Logger logger = Logger.getLogger(this.getClass().getSimpleName());
	
	private AgentFactory agentFactory = null;
	private String agentClass = null;
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
			initAgentClass();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	@Override
	public void doGet(HttpServletRequest request,
			HttpServletResponse response) throws ServletException, IOException {
		String url = request.getRequestURI();
		
		// retrieve the servlet url from the context
		String servletUrl = null;
		try {
			servletUrl = agentFactory.getServletUrl();
			if (!servletUrl.endsWith("/")) {
				servletUrl += "/";
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		// verify the path
		String servletPath = new URL(servletUrl).getPath();
		if (!url.startsWith(servletPath)) {
			throw new ServletException("I don't get it. The request url '"  +
					url + "' does not match the configured servlet url '" + 
					servletPath + "'");
		}

		// extract the resource name from the url
		String name = url.substring(servletPath.length());
		if (name.startsWith("/")) {
			name = name.substring(1);
		}
		String extension = name.substring(name.lastIndexOf(".") + 1);
		if (extension.equals(name)) {
			name = "index.html";
			extension = name.substring(name.lastIndexOf(".") + 1);
		}
		
		// retrieve and stream the resource
		String mimetype = StreamingUtil.getMimeType(extension);
		String filename = RESOURCES + name;
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
			jsonResponse = agentFactory.invoke(agentClass, agentId, jsonRequest);
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
	
	/**
	 * Initialize the agent class
	 * @throws IllegalArgumentException
	 */
	private void initAgentClass() throws IllegalArgumentException {
		Config config = agentFactory.getConfig();
		//List<String> classes = config.get("agents");
		
		String className = null;
		List<Map<String, Object>> agents = config.get("agents");
		if (agents != null) {
			if (agents.size() == 0) {
				throw new IllegalArgumentException(
					"Config parameter 'agents[]' is empty in Eve configuration.");
			}
			if (agents.size() > 1) {
				throw new IllegalArgumentException(
						"Config parameter 'agents[]' may only contain one class");
			}
			
			Map<String, Object> properties = agents.get(0);
			if (!properties.containsKey("class")) {
				throw new IllegalArgumentException(
						"Config parameter 'agents[0].class' missing in Eve configuration");
			}
			className = (String) properties.get("class");
		}
		else {
			List<String> classes = config.get("agent", "classes");
			if (classes != null) {
				logger.warning("Property agent.classes[] is deprecated. Use agents[].class instead");
				
				if (classes.size() == 0) {
					throw new IllegalArgumentException(
							"Config parameter 'agent.classes' is empty in Eve configuration.");
				}
				else if (classes.size() > 1) {
					throw new IllegalArgumentException(
							"Config parameter 'agent.classes' may only contain one class");
				}
				
				className = classes.get(0);
			}
			else {
				throw new IllegalArgumentException(
					"Config parameter 'agents[]' missing in Eve configuration.");
			}
		}
		
		Class<?> clazz = null;
		try {
			clazz = Class.forName(className);
			agentClass = clazz.getSimpleName().toLowerCase();
		} catch (ClassNotFoundException e) {
			logger.warning("Agent class '" + className + "' not found");
		}
	}
}
