package com.almende.eve.state.google;

import java.util.Map;

import com.almende.eve.agent.AgentFactory;
import com.almende.eve.state.State;
import com.almende.eve.state.StateFactory;
import com.almende.util.TwigUtil;
import com.google.appengine.api.utils.SystemProperty;

public class DatastoreStateFactory extends StateFactory {
	public DatastoreStateFactory (AgentFactory agentFactory, 
			Map<String, Object> params) {
		super(agentFactory, params);
		
		TwigUtil.register(KeyValue.class);
	}
	
	/**
	 * Get state with given id. Returns null if not found
	 * @param agentId
	 * @return state
	 * @throws Exception 
	 */
	@Override
	public DatastoreState get(String agentId) {
		DatastoreState state = new DatastoreState(agentId);
		// TODO: how to really check if the state exists in the datastore?
		if (state.get(State.KEY_AGENT_TYPE) != null) {
			return state;
		}
		return null;
	}
	
	/**
	 * Create a state with given id. Will throw an exception when already.
	 * existing.
	 * @param agentId
	 * @return state
	 */
	@Override
	public DatastoreState create(String agentId) throws Exception {
		if (exists(agentId)) {
			throw new Exception("Cannot create state, " + 
					"state with id '" + agentId + "' already exists.");
		}
		
		return new DatastoreState(agentId);
	}

	@Override
	public void delete(String agentId) throws Exception {
		// TODO: optimize deleting a state (do not first retrieve the state)
		DatastoreState state = get(agentId);
		if (state != null) {
			state.delete();
		}
	}

	@Override
	public boolean exists(String agentId) {
		// TODO: make a faster check for existence (get() will retrieve the whole state)
		return (get(agentId) != null);
	}

	@Override 
	public String getEnvironment() {
		return SystemProperty.environment.get(); // "Development" or "Production"
	}
}
