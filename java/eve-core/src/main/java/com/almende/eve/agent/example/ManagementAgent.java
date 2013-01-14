package com.almende.eve.agent.example;

import java.util.List;

import com.almende.eve.agent.Agent;
import com.almende.eve.json.annotation.Name;

public class ManagementAgent extends Agent {
	/**
	 * Create a new agent. Will throw an exception if the agent already exists
	 * @param agentId
	 * @param agentClass
	 * @return urls
	 * @throws Exception
	 */
	public List<String> create(@Name("id") String agentId,
			@Name("class") String agentClass) throws Exception {
		Agent agent = getAgentFactory().createAgent(agentClass, agentId);
		return (agent != null) ? agent.getUrls() : null;
	}

	/**
	 * Delete an agent
	 * @param agentId
	 * @return
	 * @throws Exception
	 */
	public void delete(@Name("id") String agentId) throws Exception {
		getAgentFactory().deleteAgent(agentId);
	}

	/**
	 * Retrieve an agents urls. If the agent does not exist, 
	 * null will be returned.
	 * @param agentId
	 * @return urls
	 * @throws Exception
	 */
	public List<String> get(@Name("id") String agentId) throws Exception {
		Agent agent = getAgentFactory().getAgent(agentId);
		return (agent != null) ? agent.getUrls() : null;
	}

	/**
	 * Test if an agent exists
	 * @param agentId
	 * @return exists
	 * @throws Exception
	 */
	public boolean exists(@Name("id") String agentId) throws Exception {
		Agent agent = getAgentFactory().getAgent(agentId);
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
