package com.almende.test;

import java.net.URI;
import java.util.logging.Logger;

import junit.framework.TestCase;

import org.junit.Test;

import com.almende.eve.agent.AgentHost;
import com.almende.eve.state.FileStateFactory;
import com.almende.test.agents.Test2Agent;
import com.almende.test.agents.Test2AgentInterface;

public class TestAgentHost extends TestCase {
	static final Logger	log	= Logger.getLogger("testAgentHost");
	
	@Test
	public void testAgentCall() throws Exception {
		final String TESTAGENT = "hostTestAgent";
		
		log.warning(this.getClass().getName() + ":"+this.getClass().getClassLoader().hashCode());
		AgentHost host = AgentHost.getInstance();
		FileStateFactory stateFactory = new FileStateFactory(".eveagents");
		host.setStateFactory(stateFactory);

		if (host.hasAgent(TESTAGENT)){
			host.deleteAgent(TESTAGENT);
		}
		host.createAgent(Test2Agent.class, TESTAGENT);
		
		log.warning("Creating agentProxy");
		Test2AgentInterface agent = host.createAgentProxy(null, 
				URI.create("local:"+TESTAGENT), 
				Test2AgentInterface.class);
		log.warning("Starting agentProxy");
		Double res = agent.add(3.1, 4.2);
		//result not exact due to intermediate binary representation
		assertEquals(new Double(7.300000000000001),res);
		log.warning("AgentProxy add done");
		res = agent.multiply(3.1, 4.2);
		assertEquals(new Double(13.020000000000001),res);
		System.err.println("AgentProxy multiply done");
		
		agent = host.createAgentProxy(null, 
				URI.create("https://localhost:8443/agents/"+TESTAGENT+"/"), 
				Test2AgentInterface.class);
		
		log.warning("checking local https call 1:");
		res = agent.add(3.1, 4.2);
		//result not exact due to intermediate binary representation
		assertEquals(new Double(7.300000000000001),res); 
		
		log.warning("checking local https call 2:");
		res = agent.multiply(3.1, 4.2);
		assertEquals(new Double(13.020000000000001),res);
		log.warning("Done");
		
		host.deleteAgent(TESTAGENT);
	}
	
}
