/*
 * Copyright: Almende B.V. (2014), Rotterdam, The Netherlands
 * License: The Apache Software License, Version 2.0
 */
package com.almende.test.agents;

import java.util.HashMap;
import java.util.List;

import com.almende.eve.agent.AgentInterface;
import com.almende.eve.rpc.annotation.Name;
import com.almende.test.agents.entity.Person;

/**
 * The Interface TestInterface.
 */
public interface TestInterface extends AgentInterface {
	
	/**
	 * Hello world.
	 *
	 * @param msg the msg
	 * @return the string
	 */
	public String helloWorld(@Name("msg") String msg);
	
	/**
	 * Test void.
	 */
	public void testVoid();
	
	/**
	 * Test primitive.
	 *
	 * @param num the num
	 * @param num2 the num2
	 * @return the int
	 */
	public int testPrimitive(@Name("num") int num, @Name("num2") Integer num2);
	
	/**
	 * Complex result.
	 *
	 * @return the hash map
	 */
	public HashMap<String, List<Person>> complexResult();
}
