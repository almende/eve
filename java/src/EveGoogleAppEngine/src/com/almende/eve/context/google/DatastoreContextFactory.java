package com.almende.eve.context.google;

import com.almende.eve.agent.AgentFactory;
import com.almende.eve.config.Config;
import com.almende.eve.context.Context;
import com.almende.eve.context.ContextFactory;
import com.google.appengine.api.utils.SystemProperty;

/**
 * @class DatastoreContextFactory
 * 
 * Factory for instantiating a DatastoreContext for an Eve Agent.
 * 
 * The context provides general information for the agent (about itself,
 * the environment, and the system configuration), and the agent can store its 
 * state in the context. 
 * The context extends a standard Java Map.
 * 
 * Usage:<br>
 *     ContextFactory factory = new DatastoreContextFactory();<br>
 *     factory.setConfig(config);<br>
 *     Context context = factory.getContext("MyAgentClass", "agentId");<br>
 *     Agent agent = new MyAgentClass();
 *     agent.setContext(context);
 * 
 * @author jos
 */
public class DatastoreContextFactory implements ContextFactory {
	private Config config = null;
	private AgentFactory agentFactory = null;
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
			servletUrl = config.get("environment", getEnvironment(), "servlet_url");
			if (servletUrl == null) {
				String path = "environment." + getEnvironment() + ".servlet_url";
				Exception e = new Exception("Config parameter '" + path + "' is missing");
				e.printStackTrace();
			}
			if (!servletUrl.endsWith("/")) {
				servletUrl += "/";
			}
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
