/*
 * Copyright: Almende B.V. (2014), Rotterdam, The Netherlands
 * License: The Apache Software License, Version 2.0
 */
package com.almende.eve.agent.example;

import com.almende.eve.rpc.annotation.Access;
import com.almende.eve.rpc.annotation.AccessType;

/**
 * The Class ExampleAspect.
 */
public class ExampleAspect {
	
	/**
	 * Hello world.
	 *
	 * @return the string
	 */
	@Access(AccessType.PUBLIC)
	public String helloWorld() {
		return "Hello World";
	}
}
