package com.almende.eve.context;

import com.almende.eve.config.Config;

public interface ContextFactory {
	public void setConfig(Config config) throws Exception;
	public Config getConfig() throws Exception;

	public String getEnvironment() throws Exception;
	public String getServletUrl() throws Exception;	
	
	public Context getContext(String agentClass, String id) throws Exception;
}
