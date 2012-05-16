package com.almende.eve.context.google;

import com.almende.eve.config.Config;
import com.almende.eve.context.Context;
import com.almende.eve.context.ContextFactory;

public class DatastoreContextFactory implements ContextFactory {
	private Config config = null;
	
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
		return new DatastoreContext(agentClass, id, config);
	}
}
