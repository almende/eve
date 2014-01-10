package com.almende.eve.agent.example;

import java.io.IOException;

import com.almende.eve.agent.Agent;
import com.almende.eve.agent.AgentHost;
import com.almende.eve.rpc.annotation.Access;
import com.almende.eve.rpc.annotation.AccessType;
import com.almende.eve.rpc.annotation.Name;
import com.almende.eve.rpc.jsonrpc.JSONRPCException;
import com.almende.eve.rpc.jsonrpc.jackson.JOM;
import com.almende.eve.state.MemoryStateFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * This class forms an Eve agent with two public RPC methods:
 * "helloWorld()" and "callMyself()".
 */
@Access(AccessType.PUBLIC)
public class MyFirstAgent extends Agent {
	/**
	 * Example function to be called.
	 * 
	 * @param name
	 * @return
	 */
	public String helloWorld(@Name("name") final String name) {
		return "Hello " + name + " and the rest of the world!";
	}
	
	/**
	 * Through this method the agent calls itself and prints the result to the
	 * console.
	 * 
	 * @throws IOException
	 * @throws JSONRPCException
	 */
	public void callMyself() throws IOException, JSONRPCException {
		final ObjectNode params = JOM.createObjectNode();
		params.put("name", getId());
		final String result = send(getFirstUrl(), "helloWorld", params, String.class);
		System.out.println("Got reply:" + result);
	}
	
	/**
	 * Main method to make this example self contained, providing the AgentHost,
	 * creating the agent, triggering the call, etc.
	 * 
	 * @param args
	 */
	public static void main(final String[] args) {
		final AgentHost host = AgentHost.getInstance();
		host.setStateFactory(new MemoryStateFactory());
		try {
			
			final MyFirstAgent agent = host.createAgent(MyFirstAgent.class,
					"MyFirstAgent");
			agent.callMyself();
			
		} catch (final Exception e) {
			System.err.println("Strange, couldn't create agent in host.");
			e.printStackTrace();
		}
	}
}
