package com.almende.eve.state;

import java.io.Serializable;
import java.util.Map;

//TODO: shouldn't this be a Map<String,Serializable> instead? At least for the non JSON states.
public interface State extends Map<String, Serializable> {
	public static String KEY_AGENT_TYPE = "_type"; // key name for agent type

	public boolean putIfUnchanged(String key, Serializable newVal, Serializable oldVal);
	public void init();     // executed once after the agent is instantiated
	public void destroy();  // executed once before the agent is destroyed
	public String getAgentId();
	public void setAgentType(Class<?> agentType);
	public Class<?> getAgentType() throws ClassNotFoundException;
	
}
