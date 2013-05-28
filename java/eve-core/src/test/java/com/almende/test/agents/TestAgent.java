package com.almende.test.agents;

import com.almende.eve.agent.Agent;
import com.almende.eve.rpc.annotation.Access;
import com.almende.eve.rpc.annotation.AccessType;

@Access(AccessType.PUBLIC)
public class TestAgent extends Agent implements TestInterface {

	@Override
	public String helloWorld(String msg) {
		return "Hello world, you said: "+msg;
	}

	@Override
	public void testVoid() {
		System.out.println("testVoid called!");
	}

	@Override
	public int testPrimitive(int num, Integer num2) {
		return num+num2;
	}

	@Override
	public String getDescription() {
		return "Simple TestAgent, returning some data for Proxy test";
	}

	@Override
	public String getVersion() {
		return "1.0";
	}

}
