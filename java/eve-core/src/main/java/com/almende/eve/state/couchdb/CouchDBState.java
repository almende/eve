/*
 * Copyright: Almende B.V. (2014), Rotterdam, The Netherlands
 * License: The Apache Software License, Version 2.0
 */
package com.almende.eve.state.couchdb;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.ektorp.CouchDbConnector;
import org.ektorp.UpdateConflictException;

import com.almende.eve.rpc.jsonrpc.jackson.JOM;
import com.almende.eve.state.AbstractState;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize.Inclusion;
import com.fasterxml.jackson.databind.node.NullNode;

/**
 * The Class CouchDBState.
 */
public class CouchDBState extends AbstractState<JsonNode> {
	private static final Logger		LOG			= Logger.getLogger("CouchDBState");
	private String					revision	= null;
	private Map<String, JsonNode>	properties	= Collections
														.synchronizedMap(new HashMap<String, JsonNode>());
	private CouchDbConnector		db			= null;
	private String					id			= null;
	
	/**
	 * Instantiates a new couch db state.
	 */
	public CouchDBState() {
	}
	
	/**
	 * Instantiates a new couch db state.
	 *
	 * @param agentId the agent id
	 * @param db the db
	 */
	public CouchDBState(final String agentId, final CouchDbConnector db) {
		super(agentId);
		id = couchify(agentId);
		this.db = db;
	}
	
	/**
	 * Read.
	 */
	private void read() {
		final CouchDBState state = db.get(CouchDBState.class, id);
		revision = state.revision;
		properties = state.properties;
	}
	
	/**
	 * Update.
	 */
	private synchronized void update() {
		db.update(this);
	}
	
	/* (non-Javadoc)
	 * @see com.almende.eve.state.AbstractState#locPut(java.lang.String, com.fasterxml.jackson.databind.JsonNode)
	 */
	@Override
	public synchronized JsonNode locPut(final String key, final JsonNode value) {
		final String ckey = couchify(key);
		JsonNode result = null;
		try {
			result = properties.put(ckey, value);
			update();
		} catch (final UpdateConflictException uce) {
			read();
			return locPut(ckey, value);
		} catch (final Exception e) {
			LOG.log(Level.WARNING, "Failed to store property", e);
		}
		
		return result;
	}
	
	/* (non-Javadoc)
	 * @see com.almende.eve.state.AbstractState#locPutIfUnchanged(java.lang.String, com.fasterxml.jackson.databind.JsonNode, com.fasterxml.jackson.databind.JsonNode)
	 */
	@Override
	public synchronized boolean locPutIfUnchanged(final String key,
			final JsonNode newVal, JsonNode oldVal) {
		final String ckey = couchify(key);
		boolean result = false;
		try {
			JsonNode cur = NullNode.getInstance();
			if (properties.containsKey(ckey)) {
				cur = properties.get(ckey);
			}
			if (oldVal == null) {
				oldVal = NullNode.getInstance();
			}
			
			// Poor mans equality as some Numbers are compared incorrectly: e.g.
			// IntNode versus LongNode
			if (oldVal.equals(cur) || oldVal.toString().equals(cur.toString())) {
				properties.put(ckey, newVal);
				update();
				result = true;
			}
		} catch (final UpdateConflictException uce) {
			read();
			return locPutIfUnchanged(ckey, newVal, oldVal);
		} catch (final Exception e) {
			LOG.log(Level.WARNING, "", e);
		}
		
		return result;
	}
	
	/**
	 * Check the key if it starts with a _
	 * Add a prefix if this is the case, because _ properties are reserved.
	 *
	 * @param key the key
	 * @return prefixed key (if necessary)
	 */
	private String couchify(final String key) {
		if (key.startsWith("_")) {
			return "cdb" + key;
		}
		
		return key;
	}
	
	/**
	 * Check the key if it starts with a _
	 * Add a prefix if this is the case, because _ properties are reserved.
	 *
	 * @param key the key
	 * @return prefixed key (if necessary)
	 */
	private String decouchify(final String key) {
		if (key.startsWith("cdb_")) {
			return key.replaceFirst("cdb_", "_");
		}
		
		return key;
	}
	
	/* (non-Javadoc)
	 * @see com.almende.eve.state.State#init()
	 */
	@Override
	public void init() {
	}
	
