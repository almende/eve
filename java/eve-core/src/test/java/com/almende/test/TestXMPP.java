package com.almende.test;

import junit.framework.TestCase;

import org.junit.Test;

import com.almende.eve.agent.Agent;
import com.almende.eve.agent.AgentHost;
import com.almende.eve.state.FileStateFactory;
import com.almende.eve.transport.xmpp.XmppService;
import com.almende.test.agents.Test2Agent;

public class TestXMPP extends TestCase {

	@Test
	public void testXMPP() throws Exception {
		//Create TestAgent according to TestInterface
		AgentHost agentHost = AgentHost.getInstance();
		FileStateFactory stateFactory = new FileStateFactory(".eveagents");
		agentHost.setStateFactory(stateFactory);

		String host = "openid.almende.org";
		int port = 5222;
		String serviceName = host;
		XmppService xmppService = new XmppService(agentHost, host, port, serviceName);
		agentHost.addTransportService(xmppService);
		
		agentHost.setDoesShortcut(false);
		
		// instantiate an agent and connect it to a messenger service
		String agentId = "alex";
		String agentPassword = "alex";
		Test2Agent agent = (Test2Agent)agentHost.getAgent(agentId);
		if (agent == null) {
			System.out.println("Create agent " + agentId );
			agent = (Test2Agent) agentHost.createAgent(Test2Agent.class, agentId);
		}
		xmppService.disconnect(agentId);
		xmppService.connect(agentId, agentId, agentPassword);

		// instantiate an agent
		agentId = "gloria";
		agentPassword = "gloria";
		Agent agent2 = agentHost.getAgent(agentId);
		if (agent2 == null) {
			System.out.println("Create agent " + agentId );
			agent2 = agentHost.createAgent(Test2Agent.class, agentId);
		}
		xmppService.disconnect(agentId);
		xmppService.connect(agentId, agentId, agentPassword);

		agent.testAsyncXMPP(xmppService.getAgentUrl(agent2.getId()));

		Thread.sleep(2000);
		xmppService.disconnect("gloria");
		xmppService.disconnect("alex");
		
		agentHost.setDoesShortcut(true);
	}

}
