package com.almende.eve.state;

import java.io.Serializable;
import java.lang.reflect.Type;
import java.util.logging.Logger;

import com.almende.eve.rpc.jsonrpc.jackson.JOM;
import com.almende.util.TypeUtil;
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
	public AbstractState(String agentId) {
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
	public synchronized void setAgentType(Class<?> agentType) {
		// TODO: dangerous to use a generic state parameter to store the agent
		// class, can be accidentally overwritten
		put(KEY_AGENT_TYPE, agentType.getName());
	}
	
	@Override
	public synchronized Object put(String key, Object value){
		if (value == null || Serializable.class.isAssignableFrom(value.getClass())){
			return _put(key,(Serializable) value);	
		} else if (JsonNode.class.isAssignableFrom(value.getClass())){
			return _put(key,(JsonNode) value);
		} else {
			System.err.println("Can't handle input that is not Serializable nor JsonNode.");
			throw new IllegalArgumentException();
		}
	}
	
	@Override
	public synchronized boolean putIfUnchanged(String key, Object newVal,
			Object oldVal){
		if (newVal == null || Serializable.class.isAssignableFrom(newVal.getClass())){
			return _putIfUnchanged(key,(Serializable) newVal, (Serializable) oldVal);
		} else if (JsonNode.class.isAssignableFrom(newVal.getClass())){
			return _putIfUnchanged(key,(JsonNode) newVal, (JsonNode) oldVal);
		} else {
			System.err.println("Can't handle input that is not Serializable nor JsonNode.");
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
		String agentType = get(KEY_AGENT_TYPE,String.class);
		if (agentType == null) {
			// try deprecated "class"
			agentType = get("class",String.class);
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
	public <T> T get(String key, Class<T> type) {
		return TypeUtil.inject(get(key), type);
	}
	
	@Override
	public <T> T get(String key, Type type) {
		return TypeUtil.inject(get(key), type);
	}
	
	@Override
	public <T> T get(String key, JavaType type) {
		return TypeUtil.inject(get(key), type);
	}
	
	@Override
	public <T> T get(String key, TypeUtil<T> type) {
		return type.inject(get(key));
	}
	
	@Override
	public <T> T get(TypedKey<T> typedKey){
		return get(typedKey.getKey(),typedKey.getType());
	}
	
	@Override
	public JsonNode _put(String key, JsonNode value) {
		LOG.warning("Warning, this type of State can't store JsonNodes, only Serializable objects. This JsonNode is stored as string.");
		_put(key, value.toString());
		return value;
	}
	
	@Override
	public boolean _putIfUnchanged(String key, JsonNode newVal, JsonNode oldVal) {
		LOG.warning("Warning, this type of State can't store JsonNodes, only Serializable objects. This JsonNode is stored as string.");
		return _putIfUnchanged(key, newVal.toString(), oldVal.toString());
	}
	
	@Override
	public synchronized Serializable _put(String key, Serializable value) {
		ObjectMapper om = JOM.getInstance();
		_put(key, om.valueToTree(value));
		return value;
	}
	
	@Override
	public boolean _putIfUnchanged(String key, Serializable newVal,
			Serializable oldVal) {
		ObjectMapper om = JOM.getInstance();
		return _putIfUnchanged(key, om.valueToTree(newVal), om.valueToTree(oldVal));
	}
}
