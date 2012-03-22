package com.almende.eve.servlet;

import java.io.IOException;
import java.io.InputStream;
import java.util.logging.Logger;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.almende.eve.agent.Agent;
import com.almende.eve.agent.context.AgentContext;
import com.almende.eve.agent.context.MemoryContext;
import com.almende.eve.json.JSONRPC;
import com.almende.eve.json.JSONRPCException;
import com.almende.eve.json.JSONResponse;



@SuppressWarnings("serial")
public class SingleAgentServlet extends HttpServlet {
	private Logger logger = Logger.getLogger(this.getClass().getSimpleName());
	
	private Class<?> agentClass = null;
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
			// initialize the context, and agent
			initContext(req);
			initAgent(req);
			
			// retrieve the request data
			String request = streamToString(req.getInputStream());

			// instantiate an agent and set its context
			Agent agent = (Agent) agentClass.getConstructor().newInstance();
			AgentContext context = 
				(AgentContext) contextClass.getConstructor().newInstance();
			context.setServletUrlFromRequest(req);			
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
	private void initAgent(HttpServletRequest req) throws Exception {
		if (agentClass != null) {
			return;
		}

		String className = getInitParameter("agent");
		
		if (className == null || className.isEmpty()) {
			throw new ServletException(
				"Init parameter 'agent' missing in servlet configuration." +
				"This parameter must be specified in web.xml.");
		}
		
		try {
			agentClass = Class.forName(className);
		} catch (ClassNotFoundException e) {
			throw new ServletException("Cannot find class " + className + "");
		}
		
		if (!agentClass.getSuperclass().equals(Agent.class)) {
			throw new ServletException("Class " + agentClass.getName() + 
					" must extend " + Agent.class.getName());
		}
		
		logger.info("Agent class " + agentClass.getName() + " loaded");
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
			className = MemoryContext.class.getName();
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
