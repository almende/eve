/*
 * Copyright: Almende B.V. (2014), Rotterdam, The Netherlands
 * License: The Apache Software License, Version 2.0
 */
package com.almende.eve.agent.example;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.List;

import com.almende.eve.agent.Agent;
import com.almende.eve.agent.AgentInterface;
import com.almende.eve.rpc.annotation.Access;
import com.almende.eve.rpc.annotation.AccessType;
import com.almende.eve.rpc.annotation.Name;
import com.almende.eve.rpc.jsonrpc.JSONRPCException;

/**
 * The Class ManagementAgent.
 */
@Access(AccessType.PUBLIC)
public class ManagementAgent extends Agent {
	
	/**
	 * Create a new agent. Will throw an exception if the agent already exists
	 *
	 * @param id the id
	 * @param type the type
	 * @return urls
	 * @throws JSONRPCException the jSONRPC exception
	 * @throws InstantiationException the instantiation exception
	 * @throws IllegalAccessException the illegal access exception
	 * @throws InvocationTargetException the invocation target exception
	 * @throws NoSuchMethodException the no such method exception
	 * @throws ClassNotFoundException the class not found exception
	 * @throws IOException Signals that an I/O exception has occurred.
	 */
	public List<String> create(@Name("id") final String id,
			@Name("type") final String type) throws JSONRPCException,
			InstantiationException, IllegalAccessException,
			InvocationTargetException, NoSuchMethodException,
			ClassNotFoundException, IOException {
		final Agent agent = getAgentHost().createAgent(type, id);
		return (agent != null) ? agent.getUrls() : null;
	}
	
	/**
	 * Delete an agent.
	 *
	 * @param id the id
	 * @throws JSONRPCException the jSONRPC exception
	 * @throws ClassNotFoundException the class not found exception
	 * @throws InstantiationException the instantiation exception
	 * @throws IllegalAccessException the illegal access exception
	 * @throws InvocationTargetException the invocation target exception
	 * @throws NoSuchMethodException the no such method exception
	 */
	public void delete(@Name("id") final String id) throws JSONRPCException,
			ClassNotFoundException, InstantiationException,
			IllegalAccessException, InvocationTargetException,
			NoSuchMethodException {
		getAgentHost().deleteAgent(id);
	}
	
	/**
	 * Retrieve an agents urls. If the agent does not exist,
	 * null will be returned.
	 *
	 * @param id the id
	 * @return urls
	 * @throws ClassNotFoundException the class not found exception
	 * @throws InstantiationException the instantiation exception
	 * @throws IllegalAccessException the illegal access exception
	 * @throws InvocationTargetException the invocation target exception
	 * @throws NoSuchMethodException the no such method exception
	 * @throws IOException Signals that an I/O exception has occurred.
	 */
	public List<String> get(@Name("id") final String id)
			throws ClassNotFoundException, InstantiationException,
			IllegalAccessException, InvocationTargetException,
			NoSuchMethodException, IOException {
		final AgentInterface agent = getAgentHost().getAgent(id);
		return (agent != null) ? agent.getUrls() : null;
	}
	
	/**
	 * Test if an agent exists.
	 *
	 * @param id the id
	 * @return exists
	 * @throws ClassNotFoundException the class not found exception
	 * @throws InstantiationException the instantiation exception
	 * @throws IllegalAccessException the illegal access exception
	 * @throws InvocationTargetException the invocation target exception
	 * @throws NoSuchMethodException the no such method exception
	 * @throws IOException Signals that an I/O exception has occurred.
	 */
	public boolean exists(@Name("id") final String id)
			throws ClassNotFoundException, InstantiationException,
			IllegalAccessException, InvocationTargetException,
			NoSuchMethodException, IOException {
		final AgentInterface agent = getAgentHost().getAgent(id);
		return (agent != null);
	}
	
	/* (non-Javadoc)
	 * @see com.almende.eve.agent.Agent#getDescription()
	 */
	@Override
	public String getDescription() {
		return "The ManagementAgent can create and delete agents, "
				+ "and provide general information about an agent. "
				+ "Available methods: create, delete, exists, get.";
	}
	
	/* (non-Javadoc)
	 * @see com.almende.eve.agent.Agent#getVersion()
	 */
	@Override
	public String getVersion() {
		return "0.1";
	}
}
