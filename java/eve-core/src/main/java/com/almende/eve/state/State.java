package com.almende.eve.state;

import java.io.Serializable;
import java.lang.reflect.Type;
import java.util.Map;

import com.almende.util.TypeUtil;
import com.fasterxml.jackson.databind.JavaType;

public interface State extends Map<String, Serializable> {
	String KEY_AGENT_TYPE = "_type"; // key name for agent type

	boolean putIfUnchanged(String key, Serializable newVal, Serializable oldVal);
	void init();     // executed once after the agent is instantiated
	void destroy();  // executed once before the agent is destroyed
	String getAgentId();
	void setAgentType(Class<?> agentType);
	Class<?> getAgentType() throws ClassNotFoundException;
	
	<T>T get(String key,Class<T> type);
	<T>T get(String key, Type type);
	<T>T get(String key, JavaType type);
	<T>T get(String key, TypeUtil<T> type);
	<T>T get(T ret, String key);
}
