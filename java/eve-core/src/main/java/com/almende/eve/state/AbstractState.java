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

public abstract class AbstractState<V> implements State {
	private static final Logger	LOG		= Logger.getLogger(AbstractState.class
												.getCanonicalName());
	private String				agentId	= null;
	
	/**
	 * The implemented classes must have a public constructor
	 */
	public AbstractState() {
	}
	
	/**
	 * The implemented classes must have this public constructor with
	 * parameters AgentHost, and agentId
	 */
	public AbstractState(final String agentId) {
		this.agentId = agentId;
	}
	
	/**
	 * Get the agents id
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
	 * @return agentType
	 */
	@Override
	public synchronized void setAgentType(final Class<?> agentType) {
		// TODO: dangerous to use a generic state parameter to store the agent
		// class, can be accidentally overwritten
		put(KEY_AGENT_TYPE, agentType.getName());
	}
	
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
	 * @throws ClassNotFoundException
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
	
	public abstract V get(String key);
	
	@Override
	public <T> T get(final String key, final Class<T> type) {
		return TypeUtil.inject(get(key), type);
	}
	
	@Override
	public <T> T get(final String key, final Type type) {
		return TypeUtil.inject(get(key), type);
	}
	
	@Override
	public <T> T get(final String key, final JavaType type) {
		return TypeUtil.inject(get(key), type);
	}
	
	@Override
	public <T> T get(final String key, final TypeUtil<T> type) {
		return type.inject(get(key));
	}
	
	@Override
	public <T> T get(final TypedKey<T> typedKey) {
		return get(typedKey.getKey(), typedKey.getType());
	}
	
	public JsonNode locPut(final String key, final JsonNode value) {
		LOG.warning("Warning, this type of State can't store JsonNodes, only Serializable objects. This JsonNode is stored as string.");
		locPut(key, value.toString());
		return value;
	}
	
	// Default cross type input acceptance, specific States are expected to
	// override their own typed version.
	public boolean locPutIfUnchanged(final String key, final JsonNode newVal,
			final JsonNode oldVal) {
		LOG.warning("Warning, this type of State can't store JsonNodes, only Serializable objects. This JsonNode is stored as string.");
		return locPutIfUnchanged(key, newVal.toString(), oldVal.toString());
	}
	
	public synchronized Serializable locPut(final String key,
			final Serializable value) {
		final ObjectMapper om = JOM.getInstance();
		locPut(key, om.valueToTree(value));
		return value;
	}
	
	public boolean locPutIfUnchanged(final String key,
			final Serializable newVal, final Serializable oldVal) {
		final ObjectMapper om = JOM.getInstance();
		return locPutIfUnchanged(key, om.valueToTree(newVal),
				om.valueToTree(oldVal));
	}
	
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
