package com.almende.eve.servlet;

import java.io.IOException;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.yaml.snakeyaml.Yaml;

import com.almende.eve.json.JSONRPC;
import com.almende.eve.json.JSONRequest;
import com.almende.eve.json.JSONResponse;


@SuppressWarnings("serial")
public class RESTServlet extends HttpServlet {
	private Logger logger = Logger.getLogger(this.getClass().getSimpleName());
	private Map<String, Object> classes = null;
	private Map<String, Object> config = null; // servlet configuration 

	@Override
	public void doGet(HttpServletRequest req, HttpServletResponse resp)
			throws IOException {
		try {
			initClasses();

			// get method from url
			String uri = req.getRequestURI();
			String[] path = uri.split("\\/");
			String className = (path.length > 2) ? path[path.length - 2] : null;
			String method = (path.length > 1) ? path[path.length - 1] : null;

			// get the correct class
			if (className != null) {
				className = className.toLowerCase();
			}
			if (className == null || !classes.containsKey(className)) {
				throw new ServletException("Class '" + className + "' not found");
			}
			Object instance = classes.get(className);
			
			// get query parameters
			JSONRequest request = new JSONRequest();
			request.setMethod(method);
			Enumeration<String> params = req.getParameterNames();
			while (params.hasMoreElements()) {
				String param = params.nextElement();
				request.putParam(param, req.getParameter(param));
			}
			
			// invoke the request
			JSONResponse response = JSONRPC.invoke(instance, request);
			
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
	 * Initialize an instance of the configured class
	 * The class is read from the servlet init parameters in web.xml.
	 * @throws Exception
	 */
	private void initClasses() throws Exception {
		if (classes != null) {
			return;
		}
		
		classes = new HashMap<String, Object>();
		
		List<String> classNames = getConfigParameter("classes");
		if (classNames == null || classNames.isEmpty()) {
			throw new ServletException(
				"Config parameter 'classes' missing in servlet configuration." +
				"This parameter must be specified in web.xml.");
		}
		
		for (int i = 0; i < classNames.size(); i++) {
			String className = classNames.get(i).trim();
			try {
				if (className != null && !className.isEmpty()) {
					Class<?> c = Class.forName(className);
					String simpleName = c.getSimpleName().toLowerCase();
					Object instance = c.getConstructor().newInstance();
					classes.put(simpleName, instance);
					
					logger.info("class " + c.getName() + " loaded");
				}
			} 
			catch (ClassNotFoundException e) {
				logger.warning("class " + className + " not found");
			}
			catch (Exception e) {
				logger.warning(e.getMessage()); 		
			}
		}		
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
	
}
