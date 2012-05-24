package com.almende.eve.context.google;

import java.io.IOException;

import com.almende.eve.context.Context;
import com.almende.eve.scheduler.Scheduler;
import com.almende.eve.scheduler.google.AppEngineScheduler;
import com.almende.eve.config.Config;
import com.google.code.twig.ObjectDatastore;
import com.google.code.twig.annotation.AnnotationObjectDatastore;

public class DatastoreContext implements Context {
	private Config config = null;
	private String environment = null;
	private String servletUrl = null;
	private String agentUrl = null;
	private String agentClass = null;
	private String agentId = null;
	private Scheduler scheduler = null;
	
	public DatastoreContext() {}

	protected DatastoreContext(String environment, String servletUrl, 
			String agentClass, String agentId, Config config) {
		this.environment = environment;
		this.servletUrl = servletUrl;
		this.agentClass = agentClass;
		this.agentId = agentId;
		this.config = config;
		// Note: agentUrl will be initialized when needed
	}

	/**
	 * Retrieve the url of the agents app from the system environment
	 * eve.properties, for example "http://myapp.appspot.com"
	 * 
	 * @return appUrl
	 */
	/* TODO: cleanup
	// TODO: replace this with usage of environment
	private String getAppUrl() {
		String appUrl = null;
	
		// TODO: retrieve the servlet path from the servlet parameters itself
		// http://www.jguru.com/faq/view.jsp?EID=14839
		// search for "get servlet path without request"
		// System.out.println(req.getServletPath());

		String environment = SystemProperty.environment.get();
		String id = SystemProperty.applicationId.get();
		// String version = SystemProperty.applicationVersion.get();
		
		if (environment.equals("Development")) {
			// TODO: check the port
			appUrl = "http://localhost:8888";
		} else {
			// production
			// TODO: reckon with the version of the application?
			appUrl = "http://" + id + ".appspot.com";
			// TODO: use https by default
			//appUrl = "https://" + id + ".appspot.com";
		}
		
		return appUrl;
	}
	*/
	
	@Override
	public String getAgentId() {
		return agentId;
	}

	@Override
	public String getAgentClass() {
		return agentClass;
	}
	
	/**
	 * Generate the full key, which is defined as "id.key"
	 * @param key
	 * @return
	 */
	private String getFullKey (String key) {
		return agentId + "." + key;
	}
	
	// TODO: load and save in a transaction
	
	@Override
	public <T> T get(String key) {
		ObjectDatastore datastore = new AnnotationObjectDatastore();
		
		String fullKey = getFullKey(key);
		KeyValue entity = datastore.load(KeyValue.class, fullKey);
		if (entity != null) {
			try {
				return entity.getValue();
			} catch (ClassNotFoundException e) {
				return null;
			} catch (IOException e) {
				return null;
			}
		}
		else {
			return null;
		}
	}

	@Override
	public void put(String key, Object value) {
		ObjectDatastore datastore = new AnnotationObjectDatastore();

		try {
			String fullKey = getFullKey(key);
			KeyValue entity = new KeyValue(fullKey, value);
			datastore.store(entity);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} 
	}

	@Override
	public boolean has(String key) {
		ObjectDatastore datastore = new AnnotationObjectDatastore();

		String fullKey = getFullKey(key);
		KeyValue entity = datastore.load(KeyValue.class, fullKey);
		return (entity != null);
	}

	@Override
	public void remove(String key) {
		ObjectDatastore datastore = new AnnotationObjectDatastore();

		String fullKey = getFullKey(key);
		KeyValue entity = datastore.load(KeyValue.class, fullKey);
		if (entity != null) {
			datastore.delete(entity);
		}
	}

	@Override
	public String getAgentUrl() {
		if (agentUrl == null) {
			String servletUrl = getServletUrl();
			if (servletUrl != null) {
				agentUrl = servletUrl;
				if (!agentUrl.endsWith("/")) {
					agentUrl += "/";
				}
				if (agentClass != null) {
					agentUrl += agentClass + "/";
					if (agentId != null) {
						agentUrl += agentId;
					}
				}
			}			
		}
		return agentUrl;
	}
	
	@Override
	public String getServletUrl() {
		return servletUrl;
	}	
	
	@Override 
	public String getEnvironment() {
		return environment;
	}
	
	@Override
	public Scheduler getScheduler() {
		if (scheduler == null) {
			scheduler = new AppEngineScheduler();
		}
		return scheduler;
	}
	
	@Override
	public Config getConfig() {
		return config;
	}
	
	@Override
	public void beginTransaction() {
		// TODO: transaction
	}

	@Override
	public void commitTransaction() {
		// TODO: transaction
	}
	
	@Override
	public void rollbackTransaction() {
		// TODO: transaction
	}
	
	@Override
	protected void finalize() throws Throwable {
		// TODO: rollback a transaction when active
	}
}
