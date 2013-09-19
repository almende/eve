package com.almende.eve.state.google;

import java.io.IOException;
import java.io.Serializable;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import com.almende.eve.state.AbstractState;
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
public class DatastoreState extends AbstractState<Serializable> {
	private Map<String, Serializable> properties = new ConcurrentHashMap<String, Serializable>();
	private MemcacheService cache = MemcacheServiceFactory.getMemcacheService();
	private IdentifiableValue cacheValue = null;
	private boolean isChanged = false;
	
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
	private void load() {
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
	 * changed. The state will be stored in the datastore after a fixed delay
	 * or when the method .destroy() is executed.
	 * @return success    returns true if the change is saved in memcache.
	 */
	private boolean save() {
		return save(false);
	}
	
	/**
	 * Store changes in the state into memcache, and mark the state as
	 * changed. The state will be stored in the datastore after a fixed delay
	 * or when the method .destroy() is executed.
	 * @param force       If true, the save method will immediately write
	 *                    The state to the datastore instead of after a delay.
	 * @return success    returns true if the change is saved in memcache.
	 */
	private boolean save(boolean force) {
		// save to memcache
		isChanged = true;
		boolean success = saveToCache();

		if (force) {
			// store immediately in the datastore
			isChanged = false;
			saveToDatastore();
		}
		else {
			// TODO: (re)implement a mechanism to write to the datastore less often
			isChanged = false;
			saveToDatastore();
		}
		
		return success;
	}

	/**
	 * Load the state from cache
	 * @return success
	 */
	@SuppressWarnings("unchecked")
	private boolean loadFromCache() {
		cacheValue = cache.getIdentifiable(getAgentId());
		if (cacheValue != null && cacheValue.getValue() != null) {
			properties = (Map<String, Serializable>) cacheValue.getValue();
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
			success = cache.putIfUntouched(getAgentId(), cacheValue, properties);
		}
		else {
			success = cache.put(getAgentId(), properties, null, 
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
			KeyValue entity = datastore.load(KeyValue.class, getAgentId());
			
			@SuppressWarnings("rawtypes")
			Class<? extends HashMap> MAP_OBJECT_CLASS = 
				(new HashMap<String, Serializable>()).getClass();
			
			if (entity != null) {
				// TODO: can this be simplified with the following?:
				//       Map<String, Object> newProperties = entity.getValue(Map.class);
				Map<String, Serializable> newProperties = entity.getValue(MAP_OBJECT_CLASS);
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
			KeyValue entity = new KeyValue(getAgentId(), properties);
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
		KeyValue entity = datastore.load(KeyValue.class, getAgentId());
		if (entity != null) {
			datastore.delete(entity);
		}
	}
	
	/**
	 * Delete the entity from the Cache
	 * @param entity
	 */
	private void deleteFromCache () {
		cache.delete(getAgentId());
		// TODO: check if deletion was successful?
	}
	
	/**
	 * init is executed once before the agent method is invoked
	 */
	@Override
	public void init() {}

	/**
	 * If the state is changed, it will be stored in the datastore on destroy. 
	 */
	@Override
	public void destroy() {
		if (isChanged) {
			isChanged = false;
			saveToDatastore();
		}
	}

	/**
	 * Permanently delete this state
	 */
	protected void delete() {
		clear();
		deleteFromCache();
		deleteFromDatastore();

		isChanged = false;
	}

	@Override
	public Serializable get(String key) {
		load();
		return properties.get(key);
	}

	@Override
	public Serializable locPut(String key, Serializable value) {
		load();
		Serializable ret = properties.put(key, value);
		boolean success = save();
		if (!success) {
			ret = null;
		}
		return ret;
	}

	@Override
	public boolean containsKey(String key) {
		load();
		return properties.containsKey(key);
	}

	@Override
	public Serializable remove(String key) {
		load();
		Serializable value = properties.remove(key);
		save();
		return value;
	}

	@Override
	public void clear() {
		load();
		properties.clear();
		save();
	}

	public boolean isEmpty() {
		load();
		return properties.isEmpty();
	}

	@Override
	public Set<String> keySet() {
		load();
		return new HashSet<String>(properties.keySet());
	}

	@Override
	public boolean locPutIfUnchanged(String key, Serializable newVal, Serializable oldVal) {
		boolean result=false;
		load();
		if ((oldVal == null && properties.containsKey(key)) || properties.get(key).equals(oldVal)){
			properties.put(key,newVal);
			save();
			result=true;
		}
		return result;
	}
	
	@Override
	public int size() {
		load();
		return properties.size();
	}
}

