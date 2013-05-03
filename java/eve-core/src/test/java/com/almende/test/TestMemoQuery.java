package com.almende.test;

import junit.framework.TestCase;

import org.junit.Test;

import com.almende.eve.agent.AgentFactory;
import com.almende.test.agents.TestMemoQueryAgent;

public class TestMemoQuery extends TestCase {
	
	@Test
	public void test()  throws Exception {
		AgentFactory factory = AgentFactory.getInstance();
		if (factory == null){
			factory = AgentFactory.createInstance();
		}

		if (factory.hasAgent("alice")) factory.deleteAgent("alice");
		if (factory.hasAgent("bob"))factory.deleteAgent("bob");

		TestMemoQueryAgent alice = factory.createAgent(TestMemoQueryAgent.class, "alice");
		factory.createAgent(TestMemoQueryAgent.class, "bob");
		
		alice.prepare();
		
		System.out.println("0 Alice, from bob:"+alice.get_result());
		int count=0;
		while (count++ < 10){
			Thread.sleep(1000);
			System.out.println(count+" Alice, from Bob:"+alice.get_result());
		}
		
		alice.tear_down();
		
		factory.deleteAgent("alice");
		factory.deleteAgent("bob");
	}
	
}
