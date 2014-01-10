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

public class CouchDBState extends AbstractState<JsonNode> {
	private static final Logger		LOG			= Logger.getLogger("CouchDBState");
	private String					revision	= null;
	private Map<String, JsonNode>	properties	= Collections
														.synchronizedMap(new HashMap<String, JsonNode>());
	private CouchDbConnector		db			= null;
	private String					id			= null;
	
	public CouchDBState() {
	}
	
	public CouchDBState(final String agentId, final CouchDbConnector db) {
		super(agentId);
		id = couchify(agentId);
		this.db = db;
	}
	
	private void read() {
		final CouchDBState state = db.get(CouchDBState.class, id);
		revision = state.revision;
		properties = state.properties;
	}
	
	private synchronized void update() {
		db.update(this);
	}
	
	@Override
	public synchronized JsonNode locPut(final String key, final JsonNode value) {
		final String ckey = couchify(key);
		JsonNode result = null;
		try {
			result = properties.put(ckey, value);
			update();
		} catch (final UpdateConflictException uce) {
			read();
			locPut(ckey, value);
		} catch (final Exception e) {
			LOG.log(Level.WARNING, "Failed to store property", e);
		}
		
		return result;
	}
	
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
			locPutIfUnchanged(ckey, newVal, oldVal);
		} catch (final Exception e) {
			LOG.log(Level.WARNING, "", e);
		}
		
		return result;
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
	
	/**
	 * Check the key if it starts with a _
	 * Add a prefix if this is the case, because _ properties are reserved.
	 * 
	 * @param key
	 * @return prefixed key (if necessary)
	 */
	private String decouchify(final String key) {
		if (key.startsWith("cdb_")) {
			return key.replaceFirst("cdb_", "_");
		}
		
		return key;
	}
	
	@Override
	public void init() {
	}
	
	@Override
	public void destroy() {
	}
	
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
	
	@JsonProperty("_id")
	public String getId() {
		return id;
	};
	
	@JsonProperty("_id")
	public void setId(final String id) {
		this.id = id;
	}
	
	@JsonProperty("_rev")
	@JsonSerialize(include = Inclusion.NON_NULL)
	public String getRevision() {
		return revision;
	}
	
	@JsonProperty("_rev")
	public void setRevision(final String revision) {
		this.revision = revision;
	}
	
	@Override
	@JsonIgnore
	public synchronized Class<?> getAgentType() throws ClassNotFoundException {
		return super.getAgentType();
	}
	
	@Override
	@JsonIgnore
	public synchronized void setAgentType(final Class<?> agentType) {
		super.setAgentType(agentType);
	}
	
	public Map<String, JsonNode> getProperties() {
		return properties;
	}
	
	public void setProperties(final Map<String, JsonNode> properties) {
		this.properties = properties;
	}
	
	public void setDb(final CouchDbConnector db) {
		this.db = db;
	}
}
