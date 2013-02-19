package com.almende.eve.agent.example;

import java.util.List;

import com.almende.eve.agent.Agent;
import com.almende.eve.agent.annotation.Name;

public class ManagementAgent extends Agent {
	/**
	 * Create a new agent. Will throw an exception if the agent already exists
	 * @param id
	 * @param type
	 * @return urls
	 * @throws Exception
	 */
	public List<String> create(@Name("id") String id,
			@Name("type") String type) throws Exception {
		Agent agent = getAgentFactory().createAgent(type, id);
		return (agent != null) ? agent.getUrls() : null;
	}

	/**
	 * Delete an agent
	 * @param id
	 * @return
	 * @throws Exception
	 */
	public void delete(@Name("id") String id) throws Exception {
		getAgentFactory().deleteAgent(id);
	}

	/**
	 * Retrieve an agents urls. If the agent does not exist, 
	 * null will be returned.
	 * @param id
	 * @return urls
	 * @throws Exception
	 */
	public List<String> get(@Name("id") String id) throws Exception {
		Agent agent = getAgentFactory().getAgent(id);
		return (agent != null) ? agent.getUrls() : null;
	}

	/**
	 * Test if an agent exists
	 * @param id
	 * @return exists
	 * @throws Exception
	 */
	public boolean exists(@Name("id") String id) throws Exception {
		Agent agent = getAgentFactory().getAgent(id);
		return (agent != null);
	}

	@Override
	public String getDescription() {
		return "The ManagementAgent can create and delete agents, " +
				"and provide general information about an agent. " +
				"Available methods: create, delete, exists, get.";
	}

	@Override
	public String getVersion() {
		return "0.1";
	}
}
