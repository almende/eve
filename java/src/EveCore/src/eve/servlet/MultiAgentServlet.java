package eve.servlet;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import eve.agent.Agent;
import eve.agent.context.AgentContext;
import eve.agent.context.SimpleContext;
import eve.json.JSONRPC;
import eve.json.JSONRPCException;
import eve.json.JSONResponse;


@SuppressWarnings("serial")
public class MultiAgentServlet extends HttpServlet {
	private Logger logger = Logger.getLogger(this.getClass().getSimpleName());
	
	private Map<String, Class<?>> agentClasses = null;
	private Class<?> contextClass = null;

	@Override
	public void doGet(HttpServletRequest req, HttpServletResponse resp)
			throws IOException {
		// TODO: handle get requests by serving a nice HTML page
		resp.getWriter().println(
				"Error: POST request containing a JSON-RPC message expected");
	}

	@Override
	public void doPost(HttpServletRequest req, HttpServletResponse resp)
			throws IOException {
		String response = "";
		try {
			// initialize the context and agent
			initContext(req);
			initAgents(req);
			
			// retrieve the request data
			String request = streamToString(req.getInputStream());
			
			// initialize response object
			// a servlet path is built up in three parts: first the folder 
			// agents, then the name of the agent class, then the id of the 
			// agent. For example:
			// uri="/agents/GoogleCalendarAgent/b0e3af03-3265-4d63-8d4e-21f515070abb"
			String uri = req.getRequestURI();
			String[] path = uri.split("\\/");
			String simpleName = (path.length > 2) ? path[path.length - 2] : "";
			String id = (path.length > 1) ? path[path.length - 1] : null;
			simpleName = simpleName.toLowerCase();
			
			// get the correct agent instance
			if (!agentClasses.containsKey(simpleName)) {
				throw new Exception("Unknown agent class " + simpleName);
			}
			
			// instantiate the agent and its context
			// TODO: load context depending on id
			Class<?> agentClass = agentClasses.get(simpleName);
			Agent agent = (Agent) agentClass.getConstructor().newInstance();
			AgentContext context = (AgentContext) contextClass.getConstructor().newInstance();
			context.setServletUrlFromRequest(req);
			context.setAgentClass(agentClass.getSimpleName().toLowerCase());
			context.setId(id);
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

		// return response
		resp.addHeader("Content-Type", "application/json");
		resp.getWriter().println(response);
	}

	/**
	 * Initialize the correct Agent class for the SingleAgentServlet.
	 * The class is read from the servlet init parameters in web.xml.
	 * @throws ServletException
	 */
	private void initAgents(HttpServletRequest req) throws Exception {
		if (agentClasses != null) {
			return;
		}

		agentClasses = new HashMap<String, Class<?>>();
		
		String classNames = getInitParameter("agents");
		
		if (classNames == null || classNames.isEmpty()) {
			throw new ServletException(
				"Init parameter 'agents' missing in servlet configuration." +
				"This parameter must be specified in web.xml.");
		}
		
		String[] classes = classNames.split(";");

		for (int i = 0; i < classes.length; i++) {
			try {
				String className = classes[i].trim();
				if (className != null && !className.isEmpty()) {
					Class<?> agentClass = Class.forName(className);
					
					if (!agentClass.getSuperclass().equals(Agent.class)) {
						throw new ServletException("Class " + agentClass.getName() + 
								" must extend " + Agent.class.getName());
					}
					
					String simpleName = agentClass.getSimpleName().toLowerCase();
					agentClasses.put(simpleName, agentClass);
					
					logger.info("Agent class " + agentClass.getName() + " loaded");
				}
			} 
			catch (Exception e) {
				logger.warning(e.getMessage()); 		
			}
		}
	}
	/**
	 * Initialize the correct Context class for the SingleAgentServlet.
	 * The class is read from the servlet init parameters in web.xml.
	 * @throws ServletException
	 */
	private void initContext(HttpServletRequest req) throws Exception {
		if (contextClass != null) {
			return;
		}

		String className = getInitParameter("context");
		
		if (className == null || className.isEmpty()) {
			className = SimpleContext.class.getName();
		}
		
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
	
	private static String streamToString(InputStream in) throws IOException {
		StringBuffer out = new StringBuffer();
		byte[] b = new byte[4096];
		for (int n; (n = in.read(b)) != -1;) {
			out.append(new String(b, 0, n));
		}
		return out.toString();
	}

}
