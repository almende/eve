package com.almende.eve.agent.example;

import com.almende.eve.rpc.annotation.Access;
import com.almende.eve.rpc.annotation.AccessType;

public class ExampleAspect {
	
	@Access(AccessType.PUBLIC)
	public String helloWorld() {
		return "Hello World";
	}
}
