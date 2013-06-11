package com.almende.eve.agent.example;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.List;

import com.almende.eve.agent.Agent;
import com.almende.eve.rpc.annotation.Access;
import com.almende.eve.rpc.annotation.AccessType;
import com.almende.eve.rpc.annotation.Name;
import com.almende.eve.rpc.jsonrpc.JSONRPCException;

@Access(AccessType.PUBLIC)
public class ManagementAgent extends Agent {
	/**
	 * Create a new agent. Will throw an exception if the agent already exists
	 * @param id
	 * @param type
	 * @return urls
	 * @throws IOException 
	 * @throws ClassNotFoundException 
	 * @throws NoSuchMethodException 
	 * @throws InvocationTargetException 
	 * @throws IllegalAccessException 
	 * @throws InstantiationException 
	 * @throws JSONRPCException 
	 */
	public List<String> create(@Name("id") String id,
			@Name("type") String type) throws JSONRPCException, InstantiationException, IllegalAccessException, InvocationTargetException, NoSuchMethodException, ClassNotFoundException, IOException {
		Agent agent = getAgentFactory().createAgent(type, id);
		return (agent != null) ? agent.getUrls() : null;
	}

	/**
	 * Delete an agent
	 * @param id
	 * @return
	 * @throws NoSuchMethodException 
	 * @throws InvocationTargetException 
	 * @throws IllegalAccessException 
	 * @throws InstantiationException 
	 * @throws ClassNotFoundException 
	 * @throws JSONRPCException 
	 */
	public void delete(@Name("id") String id) throws JSONRPCException, ClassNotFoundException, InstantiationException, IllegalAccessException, InvocationTargetException, NoSuchMethodException {
		getAgentFactory().deleteAgent(id);
	}

	/**
	 * Retrieve an agents urls. If the agent does not exist, 
	 * null will be returned.
	 * @param id
	 * @return urls
	 * @throws NoSuchMethodException 
	 * @throws InvocationTargetException 
	 * @throws IllegalAccessException 
	 * @throws InstantiationException 
	 * @throws ClassNotFoundException 
	 * @throws JSONRPCException 
	 * @throws IOException 
	 */
	public List<String> get(@Name("id") String id) throws JSONRPCException, ClassNotFoundException, InstantiationException, IllegalAccessException, InvocationTargetException, NoSuchMethodException, IOException {
		Agent agent = getAgentFactory().getAgent(id);
		return (agent != null) ? agent.getUrls() : null;
	}

	/**
	 * Test if an agent exists
	 * @param id
	 * @return exists
	 * @throws NoSuchMethodException 
	 * @throws InvocationTargetException 
	 * @throws IllegalAccessException 
	 * @throws InstantiationException 
	 * @throws ClassNotFoundException 
	 * @throws JSONRPCException 
	 * @throws IOException 
	 */
	public boolean exists(@Name("id") String id) throws JSONRPCException, ClassNotFoundException, InstantiationException, IllegalAccessException, InvocationTargetException, NoSuchMethodException, IOException {
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