	/* (non-Javadoc)
	 * @see com.almende.eve.state.State#destroy()
	 */
	@Override
	public void destroy() {
	}
	
	/* (non-Javadoc)
	 * @see com.almende.eve.state.State#remove(java.lang.String)
	 */
	@Override
	public synchronized Object remove(final String key) {
		Object result = null;
		try {
			result = properties.remove(key);
			update();
		} catch (final Exception e) {
			LOG.log(Level.WARNING, "", e);
		}
		return result;
	}
	
	/* (non-Javadoc)
	 * @see com.almende.eve.state.State#containsKey(java.lang.String)
	 */
	@Override
	public boolean containsKey(final String key) {
		final String ckey = couchify(key);
		boolean result = false;
		try {
			result = properties.containsKey(ckey);
		} catch (final Exception e) {
			LOG.log(Level.WARNING, "", e);
		}
		return result;
	}
	
	/* (non-Javadoc)
	 * @see com.almende.eve.state.State#keySet()
	 */
	@Override
	public Set<String> keySet() {
		final Set<String> result = new HashSet<String>();
		Set<String> keys = null;
		try {
			keys = new HashSet<String>(properties.keySet());
			for (final String key : keys) {
				result.add(decouchify(key));
			}
		} catch (final Exception e) {
			LOG.log(Level.WARNING, "", e);
		}
		return result;
	}
	
	/* (non-Javadoc)
	 * @see com.almende.eve.state.State#clear()
	 */
	@Override
	public synchronized void clear() {
		try {
			final String agentType = properties
					.get(couchify(KEY_AGENT_TYPE)).textValue();
			properties.clear();
			properties.put(couchify(KEY_AGENT_TYPE), JOM.getInstance()
					.valueToTree(agentType));
			update();
		} catch (final Exception e) {
			LOG.log(Level.WARNING, "Failed clearing state", e);
		}
	}
	
	/* (non-Javadoc)
	 * @see com.almende.eve.state.State#size()
	 */
	@Override
	public int size() {
		int result = -1;
		try {
			result = properties.size();
		} catch (final Exception e) {
			LOG.log(Level.WARNING, "", e);
		}
		return result;
	}
	
	/* (non-Javadoc)
	 * @see com.almende.eve.state.AbstractState#get(java.lang.String)
	 */
	@Override
	public JsonNode get(String key) {
		key = couchify(key);
		JsonNode result = null;
		try {
			result = properties.get(key);
		} catch (final Exception e) {
			LOG.log(Level.WARNING, "", e);
		}
		return result;
	}
	
	/**
	 * Gets the id.
	 *
	 * @return the id
	 */
	@JsonProperty("_id")
	public String getId() {
		return id;
	};
	
	/**
	 * Sets the id.
	 *
	 * @param id the new id
	 */
	@JsonProperty("_id")
	public void setId(final String id) {
		this.id = id;
	}
	
	/**
	 * Gets the revision.
	 *
	 * @return the revision
	 */
	@JsonProperty("_rev")
	@JsonSerialize(include = Inclusion.NON_NULL)
	public String getRevision() {
		return revision;
	}
	
	/**
	 * Sets the revision.
	 *
	 * @param revision the new revision
	 */
	@JsonProperty("_rev")
	public void setRevision(final String revision) {
		this.revision = revision;
	}
	
	/* (non-Javadoc)
	 * @see com.almende.eve.state.AbstractState#getAgentType()
	 */
	@Override
	@JsonIgnore
	public synchronized Class<?> getAgentType() throws ClassNotFoundException {
		return super.getAgentType();
	}
	
	/* (non-Javadoc)
	 * @see com.almende.eve.state.AbstractState#setAgentType(java.lang.Class)
	 */
	@Override
	@JsonIgnore
	public synchronized void setAgentType(final Class<?> agentType) {
		super.setAgentType(agentType);
	}
	
	/**
	 * Gets the properties.
	 *
	 * @return the properties
	 */
	public Map<String, JsonNode> getProperties() {
		return properties;
	}
	
	/**
	 * Sets the properties.
	 *
	 * @param properties the properties
	 */
	public void setProperties(final Map<String, JsonNode> properties) {
		this.properties = properties;
	}
	
	/**
	 * Sets the db.
	 *
	 * @param db the new db
	 */
	public void setDb(final CouchDbConnector db) {
		this.db = db;
	}
}
