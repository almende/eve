package com.almende.eve.context;

import java.io.File;
import java.io.IOException;
import java.util.logging.Logger;

import com.almende.eve.config.Config;

/**
 * @class FileContextFactory
 * 
 * Factory for instantiating a FileContext for an Eve Agent.
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
 */
public class FileContextFactory implements ContextFactory {
	private Logger logger = Logger.getLogger(this.getClass().getSimpleName());

	private String servletUrl = null;
	private Config config = null;
	private String path = null;
	
	public FileContextFactory() {}

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
		return new FileContext(this, agentClass, id);
	}

	@Override
	public String getEnvironment() {
		// TODO: implement environments Development and Production
		return "Production";
	}

	/**
	 * Get the path where the data of the agents will be stored
	 * @return
	 */
	public String getPath() {
		if (path == null) {
			// retrieve the path from config file
			path = getConfig().get("context.path");
			if (path == null) {
				path = ".eveagents";
				logger.warning(
					"Config parameter 'context.path' missing in Eve " +
					"configuration. Using the default path '" + path + "'");
			}
			if (!path.endsWith("/")) path += "/";
			
			// make the directory
			File f = new File(path);
            f.mkdir();
            
            // log info
            String info = "Agents data will be stored in ";
            try {
				info += f.getCanonicalPath();
			} catch (IOException e) {
				info += path;
			}
            logger.info(info);
		}
		return path;
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
}
