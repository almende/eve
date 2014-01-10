/*
 * Copyright: Almende B.V. (2014), Rotterdam, The Netherlands
 * License: The Apache Software License, Version 2.0
 */
package com.almende.eve.state;

import java.io.Serializable;
import java.lang.reflect.Type;
import java.util.logging.Logger;

import com.almende.eve.rpc.jsonrpc.jackson.JOM;
import com.almende.util.TypeUtil;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * The Class AbstractState.
 *
 * @param <V> the value type
 * @author Almende
 */
public abstract class AbstractState<V> implements State {
	private static final Logger	LOG		= Logger.getLogger(AbstractState.class
												.getCanonicalName());
	private String				agentId	= null;
	
	/**
	 * The implemented classes must have a public constructor.
	 */
	public AbstractState() {
	}
	
	/**
	 * The implemented classes must have this public constructor with
	 * parameters AgentHost, and agentId.
	 *
	 * @param agentId the agent id
	 */
	public AbstractState(final String agentId) {
		this.agentId = agentId;
	}
	
	/**
	 * Get the agents id.
	 *
	 * @return agentId
	 */
	@Override
	public synchronized String getAgentId() {
		return agentId;
	}
	
	/**
	 * Set the configured agents class.
	 *
	 * @param agentType the new agent type
	 */
	@Override
	public synchronized void setAgentType(final Class<?> agentType) {
		// TODO: dangerous to use a generic state parameter to store the agent
		// class, can be accidentally overwritten
		put(KEY_AGENT_TYPE, agentType.getName());
	}
	
	/* (non-Javadoc)
	 * @see com.almende.eve.state.State#put(java.lang.String, java.lang.Object)
	 */
	@Override
	public synchronized Object put(final String key, final Object value) {
		if (value == null
				|| Serializable.class.isAssignableFrom(value.getClass())) {
			return locPut(key, (Serializable) value);
		} else if (JsonNode.class.isAssignableFrom(value.getClass())) {
			return locPut(key, (JsonNode) value);
		} else {
			LOG.severe("Can't handle input that is not Serializable nor JsonNode.");
			throw new IllegalArgumentException();
		}
	}
	
	/* (non-Javadoc)
	 * @see com.almende.eve.state.State#putIfUnchanged(java.lang.String, java.lang.Object, java.lang.Object)
	 */
	@Override
	public synchronized boolean putIfUnchanged(final String key,
			final Object newVal, final Object oldVal) {
		if (newVal == null
				|| Serializable.class.isAssignableFrom(newVal.getClass())) {
			return locPutIfUnchanged(key, (Serializable) newVal,
					(Serializable) oldVal);
		} else if (JsonNode.class.isAssignableFrom(newVal.getClass())) {
			return locPutIfUnchanged(key, (JsonNode) newVal, (JsonNode) oldVal);
		} else {
			LOG.severe("Can't handle input that is not Serializable nor JsonNode.");
			throw new IllegalArgumentException();
		}
	}
	
	/**
	 * Get the configured agents type (the full class path).
	 *
	 * @return type
	 * @throws ClassNotFoundException the class not found exception
	 */
	@Override
	public synchronized Class<?> getAgentType() throws ClassNotFoundException {
		String agentType = get(KEY_AGENT_TYPE, String.class);
		if (agentType == null) {
			// try deprecated "class"
			agentType = get("class", String.class);
			if (agentType != null) {
				put(KEY_AGENT_TYPE, agentType);
				remove("class");
			}
		}
		if (agentType != null) {
			return Class.forName(agentType);
		} else {
			return null;
		}
	}
	
	/**
	 * Gets the.
	 *
	 * @param key the key
	 * @return the v
	 */
	public abstract V get(String key);
	
	/* (non-Javadoc)
	 * @see com.almende.eve.state.State#get(java.lang.String, java.lang.Class)
	 */
	@Override
	public <T> T get(final String key, final Class<T> type) {
		return TypeUtil.inject(get(key), type);
	}
	
	/* (non-Javadoc)
	 * @see com.almende.eve.state.State#get(java.lang.String, java.lang.reflect.Type)
	 */
	@Override
	public <T> T get(final String key, final Type type) {
		return TypeUtil.inject(get(key), type);
	}
	
	/* (non-Javadoc)
	 * @see com.almende.eve.state.State#get(java.lang.String, com.fasterxml.jackson.databind.JavaType)
	 */
	@Override
	public <T> T get(final String key, final JavaType type) {
		return TypeUtil.inject(get(key), type);
	}
	
	/* (non-Javadoc)
	 * @see com.almende.eve.state.State#get(java.lang.String, com.almende.util.TypeUtil)
	 */
	@Override
	public <T> T get(final String key, final TypeUtil<T> type) {
		return type.inject(get(key));
	}
	
	/* (non-Javadoc)
	 * @see com.almende.eve.state.State#get(com.almende.eve.state.TypedKey)
	 */
	@Override
	public <T> T get(final TypedKey<T> typedKey) {
		return get(typedKey.getKey(), typedKey.getType());
	}
	
	/**
	 * Loc put.
	 *
	 * @param key the key
	 * @param value the value
	 * @return the json node
	 */
	public JsonNode locPut(final String key, final JsonNode value) {
		LOG.warning("Warning, this type of State can't store JsonNodes, only Serializable objects. This JsonNode is stored as string.");
		locPut(key, value.toString());
		return value;
	}
	
	// Default cross type input acceptance, specific States are expected to
	// override their own typed version.
	/**
	 * Loc put if unchanged.
	 *
	 * @param key the key
	 * @param newVal the new val
	 * @param oldVal the old val
	 * @return true, if successful
	 */
	public boolean locPutIfUnchanged(final String key, final JsonNode newVal,
			final JsonNode oldVal) {
		LOG.warning("Warning, this type of State can't store JsonNodes, only Serializable objects. This JsonNode is stored as string.");
		return locPutIfUnchanged(key, newVal.toString(), oldVal.toString());
	}
	
	/**
	 * Loc put.
	 *
	 * @param key the key
	 * @param value the value
	 * @return the serializable
	 */
	public synchronized Serializable locPut(final String key,
			final Serializable value) {
		final ObjectMapper om = JOM.getInstance();
		locPut(key, om.valueToTree(value));
		return value;
	}
	
	/**
	 * Loc put if unchanged.
	 *
	 * @param key the key
	 * @param newVal the new val
	 * @param oldVal the old val
	 * @return true, if successful
	 */
	public boolean locPutIfUnchanged(final String key,
			final Serializable newVal, final Serializable oldVal) {
		final ObjectMapper om = JOM.getInstance();
		return locPutIfUnchanged(key, om.valueToTree(newVal),
				om.valueToTree(oldVal));
	}
	
	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		final StringBuilder result = new StringBuilder();
		for (final String key : keySet()) {
			try {
				result.append("'"
						+ key
						+ "': "
						+ JOM.getInstance().writeValueAsString(
								get(key, JsonNode.class)));
			} catch (final JsonProcessingException e) {
				result.append("'" + key + "': [unprintable]");
			}
			result.append("\n");
		}
		return result.toString();
	}
}
