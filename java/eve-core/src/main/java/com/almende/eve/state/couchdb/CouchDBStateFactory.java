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

public class CouchDBStateFactory implements StateFactory {
	
	private static final Logger		LOG	= Logger.getLogger(CouchDBStateFactory.class
												.getSimpleName());
	private static CouchDbConnector	db	= null;
	
	public CouchDBStateFactory(Map<String, Object> params) {
		this((String) params.get("url"), (String) params.get("database"),
				(String) params.get("username"), (String) params
						.get("password"));
	}
	
	public CouchDBStateFactory(String url) {
		this(url, null, null, null);
	}
	
	public CouchDBStateFactory(String url, String database) {
		this(url, database, null, null);
	}
	
	public CouchDBStateFactory(String url, final String database,
			String username, String password) {
		
		if (url == null) {
			LOG.severe("Cannot connect to couch db with a url");
			return;
		}
		
		String sdb = database;
		if (sdb == null) {
			sdb = "eve";
		}
		
		try {
			Builder builder = new StdHttpClient.Builder().url(url);
			
			if (username != null && !username.isEmpty()) {
				builder.username(username);
			}
			
			if (password != null && !password.isEmpty()) {
				builder.password(password);
			}
			
			HttpClient httpClient = builder.build();
			CouchDbInstance dbInstance = new StdCouchDbInstance(httpClient);
			// if the second parameter is true, the database will be created if
			// it doesn't exists
			db = dbInstance.createConnector(sdb, true);
			
		} catch (Exception e) {
			LOG.log(Level.SEVERE, "Failed to connect to couch db", e);
		}
	}
	
	/**
	 * Check the key if it starts with a _
	 * Add a prefix if this is the case, because _ properties are reserved.
	 * 
	 * @param key
	 * @return prefixed key (if necessary)
	 */
	private String couchify(final String key) {
		if (key.startsWith("_")) {
			return "cdb" + key;
		}
		
		return key;
	}
	
	@Override
	public CouchDBState get(final String agentId) {
		CouchDBState state = null;
		try {
			state = db.get(CouchDBState.class, couchify(agentId));
			state.setDb(db);
		} catch (DocumentNotFoundException dEx) {
		} catch (Exception e) {
			LOG.log(Level.WARNING, "Failed to load agent", e);
		}
		
		return state;
	}
	
	@Override
	public synchronized CouchDBState create(String agentId) throws IOException {
		if (exists(agentId)) {
			throw new IllegalStateException("Cannot create state, "
					+ "state with id '" + agentId + "' already exists.");
		}
		
		CouchDBState state = new CouchDBState(agentId, db);
		db.create(state);
		
		return state;
	}
	
	@Override
	public void delete(String agentId) {
		CouchDBState state = get(couchify(agentId));
		db.delete(state);
	}
	
	@Override
	public boolean exists(final String agentId) {
		return db.contains(couchify(agentId));
	}
	
	@Override
	public Iterator<String> getAllAgentIds() {
		return db.getAllDocIds().iterator();
	}
}
