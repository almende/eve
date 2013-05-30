package com.almende.test.agents;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import com.almende.eve.agent.Agent;
import com.almende.eve.rpc.annotation.Access;
import com.almende.eve.rpc.annotation.AccessType;
import com.almende.test.agents.entity.Person;

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

	public HashMap<String, List<Person>> complexResult() {
		HashMap<String,List<Person>> result = new HashMap<String,List<Person>>();
		List<Person> list = new ArrayList<Person>();
		Person test= new Person();
		test.setName("Ludo");
		list.add(test);

		result.put("result", list);
		
		return result;
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
