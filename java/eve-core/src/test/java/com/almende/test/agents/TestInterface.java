package com.almende.test.agents;

import java.util.HashMap;
import java.util.List;

import com.almende.eve.agent.AgentInterface;
import com.almende.eve.rpc.annotation.Name;
import com.almende.test.agents.entity.Person;

public interface TestInterface extends AgentInterface {
	public String helloWorld(@Name("msg") String msg);
	public void testVoid();
	public int testPrimitive(@Name("num") int num,@Name("num2") Integer num2);
	public HashMap<String, List<Person>> complexResult();
}
