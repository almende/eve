package com.almende.eve.context.google;

import java.io.IOException;

import com.almende.eve.context.Context;
import com.almende.eve.scheduler.Scheduler;
import com.almende.eve.scheduler.google.AppEngineScheduler;
import com.google.code.twig.ObjectDatastore;
import com.google.code.twig.annotation.AnnotationObjectDatastore;

public class DatastoreContext implements Context {
	private String servletUrl = null;
	private String agentClass = null;
	private String id = null;
	private Scheduler scheduler = null;
	
	public DatastoreContext() {}

	protected DatastoreContext(String agentClass, String id, String servletUrl) {
		this.agentClass = agentClass;
		this.id = id;
		this.servletUrl = servletUrl;
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
	
	@Override
	public Scheduler getScheduler() {
		if (scheduler == null) {
			scheduler = new AppEngineScheduler();
		}
		return scheduler;
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
