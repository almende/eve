package com.almende.eve.state;

import java.io.Serializable;
import java.lang.reflect.Type;
import java.util.Map;

import com.fasterxml.jackson.databind.JavaType;

public interface State extends Map<String, Serializable> {
	public static String KEY_AGENT_TYPE = "_type"; // key name for agent type

	public boolean putIfUnchanged(String key, Serializable newVal, Serializable oldVal);
	public void init();     // executed once after the agent is instantiated
	public void destroy();  // executed once before the agent is destroyed
	public String getAgentId();
	public void setAgentType(Class<?> agentType);
	public Class<?> getAgentType() throws ClassNotFoundException;
	
	public <T>T get(Class<T> type, String key);
	public <T>T get(Type type, String key);
	public <T>T get(JavaType type, String key);
}
