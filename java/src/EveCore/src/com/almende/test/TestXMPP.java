package com.almende.test;

import java.util.Scanner;

import com.almende.eve.agent.Agent;
import com.almende.eve.agent.AgentFactory;
import com.almende.eve.agent.example.TestAgent;
import com.almende.eve.config.Config;

public class TestXMPP {
	public static void main (String[] args) throws Exception {
		// instantiate an agent factory
		Config config = new Config("war/WEB-INF/eve_xmpp.yaml");
		AgentFactory factory = new AgentFactory(config);
		
		// instantiate an agent and connect it to a messenger service
		String agentClass = "TestAgent";
		String agentId = "alex";
		String agentPassword = "alex";
		TestAgent agent = (TestAgent)factory.getAgent(agentClass, agentId);
		agent.messengerConnect(agentId, agentPassword);

		// instantiate an agent
		agentClass = "TestAgent";
		agentId = "gloria";
		agentPassword = "gloria";
		Agent agent2 = factory.getAgent(agentClass, agentId);
		agent2.messengerConnect(agentId, agentPassword);
		
        agent.testAsyncXMPP("jos@ec2-54-246-24-74.eu-west-1.compute.amazonaws.com");

        //agent.testAsyncHTTP();
		
		System.out.println("Press ENTER to quit");
		Scanner scanner = new Scanner(System.in);
        scanner.nextLine();
        
        // in the end, neatly destroy the agent (this may be necessary to 
        // persist the agents state)
        agent.destroy();
        agent2.destroy();
        
        System.out.println("bye!");
	}
}
