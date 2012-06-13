package com.almende.eve.servlet;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.almende.eve.agent.Agent;
import com.almende.eve.agent.log.LogAgent;
import com.almende.eve.config.Config;
import com.almende.eve.context.Context;
import com.almende.eve.context.ContextFactory;
import com.almende.eve.json.JSONRPC;
import com.almende.eve.json.JSONRPCException;
import com.almende.eve.json.JSONResponse;
import com.almende.eve.session.Session;
import com.almende.util.StreamingUtil;


@SuppressWarnings("serial")
public class MultiAgentServlet extends HttpServlet {
	private Logger logger = Logger.getLogger(this.getClass().getSimpleName());
	
	private Map<String, Class<?>> agentClasses = null;
	private ContextFactory contextFactory = null;
	private Config config = null;
	private static String RESOURCES = "/com/almende/eve/resources/";
	
	@Override
	public void init() {
		try {
			// initialize configuration file, context, and agents
			initConfig();
			initContext();
			initAgents();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	private class AgentAddress {
		public AgentAddress(String agentClass, String agentId, String agentResource) {
			this.agentClass = agentClass;
			this.agentId = agentId;
			this.agentResource = agentResource;
		}
		public String agentClass = null;
		public String agentId = null;
		public String agentResource = null;
	}
	
	/**
	 * Split the agentClass, agentId, and resource from a url
	 * For example:
	 * uri="/agents/GoogleCalendarAgent/b0e3af03-3265-4d63-8d4e-21f515070abb"
	 * @param url
	 * @return
	 * @throws ServletException
	 * @throws MalformedURLException
	 */
	private AgentAddress getAgentAddress (String url) 
			throws ServletException, MalformedURLException {
		String servletUrl = null;
		try {
			servletUrl = contextFactory.getServletUrl();
			if (!servletUrl.endsWith("/")) {
				servletUrl += "/";
			}
		} catch (Exception e) {
			e.printStackTrace();
		}

		String servletPath = new URL(servletUrl).getPath();
		if (!url.startsWith(servletPath)) {
			throw new ServletException("I don't get it. The request url '"  +
					url + "' does not match the configured servlet url '" + 
					servletPath + "'");
		}
		
		String path = url.substring(servletPath.length()); 
		String agentClass = null;
		String agentId = null;
		String agentResource = null;
		int slash1 = path.indexOf('/');
		if (slash1 != -1) {
			agentClass = path.substring(0, slash1);
			int slash2 = path.indexOf('/', slash1 + 1);
			if (slash2 != -1) {
				agentId = path.substring(slash1 + 1, slash2);
				agentResource = path.substring(slash2 + 1);
			}
			else {
				agentId = path.substring(slash1 + 1);
				agentResource = "";
			}
		}
		else {
			agentClass = "";
		}
		
		/* TODO: cleanup
		System.out.println("url=" + url);
		System.out.println("agentClass=" + agentClass);
		System.out.println("agentId=" + agentId);
		System.out.println("agentResource=" + agentResource);
		*/
		
		return new AgentAddress(agentClass, agentId, agentResource);
	}
	
	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp) 
			throws ServletException, IOException {
		String uri = req.getRequestURI();
		
		AgentAddress address = getAgentAddress(uri);
		String resource = address.agentResource;
		
		if (resource.isEmpty()) {
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
		String request = "";
		String response = "";
		try {
			// retrieve the request data
			request = streamToString(req.getInputStream());

			// a servlet path is built up in three parts: first the agents servlet
			// agents, then the name of the agent class, then the id of the 
			// agent. For example:
			// uri="/agents/GoogleCalendarAgent/b0e3af03-3265-4d63-8d4e-21f515070abb"
			AgentAddress address = getAgentAddress(req.getRequestURI());
			String classLowerCase = address.agentClass.toLowerCase();
			
			// check whether the agent class is known
			if (!agentClasses.containsKey(classLowerCase)) {
				throw new Exception("Unknown agent class " + address.agentClass);
			}
			
			// instantiate the agent
			Class<?> agentClass = agentClasses.get(classLowerCase);
			Agent agent = (Agent) agentClass.getConstructor().newInstance();
			
			// instantiate context of the agent
			String agentClassName = agentClass.getSimpleName().toLowerCase();
			Context context = contextFactory.getContext(agentClassName, address.agentId);
			agent.setContext(context);
			
			// instantiate session for the agent
			Session session = new Session(req, resp);
			agent.setSession(session);
			
			// execute the init method of the agent
			agent.init();
			
			// invoke the method onto the agent
			response = JSONRPC.invoke(agent, request);
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
		
		config = new Config(this);
	}
	
	/**
	 * Initialize the correct Agent class for the SingleAgentServlet.
	 * The class is read from the servlet init parameters in web.xml.
	 * @throws Exception 
	 */
	private void initAgents() throws Exception {
		if (agentClasses != null) {
			return;
		}

		Map<String, Class<?>> newAgentClasses = new HashMap<String, Class<?>>();
		
		List<String> classes = config.get("agent.classes");
		if (classes == null) {
			throw new ServletException(
				"Config parameter 'agent.classes' missing in Eve configuration.");
		}

		for (int i = 0; i < classes.size(); i++) {
			String className = classes.get(i);
			try {
				if (className != null && !className.isEmpty()) {
					Class<?> agentClass = Class.forName(className);

					if (!agentClass.getSuperclass().equals(Agent.class)) {
						throw new ServletException("Class " + agentClass.getName() + 
								" must extend " + Agent.class.getName());
					}
					
					// test if the class contains valid JSON-RPC messages
					List<String> errors = JSONRPC.validate(agentClass);
					for (String e : errors) {
						logger.warning(e);
					}
					
					String simpleName = agentClass.getSimpleName().toLowerCase();
					newAgentClasses.put(simpleName, agentClass);
					
					logger.info("Agent class " + agentClass.getName() + " loaded");
				}
			} 
			catch (ClassNotFoundException e) {
				logger.warning("Agent class " + className + " not found");
			}
			catch (Exception e) {
				logger.warning(e.getMessage());
			}
		}
		
		String simpleName = LogAgent.class.getSimpleName().toLowerCase();
		newAgentClasses.put(simpleName, LogAgent.class);
		logger.info("Agent class " + LogAgent.class.getName() + " loaded");
		
		// copy to agentClasses once the map is loaded
		agentClasses = newAgentClasses;
	}
	
	/**
	 * Initialize the correct Context class for the SingleAgentServlet.
	 * The class is read from the servlet init parameters in web.xml.
	 * @throws Exception
	 */
	private void initContext() throws Exception {
		if (contextFactory != null) {
			return;
		}
		
		String className = config.get("context.class");
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
		
		// copy the context as soon as it is done
		contextFactory = newContextFactory;
		
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
