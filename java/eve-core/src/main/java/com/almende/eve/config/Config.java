package com.almende.eve.config;

import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;

import com.almende.eve.agent.AgentFactory;

public class Config extends YamlConfig {
	
	public Config() {
		super();
	}
	
	public Config(String filename) throws FileNotFoundException {
		super(filename);
	}
	
	public Config(InputStream inputStream) {
		super(inputStream);
	}
	
	public <T> T get(String... params) {
		ArrayList<String> envParams = new ArrayList<String>(params.length + 2);
		envParams.add("environment");
		// TODO: remove dependency on AgentFactory, invert environment
		// addition(AgentFactory injects this i.s.o. being pulled from here.
		envParams.add(AgentFactory.getEnvironment());
		envParams.addAll(Arrays.asList(params));
		T result = super.get(envParams.toArray(new String[0]));
		if (result == null) {
			result = super.get(params);
		}
		return result;
	}
}
