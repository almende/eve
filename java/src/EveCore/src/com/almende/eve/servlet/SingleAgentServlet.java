package com.almende.eve.servlet;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.List;
import java.util.logging.Logger;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.almende.eve.agent.Agent;
import com.almende.eve.config.Config;
import com.almende.eve.context.Context;
import com.almende.eve.context.ContextFactory;
import com.almende.eve.json.JSONRPC;
import com.almende.eve.json.JSONRPCException;
import com.almende.eve.json.JSONResponse;
import com.almende.util.StreamingUtil;


@SuppressWarnings("serial")
public class SingleAgentServlet extends HttpServlet {
	private Logger logger = Logger.getLogger(this.getClass().getSimpleName());
	
	private Class<?> agentClass = null;
	private ContextFactory contextFactory = null;
	private Config config = null; // servlet configuration 
	private static String RESOURCES = "/com/almende/eve/resources/";
	
	@Override
	public void init() {
		try {
			// initialize configuration file, context, and agents
			initConfig();
			initContext();
			initAgent();
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
			servletUrl = contextFactory.getServletUrl();
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
		String response = "";
		try {
			// retrieve the request data
			String request = streamToString(req.getInputStream());

			// instantiate an agent
			Agent agent = (Agent) agentClass.getConstructor().newInstance();

			// instantiate and initialize context
			// FIXME: setting class and id results in a wrong getUrl() of the agent
			// FIXME: getUrl() of the agent does not work
			String agentClassName = agent.getClass().getSimpleName().toLowerCase();
			String id = "1"; // TODO: what to do with id?
			Context context = contextFactory.getContext(agentClassName, id);
			try {
				context.init();
				agent.setContext(context);			
				
				// TODO: instantiate session?
				
				// invoke the method onto the agent
				response = JSONRPC.invoke(agent, request);
			}
			finally {
				// destroy context (this will typically persist the context)
				context.destroy();
			}
		} catch (Exception err) {
			// generate JSON error response
			JSONRPCException jsonError = new JSONRPCException(
					JSONRPCException.CODE.INTERNAL_ERROR, err.getMessage());
			JSONResponse jsonResponse = new JSONResponse(jsonError);
			response = jsonResponse.toString();
			
			err.printStackTrace(); // TODO: remove printing stacktrace?
		}

		// return response
		resp.addHeader("Content-Type", "application/json");
		resp.getWriter().println(response);
	}

	/**
	 * Load configuration file
	 * @throws IOException 
	 * @throws ServletException 
	 * @throws Exception 
	 */
	private void initConfig() throws ServletException, IOException {
		if (config != null) {
			return;
		}

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
		config = new Config(getServletContext().getResourceAsStream(fullname));
	}
	
		
	/**
	 * Initialize the correct Agent class for the SingleAgentServlet.
	 * The class is read from the servlet init parameters in web.xml.
	 * @throws ServletException
	 */
	private void initAgent() throws Exception {
		if (agentClass != null) {
			return;
		}

		List<String> classNames = config.get("agent", "classes");		
		if (classNames == null || classNames.size() == 0) {
			throw new ServletException(
				"Config parameter 'agent.classes' missing in Eve configuration.");
		}
		if (classNames.size() > 1) {
			throw new ServletException(
					"Config parameter 'agent.classes' may only contain one class");
		}
		String className = classNames.get(0);
		
		Class<?> newAgentClass;
		try {
			newAgentClass = Class.forName(className);
		} catch (ClassNotFoundException e) {
			throw new ServletException("Cannot find class " + className + "");
		}
		
		if (!newAgentClass.getSuperclass().equals(Agent.class)) {
			throw new ServletException("Class " + newAgentClass.getName() + 
					" must extend " + Agent.class.getName());
		}

		// test if the class contains valid JSON-RPC messages
		List<String> errors = JSONRPC.validate(agentClass);
		for (String e : errors) {
			logger.warning(e);
		}
		
		// copy to the final agentClass once loaded
		agentClass = newAgentClass;
		
		logger.info("Agent class " + agentClass.getName() + " loaded");
	}

	/**
	 * Initialize the correct Context class for the SingleAgentServlet.
	 * The class is read from the servlet init parameters in web.xml.
	 * @throws ServletException
	 */
	private void initContext() throws Exception {
		if (contextFactory != null) {
			return;
		}

		String className = config.get("context", "class");
		if (className == null) {
			throw new ServletException(
				"Config parameter 'context.class' missing in Eve configuration.");
		}
		
		Class<?> contextClass = null;
		try {
			contextClass = Class.forName(className);
		} catch (ClassNotFoundException e) {
			throw new ServletException("Cannot find class " + className + "");
		}
		
		if (!hasInterface(contextClass, ContextFactory.class)) {
			throw new ServletException(
					"Context class " + contextClass.getName() + 
					" must implement interface " + ContextFactory.class.getName());
		}

		ContextFactory newContextFactory = 
			(ContextFactory) contextClass.getConstructor().newInstance();
		newContextFactory.setConfig(config);
		
		// copy to the final contextFactory once loaded
		contextFactory = newContextFactory;
		
		// FIXME: it is not safe retrieving the servlet url from the request!
		logger.info("Context class " + contextClass.getName() + " loaded");
	}
	
	/**
	 * Check if checkClass has implemented interfaceClass
	 * @param checkClass
	 * @param interfaceClass
	 */
	private boolean hasInterface(Class<?> checkClass, Class<?> interfaceClass) {
		Class<?>[] interfaces = checkClass.getInterfaces();
		
		for (Class<?> i : interfaces) {
			if (i.equals(interfaceClass)) {
				return true;
			}
		}
		
		return false;
	}
	
	/**
	 * Convert a stream to a string
	 * @param in
	 * @return
	 * @throws IOException
	 */
	private static String streamToString(InputStream in) throws IOException {
		StringBuffer out = new StringBuffer();
		byte[] b = new byte[4096];
		for (int n; (n = in.read(b)) != -1;) {
			out.append(new String(b, 0, n));
		}
		return out.toString();
	}
}
