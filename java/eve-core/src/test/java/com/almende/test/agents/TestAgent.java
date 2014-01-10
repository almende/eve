/*
 * Copyright: Almende B.V. (2014), Rotterdam, The Netherlands
 * License: The Apache Software License, Version 2.0
 */
package com.almende.test.agents;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import com.almende.eve.agent.Agent;
import com.almende.eve.rpc.annotation.Access;
import com.almende.eve.rpc.annotation.AccessType;
import com.almende.test.agents.entity.Person;

/**
 * The Class TestAgent.
 */
@Access(AccessType.PUBLIC)
public class TestAgent extends Agent implements TestInterface {
	
	/* (non-Javadoc)
	 * @see com.almende.test.agents.TestInterface#helloWorld(java.lang.String)
	 */
	@Override
	public String helloWorld(final String msg) {
		return "Hello world, you said: " + msg;
	}
	
	/* (non-Javadoc)
	 * @see com.almende.test.agents.TestInterface#testVoid()
	 */
	@Override
	public void testVoid() {
		System.out.println("testVoid called!");
	}
	
	/* (non-Javadoc)
	 * @see com.almende.test.agents.TestInterface#testPrimitive(int, java.lang.Integer)
	 */
	@Override
	public int testPrimitive(final int num, final Integer num2) {
		return num + num2;
	}
	
	/* (non-Javadoc)
	 * @see com.almende.test.agents.TestInterface#complexResult()
	 */
	@Override
	public HashMap<String, List<Person>> complexResult() {
		final HashMap<String, List<Person>> result = new HashMap<String, List<Person>>();
		final List<Person> list = new ArrayList<Person>();
		final Person test = new Person();
		test.setName("Ludo");
		list.add(test);
		
		result.put("result", list);
		
		return result;
	}
	
	/* (non-Javadoc)
	 * @see com.almende.eve.agent.Agent#getDescription()
	 */
	@Override
	public String getDescription() {
		return "Simple TestAgent, returning some data for Proxy test";
	}
	
	/* (non-Javadoc)
	 * @see com.almende.eve.agent.Agent#getVersion()
	 */
	@Override
	public String getVersion() {
		return "1.0";
	}
	
}
