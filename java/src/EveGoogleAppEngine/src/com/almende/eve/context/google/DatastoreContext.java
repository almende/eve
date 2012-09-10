package com.almende.eve.context.google;

import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import com.almende.eve.agent.AgentFactory;
import com.almende.eve.context.Context;
import com.almende.eve.scheduler.Scheduler;
import com.almende.eve.scheduler.google.AppEngineScheduler;
import com.almende.eve.config.Config;
import com.almende.eve.json.JSONRequest;
import com.almende.eve.json.JSONResponse;
import com.google.appengine.api.memcache.MemcacheService;
import com.google.appengine.api.memcache.MemcacheService.IdentifiableValue;
import com.google.appengine.api.memcache.MemcacheService.SetPolicy;
import com.google.appengine.api.memcache.MemcacheServiceFactory;
import com.google.code.twig.ObjectDatastore;
import com.google.code.twig.annotation.AnnotationObjectDatastore;

/**
 * @class DatastoreContext
 * 
 * A context for an Eve Agent, which stores the data in the Google Datastore.
 * This context is only available when the application is running in Google 
 * App Engine.
 * 
 * The context provides general information for the agent (about itself,
 * the environment, and the system configuration), and the agent can store its 
 * state in the context. 
 * The context extends a standard Java Map.
 * 
 * During the lifetime of a DatastoreContext, the context synchronized over all
 * running instances of the same DatastoreContext using MemCache.
 * 
 * Usage:<br>
 *     ContextFactory factory = new DatastoreContextFactory();<br>
 *     factory.setConfig(config);<br>
 *     Context context = factory.getContext("agentClass", "agentId");<br>
 *     context.put("key", "value");<br>
 *     System.out.println(context.get("key")); // "value"<br>
 * 
 * @author jos
 */
public class DatastoreContext implements Context {
	private DatastoreContextFactory factory = null;
	private Scheduler scheduler = null;
	private String agentClass = null;
	private String agentId = null;
	private String agentUrl = null;
	private Map<String, Object> properties = new HashMap<String, Object>();
	
	private MemcacheService cache = MemcacheServiceFactory.getMemcacheService();
	private IdentifiableValue cacheValue = null;
	private boolean isChanged = false;
	
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
			scheduler.setContext(this);
		}
		return scheduler;
	}

	/**
	 * Load the context from cache. If the context is not available in cache,
	 * it will be loaded from the Datastore and the cache will be created.
	 * If there is no context stored in both cache and Datastore, an empty
	 * map with properties is initialized.
	 */
	private void refresh() {
		// load from cache
		boolean success = loadFromCache();
		if (!success) {
			// if not in cache, load from datastore
			success = loadFromDatastore();
			if (success) {
				// create memcache entry
				saveToCache();
			}
		}
	}
	
	/**
	 * Store changes in the context into memcache, and mark the context as
	 * changed. The context will be stored in the datastore when the method 
	 * .destroy() is executed.
	 * @return success    returns true if the change is saved in memcache.
	 */
	private boolean update() {
		isChanged = true;
		boolean success = saveToCache();
		return success;
	}

	/**
	 * Get a unique key for storing the context. The key is a composed key,
	 * namely "agentclass.agentid".
	 * @return
	 */
	private String getPropertiesKey () {
		return agentClass.toLowerCase() + "." + agentId;
	}

	/**
	 * Load the context from cache
	 * @return success
	 */
	@SuppressWarnings("unchecked")
	private boolean loadFromCache() {
		cacheValue = cache.getIdentifiable(getPropertiesKey());
		if (cacheValue != null && cacheValue.getValue() != null) {
			properties = (Map<String, Object>) cacheValue.getValue();
			return true;
		}
		
		return false;
	}
	
	/**
	 * Save the context to cache
	 * If the cache is changed since the last retrieval of the cache, saving
	 * will fail and false will be returned.
	 * @return success
	 */
	private boolean saveToCache() {
		boolean success = false;
		if (cacheValue != null) {
			success = cache.putIfUntouched(getPropertiesKey(), cacheValue, properties);
		}
		else {
			success = cache.put(getPropertiesKey(), properties, null, 
					SetPolicy.ADD_ONLY_IF_NOT_PRESENT);
			if (success) {
				// reload from cache to get the IdentifiableValue from cache,
				// for later reference
				success = loadFromCache();
			}
		}
		
		return success;
	}
	
	/**
	 * load the properties from the datastore
	 * @return success    True if successfully loaded
	 * @throws IOException 
	 * @throws ClassNotFoundException 
	 * @return
	 */
	@SuppressWarnings("unchecked")
	private boolean loadFromDatastore () {
		try {
			ObjectDatastore datastore = new AnnotationObjectDatastore();
			KeyValue entity = datastore.load(KeyValue.class, getPropertiesKey());
			
			@SuppressWarnings("rawtypes")
			Class<? extends HashMap> MAP_OBJECT_CLASS = 
				(new HashMap<String, Object>()).getClass();
			
			if (entity != null) {
				Map<String, Object> newProperties = entity.getValue(MAP_OBJECT_CLASS);
				if (newProperties != null) {
					properties = newProperties;
				}
			}
			
			return true;
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		return false;
	}
	
	/**
	 * Write the properties to the datastore when they are changed
	 * @param entity
	 * @return success    True if successfully saved
	 * @throws IOException 
	 */
	private boolean saveToDatastore () {
		try {
			ObjectDatastore datastore = new AnnotationObjectDatastore();
			KeyValue entity = new KeyValue(getPropertiesKey(), properties);
			datastore.store(entity);
			return true;
		} catch (IOException e) {
			e.printStackTrace();
		}
		return false;
	}

	/**
	 * Delete the entity from the Datastore
	 * @param entity
	 */
	private void deleteFromDatastore () {
		ObjectDatastore datastore = new AnnotationObjectDatastore();
		KeyValue entity = datastore.load(KeyValue.class, getPropertiesKey());
		if (entity != null) {
			datastore.delete(entity);
		}
	}
	
	/**
	 * Delete the entity from the Cache
	 * @param entity
	 */
	private void deleteFromCache () {
		cache.delete(getPropertiesKey());
		// TODO: check if deletion was successful?
	}
	
	/**
	 * init is executed once before the agent method is invoked
	 */
	@Override
	public synchronized void init() {}

	/**
	 * If the context is changed, it will be stored in the datastore on destroy. 
	 */
	@Override
	public synchronized void destroy() {
		if (isChanged) {
			saveToDatastore();
			isChanged = false;
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
		boolean success = update();
		if (!success) {
			ret = null;
		}
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

		isChanged = false;
		properties.clear();
		deleteFromCache();
		deleteFromDatastore();
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

	@Override
	public JSONResponse invoke(String url, JSONRequest request)
			throws Exception {
		return getAgentFactory().invoke(url, request);
	}

	@Override
	public AgentFactory getAgentFactory() {
		return factory.getAgentFactory();
	}
}
