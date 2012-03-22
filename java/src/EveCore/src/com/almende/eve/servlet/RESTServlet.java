package com.almende.eve.servlet;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.almende.eve.json.JSONRPC;
import com.almende.eve.json.JSONRequest;
import com.almende.eve.json.JSONResponse;


@SuppressWarnings("serial")
public class RESTServlet extends HttpServlet {
	private Logger logger = Logger.getLogger(this.getClass().getSimpleName());
	private Map<String, Object> classes = null;

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
			
			System.out.println("request:" + request.toString());
			
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
	 * @throws ServletException
	 * @throws NoSuchMethodException 
	 * @throws InvocationTargetException 
	 * @throws IllegalAccessException 
	 * @throws InstantiationException 
	 * @throws SecurityException 
	 * @throws IllegalArgumentException 
	 */
	private void initClasses() throws ServletException, IllegalArgumentException, SecurityException, InstantiationException, IllegalAccessException, InvocationTargetException, NoSuchMethodException {
		if (classes != null) {
			return;
		}
		
		classes = new HashMap<String, Object>();
		
		String classesParam = getInitParameter("classes");
		
		if (classesParam == null || classesParam.isEmpty()) {
			throw new ServletException(
				"Init parameter 'classes' missing in servlet configuration." +
				"This parameter must be specified in web.xml.");
		}
		
		String[] classeNames = classesParam.split(";");

		for (int i = 0; i < classeNames.length; i++) {
			String className = classeNames[i].trim();
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
}
