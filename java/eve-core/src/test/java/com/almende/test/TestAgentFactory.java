package com.almende.test;

import com.almende.eve.agent.AgentFactory;
import com.almende.eve.agent.example.TestAgentInterface;

public class TestAgentFactory {
	public static void main (String[] args) throws Exception {
		AgentFactory factory = new AgentFactory();
		
		TestAgentInterface agent = factory.createAgentProxy(null, 
				"http://eveagents.appspot.com/agents/testagent/1/", 
				TestAgentInterface.class);
		
		Double res = agent.add(3.1, 4.2);
		
		System.out.println("result=" + res);
	}
}
