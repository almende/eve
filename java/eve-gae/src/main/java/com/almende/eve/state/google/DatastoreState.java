package com.almende.eve.state.google;

import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import com.almende.eve.state.State;
import com.google.appengine.api.memcache.MemcacheService;
import com.google.appengine.api.memcache.MemcacheService.IdentifiableValue;
import com.google.appengine.api.memcache.MemcacheService.SetPolicy;
import com.google.appengine.api.memcache.MemcacheServiceFactory;
import com.google.code.twig.ObjectDatastore;
import com.google.code.twig.annotation.AnnotationObjectDatastore;

/**
 * @class DatastoreState
 * 
 * A state for an Eve Agent, which stores the data in the Google Datastore.
 * This state is only available when the application is running in Google 
 * App Engine.
 * 
 * The state provides general information for the agent (about itself,
 * the environment, and the system configuration), and the agent can store its 
 * state in the state. 
 * The state extends a standard Java Map.
 * 
 * During the lifetime of a DatastoreState, the state synchronized over all
 * running instances of the same DatastoreState using MemCache.
 * 
 * Usage:<br>
 *     AgentFactory factory = new AgentFactory(config);<br>
 *     DatastoreState state = 
 *     	   new DatastoreState(factory, "agentType", "agentId");<br>
 *     state.put("key", "value");<br>
 *     System.out.println(state.get("key")); // "value"<br>
 * 
 * @author jos
 */
public class DatastoreState extends State {
	public DatastoreState() {}

	public DatastoreState(String agentId) {
		super(agentId);
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

	/**
	 * Load the state from cache. If the state is not available in cache,
	 * it will be loaded from the Datastore and the cache will be created.
	 * If there is no state stored in both cache and Datastore, an empty
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
	 * Store changes in the state into memcache, and mark the state as
	 * changed. The state will be stored in the datastore when the method 
	 * .destroy() is executed.
	 * @return success    returns true if the change is saved in memcache.
	 */
	private boolean update() {
		isChanged = true;
		boolean success = saveToCache();
		return success;
	}

	/**
	 * Load the state from cache
	 * @return success
	 */
	@SuppressWarnings("unchecked")
	private boolean loadFromCache() {
		cacheValue = cache.getIdentifiable(agentId);
		if (cacheValue != null && cacheValue.getValue() != null) {
			properties = (Map<String, Object>) cacheValue.getValue();
			return true;
		}
		
		return false;
	}
	
	/**
	 * Save the state to cache
	 * If the cache is changed since the last retrieval of the cache, saving
	 * will fail and false will be returned.
	 * @return success
	 */
	private boolean saveToCache() {
		boolean success = false;
		if (cacheValue != null) {
			success = cache.putIfUntouched(agentId, cacheValue, properties);
		}
		else {
			success = cache.put(agentId, properties, null, 
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
			KeyValue entity = datastore.load(KeyValue.class, agentId);
			
			@SuppressWarnings("rawtypes")
			Class<? extends HashMap> MAP_OBJECT_CLASS = 
				(new HashMap<String, Object>()).getClass();
			
			if (entity != null) {
				// TODO: can this be simplified with the following?:
				//       Map<String, Object> newProperties = entity.getValue(Map.class);
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
			KeyValue entity = new KeyValue(agentId, properties);
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
		KeyValue entity = datastore.load(KeyValue.class, agentId);
		if (entity != null) {
			datastore.delete(entity);
		}
	}
	
	/**
	 * Delete the entity from the Cache
	 * @param entity
	 */
	private void deleteFromCache () {
		cache.delete(agentId);
		// TODO: check if deletion was successful?
	}
	
	/**
	 * init is executed once before the agent method is invoked
	 */
	@Override
	public synchronized void init() {}

	/**
	 * If the state is changed, it will be stored in the datastore on destroy. 
	 */
	@Override
	public synchronized void destroy() {
		if (isChanged) {
			refresh();
			saveToDatastore();
			isChanged = false;
		}
	}

	/**
	 * Permanently delete this state
	 */
	protected void delete() {
		clear();
		deleteFromCache();
		deleteFromDatastore();
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

	private Map<String, Object> properties = new HashMap<String, Object>();
	private MemcacheService cache = MemcacheServiceFactory.getMemcacheService();
	private IdentifiableValue cacheValue = null;
	private boolean isChanged = false;
}

