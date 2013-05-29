package com.almende.test.agents;

import java.util.List;
import java.util.Map;

import com.almende.eve.agent.AgentInterface;
import com.almende.eve.rpc.annotation.Name;

public interface TestInterface extends AgentInterface {
	public String helloWorld(@Name("msg") String msg);
	public void testVoid();
	public int testPrimitive(@Name("num") int num,@Name("num2") Integer num2);
	public Map<String, List<Double>> complexResult();
}
