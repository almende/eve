/*
 * Copyright: Almende B.V. (2014), Rotterdam, The Netherlands
 * License: The Apache Software License, Version 2.0
 */
package com.almende.eve.state.couchdb;

import java.io.IOException;
import java.util.Iterator;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.ektorp.CouchDbConnector;
import org.ektorp.CouchDbInstance;
import org.ektorp.DocumentNotFoundException;
import org.ektorp.http.HttpClient;
import org.ektorp.http.StdHttpClient;
import org.ektorp.http.StdHttpClient.Builder;
import org.ektorp.impl.StdCouchDbInstance;

import com.almende.eve.state.StateFactory;

/**
 * A factory for creating CouchDBState objects.
 */
public class CouchDBStateFactory implements StateFactory {
	private static final Logger		LOG	= Logger.getLogger(CouchDBStateFactory.class
												.getSimpleName());
	private static CouchDbConnector	db	= null;
	
	/**
	 * Instantiates a new couch db state factory.
	 * 
	 * @param params
	 *            the params
	 */
	public CouchDBStateFactory(final Map<String, Object> params) {
		this((String) params.get("url"), (String) params.get("database"),
				(String) params.get("username"), (String) params
						.get("password"));
	}
	
	/**
	 * Instantiates a new couch db state factory.
	 * 
	 * @param url
	 *            the url
	 */
	public CouchDBStateFactory(final String url) {
		this(url, null, null, null);
	}
	
	/**
	 * Instantiates a new couch db state factory.
	 * 
	 * @param url
	 *            the url
	 * @param database
	 *            the database
	 */
	public CouchDBStateFactory(final String url, final String database) {
		this(url, database, null, null);
	}
	
	/**
	 * Instantiates a new couch db state factory.
	 * 
	 * @param url
	 *            the url
	 * @param database
	 *            the database
	 * @param username
	 *            the username
	 * @param password
	 *            the password
	 */
	public CouchDBStateFactory(final String url, final String database,
			final String username, final String password) {
		
		if (url == null) {
			LOG.severe("Cannot connect to couch db with a url");
			return;
		}
		
		String sdb = database;
		if (sdb == null) {
			sdb = "eve";
		}
		
		try {
			final Builder builder = new StdHttpClient.Builder().url(url);
			
			if (username != null && !username.isEmpty()) {
				builder.username(username);
			}
			
			if (password != null && !password.isEmpty()) {
				builder.password(password);
			}
			
			final HttpClient httpClient = builder.build();
			final CouchDbInstance dbInstance = new StdCouchDbInstance(
					httpClient);
			// if the second parameter is true, the database will be created if
			// it doesn't exists
			db = dbInstance.createConnector(sdb, true);
			
		} catch (final Exception e) {
			LOG.log(Level.SEVERE, "Failed to connect to couch db", e);
		}
	}
	
	/**
	 * Check the key if it starts with a _
	 * Add a prefix if this is the case, because _ properties are reserved.
	 * 
	 * @param key
	 *            the key
	 * @return prefixed key (if necessary)
	 */
	private String couchify(final String key) {
		if (key.startsWith("_")) {
			return "cdb" + key;
		}
		
		return key;
	}
	
	/*
	 * (non-Javadoc)
	 * 
	 * @see com.almende.eve.state.StateFactory#get(java.lang.String)
	 */
	@Override
	public CouchDBState get(final String agentId) {
		CouchDBState state = null;
		try {
			state = db.get(CouchDBState.class, couchify(agentId));
			state.setDb(db);
		} catch (final DocumentNotFoundException dEx) {
		} catch (final Exception e) {
			LOG.log(Level.WARNING, "Failed to load agent", e);
		}
		
		return state;
	}
	
	/*
	 * (non-Javadoc)
	 * 
	 * @see com.almende.eve.state.StateFactory#create(java.lang.String)
	 */
	@Override
	public synchronized CouchDBState create(final String agentId)
			throws IOException {
		if (exists(agentId)) {
			throw new IllegalStateException("Cannot create state, "
					+ "state with id '" + agentId + "' already exists.");
		}
		
		final CouchDBState state = new CouchDBState(agentId, db);
		db.create(state);
		
		return state;
	}
	
	/*
	 * (non-Javadoc)
	 * 
	 * @see com.almende.eve.state.StateFactory#delete(java.lang.String)
	 */
	@Override
	public void delete(final String agentId) {
		final CouchDBState state = get(couchify(agentId));
		db.delete(state);
	}
	
	/*
	 * (non-Javadoc)
	 * 
	 * @see com.almende.eve.state.StateFactory#exists(java.lang.String)
	 */
	@Override
	public boolean exists(final String agentId) {
		return db.contains(couchify(agentId));
	}
	
	/*
	 * (non-Javadoc)
	 * 
	 * @see com.almende.eve.state.StateFactory#getAllAgentIds()
	 */
	@Override
	public Iterator<String> getAllAgentIds() {
		return db.getAllDocIds().iterator();
	}
}
