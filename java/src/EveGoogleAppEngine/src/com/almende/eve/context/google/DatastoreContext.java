package com.almende.eve.context.google;

import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import com.almende.eve.context.Context;
import com.almende.eve.scheduler.Scheduler;
import com.almende.eve.scheduler.google.AppEngineScheduler;
import com.almende.eve.config.Config;
import com.google.code.twig.ObjectDatastore;
import com.google.code.twig.annotation.AnnotationObjectDatastore;

public class DatastoreContext implements Context {
	private DatastoreContextFactory factory = null;
	private Scheduler scheduler = null;
	private String agentClass = null;
	private String agentId = null;
	private String agentUrl = null;
	private KeyValue entity = null;
	private Map<String, Object> properties = null;
	
	public DatastoreContext() {}

	protected DatastoreContext(DatastoreContextFactory factory, 
			String agentClass, String agentId) {
		this.factory = factory;
		this.agentClass = agentClass;
		this.agentId = agentId;
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
						agentUrl += agentId + "/";
					}
				}
			}			
		}
		return agentUrl;
	}
	
	@Override
	public String getServletUrl() {
		return factory.getServletUrl();
	}	
	
	@Override 
	public String getEnvironment() {
		return factory.getEnvironment();
	}
	
	@Override
	public Config getConfig() {
		return factory.getConfig();
	}

	@Override
	public Scheduler getScheduler() {
		if (scheduler == null) {
			scheduler = new AppEngineScheduler();
		}
		return scheduler;
	}

	/**
	 * Load/refresh the entity, retrieve it from the datastore
	 * @throws IOException 
	 * @throws ClassNotFoundException 
	 */
	@SuppressWarnings("unchecked")
	private void refresh() {
		try {
			ObjectDatastore datastore = new AnnotationObjectDatastore();
			String propertiesKey = agentClass + "." + agentId;
			if (entity == null) {
				entity = datastore.load(KeyValue.class, propertiesKey);
				if (entity == null) {
					entity = new KeyValue(propertiesKey, properties);
					datastore.store(entity);
				}
			}
			else {
				datastore.associate(entity);
				datastore.refresh(entity);
			}
			
			@SuppressWarnings("rawtypes")
			Class<? extends HashMap> MAP_OBJECT_CLASS = 
				(new HashMap<String, Object>()).getClass();
			
			properties = entity.getValue(MAP_OBJECT_CLASS);
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		// ensure there is always a properties initialized!
		if (properties == null) {
			properties = new HashMap<String, Object>();
		}
	}
	
	/**
	 * Update the entity, store changes to the datastore
	 * @param entity
	 * @throws IOException 
	 */
	private void update() {
		try {
			ObjectDatastore datastore = new AnnotationObjectDatastore();
			// TODO: check if entity != null
			entity.setValue(properties);
			datastore.associate(entity);
			datastore.update(entity);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	@Override
	public Object get(Object key) {
		refresh();
		return properties.get(key);
	}

	@Override
	public Object put(String key, Object value) {
		refresh();
		Object ret = properties.put(key, value);
		update();
		return ret;
	}

	@Override
	public boolean containsKey(Object key) {
		refresh();
		return properties.containsKey(key);
	}

	@Override
	public Object remove(Object key) {
		refresh();
		Object value = properties.remove(key);
		update();
		return value;
	}

	@Override
	public void clear() {
		refresh();
		properties.clear();
		update();
	}

	@Override
	public boolean containsValue(Object value) {
		refresh();
		return properties.containsValue(value);
	}

	@Override
	public Set<java.util.Map.Entry<String, Object>> entrySet() {
		refresh();
		return properties.entrySet();
	}

	@Override
	public boolean isEmpty() {
		refresh();
		return properties.isEmpty();
	}

	@Override
	public Set<String> keySet() {
		refresh();
		return properties.keySet();
	}

	@Override
	public void putAll(Map<? extends String, ? extends Object> map) {
		refresh();
		properties.putAll(map);
		update();
	}

	@Override
	public int size() {
		refresh();
		return properties.size();
	}

	@Override
	public Collection<Object> values() {
		refresh();
		return properties.values();
	}
}
