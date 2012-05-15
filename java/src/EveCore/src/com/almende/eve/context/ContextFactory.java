package com.almende.eve.context;

import java.util.Map;

public interface ContextFactory {
	public void init(Map<String, Object> config) throws Exception;
	public Context getContext(String agentClass, String id);
}
