package com.almende.eve.context.google;

import java.util.Map;

import com.almende.eve.agent.AgentFactory;
import com.almende.eve.context.ContextFactory;
import com.almende.util.TwigUtil;
import com.google.appengine.api.utils.SystemProperty;

public class DatastoreContextFactory extends ContextFactory {
	public DatastoreContextFactory (AgentFactory agentFactory, 
			Map<String, Object> params) {
		super(agentFactory, params);
		
		TwigUtil.register(KeyValue.class);
	}
	
	/**
	 * Get context with given id. Returns null if not found
	 * @param agentId
	 * @return context
	 * @throws Exception 
	 */
	@Override
	public DatastoreContext get(String agentId) {
		DatastoreContext context = new DatastoreContext(agentId);
		// TODO: how to really check if the context exists in the datastore?
		if (context.get("class") != null) {
			return context;
		}
		return null;
	}
	
	/**
	 * Create a context with given id. Will throw an exception when already.
	 * existing.
	 * @param agentId
	 * @return context
	 */
	@Override
	public DatastoreContext create(String agentId) throws Exception {
		if (exists(agentId)) {
			throw new Exception("Cannot create context, " + 
					"context with id '" + agentId + "' already exists.");
		}
		
		return new DatastoreContext(agentId);
	}

	@Override
	public void delete(String agentId) throws Exception {
		// TODO: optimize deleting a context (do not first retrieve the context)
		DatastoreContext context = get(agentId);
		if (context != null) {
			context.delete();
		}
	}

	@Override
	public boolean exists(String agentId) {
		// TODO: make a faster check for existence (get() will retrieve the whole context)
		return (get(agentId) != null);
	}

	@Override 
	public String getEnvironment() {
		return SystemProperty.environment.get(); // "Development" or "Production"
	}
}
