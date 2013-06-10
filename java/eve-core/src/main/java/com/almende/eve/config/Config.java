package com.almende.eve.config;

import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.logging.Logger;

public class Config extends YamlConfig {
	private static final Logger	LOG					= Logger.getLogger(Config.class
															.getCanonicalName());
	private static String		ENVIRONMENT_PATH[]	= new String[] {
			"com.google.appengine.runtime.environment",
			"com.almende.eve.runtime.environment"	};
	private static String		environment			= null;
	
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
		envParams.add(getEnvironment());
		envParams.addAll(Arrays.asList(params));
		T result = super.get(envParams.toArray(new String[0]));
		if (result == null) {
			result = super.get(params);
		}
		return result;
	}
	
	public static String getEnvironment() {
		if (environment == null) {
			for (String path : ENVIRONMENT_PATH) {
				environment = System.getProperty(path);
				if (environment != null) {
					LOG.info("Current environment: '" + environment
							+ "' (read from path '" + path + "')");
					break;
				}
			}
			
			if (environment == null) {
				// no environment variable found. Fall back to "Production"
				environment = "Production";
				
				String msg = "No environment variable found. "
						+ "Environment set to '" + environment
						+ "'. Checked paths: ";
				for (String path : ENVIRONMENT_PATH) {
					msg += path + ", ";
				}
				LOG.warning(msg);
			}
		}
		
		return environment;
	}
	public static final void setEnvironment(String env) {
		environment = env;
	}
}
