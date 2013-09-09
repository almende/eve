package com.almende.eve.state;

import java.lang.reflect.Type;
import java.util.Set;

import com.almende.util.TypeUtil;
import com.fasterxml.jackson.databind.JavaType;

public interface State {
	// key name for agent type
	String	KEY_AGENT_TYPE	= "_type";
	
	void init();
	void destroy();
	
	String getAgentId();
	
	void setAgentType(Class<?> agentType);
	Class<?> getAgentType() throws ClassNotFoundException;
	
	Object put(String key, Object value);
	Object remove(String key);
	boolean putIfUnchanged(String key, Object newVal, Object oldVal);
	boolean containsKey(String key);
	
	Set<String> keySet();
	
	void clear();
	int size();

	<T> T get(String key, Class<T> type);
	<T> T get(String key, Type type);
	<T> T get(String key, JavaType type);
	<T> T get(String key, TypeUtil<T> type);
	<T> T get(TypedKey<T> key);
}
