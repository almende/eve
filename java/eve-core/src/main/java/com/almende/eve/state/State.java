package com.almende.eve.state;

import java.io.Serializable;
import java.lang.reflect.Type;
import java.util.Set;

import com.almende.util.TypeUtil;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonNode;

public interface State {
	// key name for agent type
	String	KEY_AGENT_TYPE	= "_type";
	
	void init();
	void destroy();
	
	String getAgentId();
	
	void setAgentType(Class<?> agentType);
	Class<?> getAgentType() throws ClassNotFoundException;
	
	Serializable _put(String key, Serializable value);
	JsonNode _put(String key, JsonNode value);
	Object put(String key, Object value);
	
	Object remove(String key);
	
	boolean putIfUnchanged(String key, Object newVal, Object oldVal);
	boolean _putIfUnchanged(String key, Serializable newVal, Serializable oldVal);
	boolean _putIfUnchanged(String key, JsonNode newVal, JsonNode oldVal);
	
	boolean containsKey(String key);
	
	Set<String> keySet();
	
	void clear();
	int size();

	<T> T get(String key, Class<T> type);
	<T> T get(String key, Type type);
	<T> T get(String key, JavaType type);
	<T> T get(String key, TypeUtil<T> type);
	<T> T get(TypedKey<T> key);
	<T> T get(T ret, String key);
}
