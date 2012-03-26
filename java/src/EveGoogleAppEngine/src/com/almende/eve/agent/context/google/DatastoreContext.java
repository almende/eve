package com.almende.eve.agent.context.google;

import java.io.IOException;

import javax.servlet.http.HttpServletRequest;

import com.almende.eve.agent.context.AgentContext;
import com.google.appengine.api.utils.SystemProperty;
import com.google.code.twig.ObjectDatastore;
import com.google.code.twig.annotation.AnnotationObjectDatastore;

public class DatastoreContext implements AgentContext {
	private String servletUrl = null;
	private String agentClass = null;
	private String id = null;
	private ObjectDatastore datastore = new AnnotationObjectDatastore();
	
	public DatastoreContext() {}

	public DatastoreContext(String id) {
		setId(id);
	}
	
	@Override
	public void setId(String id) {
		this.id = id;
	}
	
	@Override
	public String getId() {
		return id;
	}
	
	/**
	 * Generate the full key, which is defined as "id.key"
	 * @param key
	 * @return
	 */
	private String getFullKey (String key) {
		return id + "." + key;
	}
	
	@Override
	public Object get(String key) {
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
		try {
			String fullKey = getFullKey(key);
			KeyValue entity= new KeyValue(fullKey, value);
			datastore.store(entity);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} 
	}

	@Override
	public boolean has(String key) {
		String fullKey = getFullKey(key);
		KeyValue entity = datastore.load(KeyValue.class, fullKey);
		return (entity != null);
	}

	@Override
	public void setServletUrlFromRequest(HttpServletRequest req) {
		// this class does not utilize the request
		servletUrl = getAppUrl() +  req.getServletPath() + "/" ;
	}

	@Override
	public void setAgentClass(String agentClass) {
		if (agentClass != null) {
			this.agentClass = agentClass;
		}
	}

	@Override
	public String getAgentUrl() {
		String agentUrl = null;
		if (servletUrl != null) {
			agentUrl = servletUrl;
			if (agentClass != null) {
				agentUrl += agentClass + "/";
				if (id != null) {
					agentUrl += id;
				}
			}
		}
		
		return agentUrl;
	}	
	
	/**
	 * Retrieve the url of the agents app from the system environment
	 * eve.properties, for example "http://myapp.appspot.com"
	 * 
	 * @return appUrl
	 */
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
			appUrl = "https://" + id + ".appspot.com";
		}
		
		return appUrl;
	}
}
