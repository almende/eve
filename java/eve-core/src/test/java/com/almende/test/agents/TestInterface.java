package com.almende.test.agents;

import com.almende.eve.agent.AgentInterface;

public interface TestInterface extends AgentInterface {
	public String helloWorld(String msg);
	public void testVoid();
	public int testPrimitive(int num,Integer num2);
}
