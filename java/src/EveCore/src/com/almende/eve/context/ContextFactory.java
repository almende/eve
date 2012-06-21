package com.almende.eve.context;

import com.almende.eve.config.Config;

/**
 * @class ContextFactory
 * 
 * Interface for a context factory for Eve Agents.
 * 
 * The context provides general information for the agent (about itself,
 * the environment, and the system configuration), and the agent can store its 
 * state in the context. 
 * The context extends a standard Java Map.
 * 
 * Usage:<br>
 *     FileContextFactory factory = new FileContextFactory();<br>
 *     factory.setConfig(config);<br>
 *     Context context = factory.getContext("MyAgentClass", "agentId");<br>
 *     Agent agent = new MyAgentClass();
 *     agent.setContext(context);
 * 
 * @author jos
 *
 */
public interface ContextFactory {
	public void setConfig(Config config) throws Exception;
	public Config getConfig() throws Exception;

	public String getEnvironment() throws Exception;
	public String getServletUrl() throws Exception;	
	
	public Context getContext(String agentClass, String id) throws Exception;
}
