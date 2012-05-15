package com.almende.eve.servlet;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.lang.reflect.InvocationTargetException;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.yaml.snakeyaml.Yaml;

import com.almende.eve.agent.Agent;
import com.almende.eve.context.Context;
import com.almende.eve.context.ContextFactory;
import com.almende.eve.context.MemoryContext;
import com.almende.eve.json.JSONRPC;
import com.almende.eve.json.JSONRPCException;
import com.almende.eve.json.JSONResponse;


@SuppressWarnings("serial")
public class SingleAgentServlet extends HttpServlet {
	private Logger logger = Logger.getLogger(this.getClass().getSimpleName());
	
	private Class<?> agentClass = null;
	private ContextFactory contextFactory = null;
	private Map<String, Object> config = null; // servlet configuration 
	
	@Override
	public void init() {
		
	}
	
	@Override
	public void doGet(HttpServletRequest request,
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
		String response = "";
		try {
			// initialize the context, and agent
			initContext();
			initAgent();
			
			// retrieve the request data
			String request = streamToString(req.getInputStream());

			// instantiate an agent and set its context
			Agent agent = loadAgent(req);
			
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
	 * instantiate an agent and set its context
	 * @return
	 * @throws NoSuchMethodException 
	 * @throws InvocationTargetException 
	 * @throws IllegalAccessException 
	 * @throws InstantiationException 
	 * @throws SecurityException 
	 * @throws IllegalArgumentException 
	 */
	Agent loadAgent(HttpServletRequest req) 
			throws IllegalArgumentException, SecurityException, 
			InstantiationException, IllegalAccessException, 
			InvocationTargetException, NoSuchMethodException {
		Agent agent = (Agent) agentClass.getConstructor().newInstance();
		String agentClassName = agent.getClass().getSimpleName().toLowerCase();
		String id = "1"; // TODO: what to do with id?
		Context context = contextFactory.getContext(agentClassName, id);
		agent.setContext(context);
		
		return agent;		
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

		List<String> classNames = getConfigParameter("agents");
		
		if (classNames == null || classNames.size() == 0) {
			throw new ServletException(
				"Config parameter 'agents' missing in servlet configuration." +
				"This parameter must be specified in web.xml.");
		}
		if (classNames.size() > 1) {
			throw new ServletException(
					"Config parameter 'agents' may only contain one class");
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

		String className = getConfigParameter("context_factory");
		
		if (className == null || className.isEmpty()) {
			className = MemoryContext.class.getName();
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
		newContextFactory.init(getConfig());
		
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
