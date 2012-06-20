package com.almende.eve.context.google;

import com.almende.eve.config.Config;
import com.almende.eve.context.Context;
import com.almende.eve.context.ContextFactory;
import com.google.appengine.api.utils.SystemProperty;

public class DatastoreContextFactory implements ContextFactory {
	private Config config = null;
	private String servletUrl = null;
	private String environment = null;

	public DatastoreContextFactory() {}
	
	@Override
	public void setConfig(Config config) throws Exception {
		this.config = config;
	}

	public Config getConfig() {
		return config;
	}

	@Override
	public Context getContext(String agentClass, String id) throws Exception {
		return new DatastoreContext(this, agentClass, id);
	}
	
	@Override 
	public String getEnvironment() {
		if (environment == null) {
			environment = SystemProperty.environment.get(); // "Development" or "Production"
		}
		return environment;
	}
	
	@Override
	public String getServletUrl() {
		if (servletUrl == null) {
			// read the servlet url from the config
			String path = "environment." + getEnvironment() + ".servlet_url";
			servletUrl = config.get(path);
			if (servletUrl == null) {
				Exception e = new Exception("Config parameter '" + path + "' is missing");
				e.printStackTrace();
			}
			if (!servletUrl.endsWith("/")) {
				servletUrl += "/";
			}
		}
		return servletUrl;
	}

}
