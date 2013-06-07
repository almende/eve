package com.almende.eve.state.google;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Iterator;
import java.util.Map;

import com.almende.eve.agent.AgentFactory;
import com.almende.eve.state.AbstractState;
import com.almende.eve.state.StateFactory;
import com.almende.util.TwigUtil;

public class DatastoreStateFactory implements StateFactory {
	/**
	 * This constructor is called when constructed by the AgentFactory
	 * @param agentFactory
	 * @param params
	 */
	public DatastoreStateFactory (AgentFactory agentFactory, 
			Map<String, Object> params) {
		this();
	}
	
	public DatastoreStateFactory () {
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
		if (state.get(AbstractState.KEY_AGENT_TYPE) != null) {
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
	public DatastoreState create(String agentId) throws IOException, FileNotFoundException {
		if (exists(agentId)) {
			throw new IllegalStateException("Cannot create state, " + 
					"state with id '" + agentId + "' already exists.");
		}
		
		return new DatastoreState(agentId);
	}

	@Override
	public void delete(String agentId) {
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
	public Iterator<String> getAllAgentIds() {
		//TODO: This needs to be implemented.
		return null;
	}

}
