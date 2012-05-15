package com.almende.eve.servlet;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.yaml.snakeyaml.Yaml;

import com.almende.eve.agent.Agent;
import com.almende.eve.agent.log.LogAgent;
import com.almende.eve.context.AgentContext;
import com.almende.eve.context.MemoryContext;
import com.almende.eve.json.JSONRPC;
import com.almende.eve.json.JSONRPCException;
import com.almende.eve.json.JSONResponse;



@SuppressWarnings("serial")
public class MultiAgentServlet extends HttpServlet {
	private Logger logger = Logger.getLogger(this.getClass().getSimpleName());
	
	private Map<String, Class<?>> agentClasses = null;
	private AgentContext contextFactory = null;
	private Map<String, Object> config = null; // servlet configuration 

	@Override
	protected void doGet(HttpServletRequest request,
			HttpServletResponse response) throws ServletException, IOException {
		response.setContentType("text/html");
		String filename = "/com/almende/eve/resources/agent.html";
		InputStream is = this.getClass().getResourceAsStream(filename);
		if (is != null) {
			InputStreamReader isr = new InputStreamReader(is);
			BufferedReader reader = new BufferedReader(isr);
			PrintWriter pw = response.getWriter();

			String text;
			while ((text = reader.readLine()) != null) {
				pw.println(text);
			}
		}
	}

	@Override
	public void doPost(HttpServletRequest req, HttpServletResponse resp)
			throws IOException {
		String request = "";
		String response = "";
		try {
			// initialize the context and agent
			initContext(req);
			initAgents(req);
			
			// retrieve the request data
			request = streamToString(req.getInputStream());

			// initialize response object
			// a servlet path is built up in three parts: first the folder 
			// agents, then the name of the agent class, then the id of the 
			// agent. For example:
			// uri="/agents/GoogleCalendarAgent/b0e3af03-3265-4d63-8d4e-21f515070abb"
			String uri = req.getRequestURI();
			String[] path = uri.split("\\/");
			String simpleName = (path.length > 2) ? path[path.length - 2] : "";
			String id = (path.length > 1) ? path[path.length - 1] : ".";
			simpleName = simpleName.toLowerCase();

			// check whether the agent class is known
			if (!agentClasses.containsKey(simpleName)) {
				throw new Exception("Unknown agent class " + simpleName);
			}
			
			// instantiate the agent
			Class<?> agentClass = agentClasses.get(simpleName);
			Agent agent = (Agent) agentClass.getConstructor().newInstance();

			// instantiate context of the agent
			String agentClassName = agentClass.getSimpleName().toLowerCase();
			AgentContext context = contextFactory.getInstance(agentClassName, id);
			agent.setContext(context);
			
			// invoke the method onto the agent
			response = JSONRPC.invoke(agent, request);
		} catch (Exception err) {
			// generate JSON error response
			JSONRPCException jsonError = new JSONRPCException(
					JSONRPCException.CODE.INTERNAL_ERROR, err.getMessage());
			JSONResponse jsonResponse = new JSONResponse(jsonError);
			response = jsonResponse.toString();
		}

		// TODO: cleanup? or use it?
		// logger.info("url=" + req.getRequestURI() + ", request=" + request + ", response=" + response);

		// return response
		resp.addHeader("Content-Type", "application/json");
		resp.getWriter().println(response);
	}

	/**
	 * Initialize the correct Agent class for the SingleAgentServlet.
	 * The class is read from the servlet init parameters in web.xml.
	 * @throws ServletException
	 */
	private void initAgents(HttpServletRequest req) throws ServletException {
		if (agentClasses != null) {
			return;
		}

		Map<String, Class<?>> newAgentClasses = new HashMap<String, Class<?>>();
		
		List<String> classes = getConfigParameter("agents");
		if (classes == null) {
			throw new ServletException(
				"Config parameter 'agents' missing in servlet configuration." +
				"This parameter must be specified in web.xml.");
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
	private void initContext(HttpServletRequest req) throws Exception {
		if (contextFactory != null) {
			return;
		}

		String className = getConfigParameter("context");
		
		if (className == null || className.isEmpty()) {
			className = MemoryContext.class.getName();
			// TODO: isn't it better to just return null?
		}
		
		Class<?> contextClass = null;
		try {
			contextClass = Class.forName(className);
		} catch (ClassNotFoundException e) {
			throw new ServletException("Cannot find class " + className + "");
		}
		
		if (!hasInterface(contextClass, AgentContext.class)) {
			throw new ServletException(
					"Context class " + contextClass.getName() + 
					" must implement interface " + AgentContext.class.getName());
		}

		// FIXME: it is not safe retrieving the servlet url from the request!
		AgentContext newContextFactory = 
			(AgentContext) contextClass.getConstructor().newInstance();
		newContextFactory.setServletUrl(req);

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
	 * Retrieve the configuration file
	 * @return
	 * @throws ServletException 
	 */
	@SuppressWarnings("unchecked")
	private Map<String, Object> getConfig() throws ServletException {
		if (config == null) {
			String file = getInitParameter("config");
			if (file == null) {
				throw new ServletException(
					"Init parameter 'config' missing in servlet configuration." +
					"This parameter must be specified in web.xml.");
			}
			Yaml yaml = new Yaml();
			config = (Map<String, Object>) yaml.load(file);
		}
		
		return config;
	}
	
	/**
	 * retrieve a config parameter from the configuration file
	 * @param param    Parameter name
	 * @return
	 * @throws ServletException
	 */
	@SuppressWarnings("unchecked")
	private <T> T getConfigParameter(String param) throws ServletException {
		Map<String, Object> config = getConfig();
		return (T) config.get(param);
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
