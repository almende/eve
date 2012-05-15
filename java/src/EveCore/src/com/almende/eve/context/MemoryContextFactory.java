package com.almende.eve.context;

import java.util.HashMap;
import java.util.Map;

public class MemoryContextFactory implements ContextFactory {	
	// Singleton containing all contexts, stored in a Map[agentClass][id]
	private static String servletUrl = null;
	private static Map<String, Map<String, MemoryContext>> contexts = 
		new HashMap<String, Map<String, MemoryContext>>();
	
	public MemoryContextFactory() {}

	@Override
	public void init(Map<String, Object> config) throws Exception {
		servletUrl = (String) config.get("servlet_url");
		if (servletUrl == null) {
			throw new Exception("Config parameter 'servlet_url' is missing");
		}
	}
	
	/* TODO: cleanup
	@Override
	public synchronized void setServletUrl (HttpServletRequest req) {
		// TODO: reckon with https
		servletUrl = "http://" + req.getServerName() + ":" + req.getServerPort() + 
			req.getContextPath() + req.getServletPath() + "/";
	}
	*/
	
	@Override
	public Context getContext(String agentClass, String id) {
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
					context = new MemoryContext(agentClass, id, servletUrl);
					classContexts.put(id, context);
				}
			}
		}
		
		return context;
	}
}
