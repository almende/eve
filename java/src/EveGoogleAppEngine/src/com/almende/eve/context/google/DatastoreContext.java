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
	private void refresh() throws IOException {
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
		
		try {
			@SuppressWarnings("rawtypes")
			Class<? extends HashMap> MAP_OBJECT_CLASS = 
				(new HashMap<String, Object>()).getClass();
			
			properties = entity.getValue(MAP_OBJECT_CLASS);
		} catch (ClassNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		if (properties == null) {
			properties = new HashMap<String, Object>();
		}
	}
	
	/**
	 * Update the entity, store changes to the datastore
	 * @param entity
	 * @throws IOException 
	 */
	private void update() throws IOException {
		// TODO: does it give better performance when making a datastore
		//       in the context and make this synchronized?
		
		ObjectDatastore datastore = new AnnotationObjectDatastore();
		entity.setValue(properties);
		datastore.associate(entity);
		datastore.update(entity);
	}
	
	@Override
	public Object get(Object key) {
		try {
			refresh();
			return properties.get(key);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return null;
	}

	@Override
	public Object put(String key, Object value) {
		try {
			refresh();
			properties.put(key, value);
			update();
			return true;
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return null;
	}

	@Override
	public boolean containsKey(Object key) {
		try {
			refresh();
			return properties.containsKey(key);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return false;
	}

	@Override
	public Object remove(Object key) {
		try {
			refresh();
			Object value = properties.remove(key);
			update();
			return value;
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return null;
	}

	@Override
	public void clear() {
		try {
			refresh();
			properties.clear();
			update();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	@Override
	public boolean containsValue(Object value) {
		try {
			refresh();
			return properties.containsValue(value);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return false;
	}

	@Override
	public Set<java.util.Map.Entry<String, Object>> entrySet() {
		try {
			refresh();
			return properties.entrySet();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return null;
	}

	@Override
	public boolean isEmpty() {
		try {
			refresh();
			return properties.isEmpty();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return true;
	}

	@Override
	public Set<String> keySet() {
		try {
			refresh();
			return properties.keySet();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return null;
	}

	@Override
	public void putAll(Map<? extends String, ? extends Object> map) {
		try {
			refresh();
			properties.putAll(map);
			update();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	@Override
	public int size() {
		try {
			refresh();
			return properties.size();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return 0;
	}

	@Override
	public Collection<Object> values() {
		try {
			refresh();
			return properties.values();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return null;
	}
}
