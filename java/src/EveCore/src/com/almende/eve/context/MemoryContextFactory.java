package com.almende.eve.context;

import java.util.HashMap;
import java.util.Map;

import com.almende.eve.config.Config;

public class MemoryContextFactory implements ContextFactory {
	private String servletUrl = null;
	private Config config = null;
	// Singleton containing all contexts, stored in a Map[agentClass][id]
	private Map<String, Map<String, MemoryContext>> contexts = 
		new HashMap<String, Map<String, MemoryContext>>();
	
	public MemoryContextFactory() {}

	@Override
	public void setConfig(Config config) {
		this.config = config;
	}

	@Override
	public Config getConfig() {
		return config;
	}

	@Override
	public Context getContext(String agentClass, String id) throws Exception {
		MemoryContext context = null;

		if (agentClass != null) {
			// get map with the current agentClass
			Map<String, MemoryContext> classContexts = contexts.get(agentClass);
			if (classContexts == null) {
				classContexts = new HashMap<String, MemoryContext>();
				contexts.put(agentClass, classContexts);
			}
	
			// get map with the current id
			if (id != null && classContexts != null) {
				context = classContexts.get(id);
				if (context == null) {
					context = new MemoryContext(getEnvironment(), 
							getServletUrl(), agentClass, id, config);
					classContexts.put(id, context);
				}
			}
		}
		
		return context;
	}

	@Override
	public String getEnvironment() {
		// TODO: implement environments Development and Production
		return "Production";
	}
	
	@Override
	public String getServletUrl() {
		if (servletUrl == null) {
			// read the servlet url from the config
			String path = "environment." + getEnvironment() + ".servlet_url";
			String servletUrl = config.get(path);
			if (servletUrl == null) {
				Exception e = new Exception("Config parameter '" + path + "' is missing");
				e.printStackTrace();
			}
			return servletUrl;
		}
		return servletUrl;
	}
}
