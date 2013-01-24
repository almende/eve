package com.almende.test;

import java.util.Scanner;

import com.almende.eve.agent.Agent;
import com.almende.eve.agent.AgentFactory;
import com.almende.eve.agent.example.TestAgent;
import com.almende.eve.context.FileContextFactory;
import com.almende.eve.transport.xmpp.XmppService;

public class TestXmpp {
	public static void main (String[] args) throws Exception {
		/*
		// instantiate an agent factory from config file
		Config config = new Config("war/WEB-INF/eve_xmpp.yaml");
		AgentFactory factory = new AgentFactory(config);
		XmppService xmppService = (XmppService) factory.getService("xmpp");
		*/
		
		// instantiate an agent factory (without config file)
		AgentFactory factory = new AgentFactory();
		FileContextFactory contextFactory = new FileContextFactory(factory, ".eveagents");
		factory.setContextFactory(contextFactory);
		//String host = "ec2-54-246-112-19.eu-west-1.compute.amazonaws.com";
		String host = "openid.almende.org";
		int port = 5222;
		String serviceName = host;
		XmppService xmppService = new XmppService(factory);
		xmppService.init(host, port, serviceName);
		factory.addTransportService(xmppService);
		
		// instantiate an agent and connect it to a messenger service
		String agentId = "alex";
		String agentPassword = "alex";
		TestAgent agent = (TestAgent)factory.getAgent(agentId);
		if (agent == null) {
			System.out.println("Create agent " + agentId );
			agent = (TestAgent) factory.createAgent(TestAgent.class, agentId);
		}
		xmppService.connect(agentId, agentId, agentPassword);

		// instantiate an agent
		agentId = "gloria";
		agentPassword = "gloria";
		Agent agent2 = factory.getAgent(agentId);
		if (agent2 == null) {
			System.out.println("Create agent " + agentId );
			agent2 = factory.createAgent(TestAgent.class, agentId);
		}
		xmppService.connect(agentId, agentId, agentPassword);
		
        //agent.testAsyncXMPP("xmpp:jos@ec2-54-246-112-19.eu-west-1.compute.amazonaws.com");
        //agent.testAsyncXMPP("xmpp:gloria@ec2-54-246-112-19.eu-west-1.compute.amazonaws.com");
        //agent.testAsyncXMPP("xmpp:jos@openid.almende.org");
        //agent.testAsyncXMPP("xmpp:gloria@openid.almende.org");
        //agent.testGetContacts("xmpp:contact1@ec2-54-246-112-19.eu-west-1.compute.amazonaws.com");
        //agent.testGetContacts("xmpp:contact1@ec2-54-246-112-19.eu-west-1.compute.amazonaws.com");
        //agent.testAsyncHTTP();
        agent.testSyncXMPP("xmpp:jos@openid.almende.org");
        
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
