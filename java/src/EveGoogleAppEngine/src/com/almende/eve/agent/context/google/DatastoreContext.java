package com.almende.eve.agent.context.google;

import javax.servlet.http.HttpServletRequest;

import com.almende.eve.agent.context.AgentContext;
import com.google.appengine.api.utils.SystemProperty;
import com.google.code.twig.ObjectDatastore;
import com.google.code.twig.annotation.AnnotationObjectDatastore;

public class DatastoreContext implements AgentContext {
	// TODO: change the DataStoreContext 
	
	private String servletUrl = null;
	private String agentClass = null;
	private String id = null;
	private ObjectDatastore datastore = new AnnotationObjectDatastore();
	private DatastoreEntity entity = null; 
	
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
	
	private DatastoreEntity getEntity() {
		if (entity == null) {
			// TODO: do not create entity if not existing, but only if id exists
			entity = datastore.load(DatastoreEntity.class, id);	
			if (entity == null) {
				entity = new DatastoreEntity(id);
				datastore.store(entity);
			}
		}
		return entity;
	}
	
	@Override
	public Object get(String key) {
		DatastoreEntity e = getEntity();
		datastore.refresh(e);
		return e.get(key);
	}

	@Override
	public void put(String key, Object value) {
		DatastoreEntity e = getEntity();
		e.put(key, value);
		datastore.update(e);		
	}

	@Override
	public boolean has(String key) {
		DatastoreEntity e = getEntity();
		datastore.refresh(e);
		return e.has(key);
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
