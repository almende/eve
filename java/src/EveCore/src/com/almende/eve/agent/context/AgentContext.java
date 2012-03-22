package com.almende.eve.agent.context;

import javax.servlet.http.HttpServletRequest;

public interface AgentContext {
	public void setId(String id);
	public String getId();
	
	public void setServletUrlFromRequest(HttpServletRequest req);
	public void setAgentClass(String agentClass);
	public String getAgentUrl();
	
	public Object get(String key);
	public void put(String key, Object value);
	public boolean has(String key);
}
