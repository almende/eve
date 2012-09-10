package com.almende.eve.context;

import java.util.HashMap;
import java.util.Map;

import com.almende.eve.agent.AgentFactory;
import com.almende.eve.config.Config;

/**
 * @class MemoryContextFactory
 * 
 * Factory for instantiating a MemoryContext for an Eve Agent.
 * A MemoryContext stores the state of an agent only in memory, 
 * after a restart of the application, the data will be gone!
 * 
 * The context provides general information for the agent (about itself,
 * the environment, and the system configuration), and the agent can store its 
 * state in the context. 
 * The context extends a standard Java Map.
 * 
 * Usage:<br>
 *     FileContextFactory factory = new MemoryContextFactory();<br>
 *     factory.setConfig(config);<br>
 *     Context context = factory.getContext("MyAgentClass", "agentId");<br>
 *     Agent agent = new MyAgentClass();
 *     agent.setContext(context);
 * 
 * @author jos
 *
 */
public class MemoryContextFactory implements ContextFactory {
	private String servletUrl = null;
	private AgentFactory agentFactory = null;
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
			Map<String, MemoryContext> classContexts = contexts.get(agentClass.toLowerCase());
			if (classContexts == null) {
				classContexts = new HashMap<String, MemoryContext>();
				contexts.put(agentClass.toLowerCase(), classContexts);
			}
	
			// get map with the current id
			if (id != null && classContexts != null) {
				context = classContexts.get(id);
				if (context == null) {
					context = new MemoryContext(this, agentClass, id);
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
			if (!servletUrl.endsWith("/")) {
				servletUrl += "/";
			}
			return servletUrl;
		}
		return servletUrl;
	}

	@Override
	public void setAgentFactory(AgentFactory agentFactory) throws Exception {
		this.agentFactory = agentFactory;
	}

	@Override
	public AgentFactory getAgentFactory() {
		return agentFactory;
	}
}
