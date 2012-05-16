package com.almende.eve.context;

import java.util.HashMap;
import java.util.Map;

import com.almende.eve.config.Config;

public class MemoryContextFactory implements ContextFactory {	
	// Singleton containing all contexts, stored in a Map[agentClass][id]
	private Config config = null;
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
					context = new MemoryContext(agentClass, id, config);
					classContexts.put(id, context);
				}
			}
		}
		
		return context;
	}

}
