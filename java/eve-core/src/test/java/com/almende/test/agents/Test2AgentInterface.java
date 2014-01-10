/*
 * Copyright: Almende B.V. (2014), Rotterdam, The Netherlands
 * License: The Apache Software License, Version 2.0
 */
package com.almende.test.agents;

import com.almende.eve.agent.AgentInterface;
import com.almende.eve.rpc.annotation.Name;

/**
 * The Interface Test2AgentInterface.
 */
public interface Test2AgentInterface extends AgentInterface {
	
	/**
	 * Adds the.
	 *
	 * @param a the a
	 * @param b the b
	 * @return the double
	 */
	public Double add(@Name("a") Double a, @Name("b") Double b);
	
	/**
	 * Multiply.
	 *
	 * @param a the a
	 * @param b the b
	 * @return the double
	 */
	public Double multiply(@Name("a") Double a, @Name("b") Double b);
	
	/**
	 * Increment.
	 *
	 * @return the double
	 */
	public Double increment();
	
	/**
	 * The Enum STATUS.
	 */
	public enum STATUS {
		
		/** The good. */
		GOOD, 
 /** The bad. */
 BAD, 
 /** The ok. */
 OK, 
 /** The wrong. */
 WRONG, 
 /** The failed. */
 FAILED, 
 /** The success. */
 SUCCESS
	};
	
	/**
	 * Test enum.
	 *
	 * @param status the status
	 * @return the status
	 */
	public STATUS testEnum(@Name("status") STATUS status);
	
	/**
	 * Test void.
	 */
	public void testVoid();
}
