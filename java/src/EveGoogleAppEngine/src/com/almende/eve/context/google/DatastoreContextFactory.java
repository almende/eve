package com.almende.eve.context.google;

import java.util.Map;

import com.almende.eve.context.Context;
import com.almende.eve.context.ContextFactory;

public class DatastoreContextFactory implements ContextFactory {
	private String servletUrl = null;
	public DatastoreContextFactory() {}
	
	@Override
	public void init(Map<String, Object> config) throws Exception {
		servletUrl = (String) config.get("servlet_url");
		if (servletUrl == null) {
			throw new Exception("Config parameter 'servlet_url' is missing");
		}
	}

	@Override
	public Context getContext(String agentClass, String id) {
		return new DatastoreContext(agentClass, id, servletUrl);
	}
	
	/**
	 * Retrieve the url of the agents app from the system environment
	 * eve.properties, for example "http://myapp.appspot.com"
	 * 
	 * @return appUrl
	 */
	/* TODO: remove this
	// TODO: replace this with usage of environment
	private String getAppUrl() {
		String appUrl = null;
	
		// TODO: retrieve the servlet path from the servlet parameters itself
		// http://www.jguru.com/faq/view.jsp?EID=14839
		// search for "get servlet path without request"
		// System.out.println(req.getServletPath());

		String environment = SystemProperty.environment.get();
		String id = SystemProperty.applicationId.get();
		// String version = SystemProperty.applicationVersion.get();
		
		if (environment.equals("Development")) {
			// TODO: check the port
			appUrl = "http://localhost:8888";
		} else {
			// production
			// TODO: reckon with the version of the application?
			appUrl = "http://" + id + ".appspot.com";
			// TODO: use https by default
			//appUrl = "https://" + id + ".appspot.com";
		}
		
		return appUrl;
	}
	*/
}
