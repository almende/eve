package com.almende.eve.config;

import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

public class Config extends YamlConfig {
	// TODO: https://github.com/mojombo/toml
	private static final Logger					LOG					= Logger.getLogger(Config.class
																			.getCanonicalName());
	private static final String					ENVIRONMENTPATH[]	= new String[] {
			"com.google.appengine.runtime.environment",
			"com.almende.eve.runtime.environment"					};
	private static String						environment			= null;
	/*
	 * Several classname maps for configuration conveniency:
	 */
	private static final Map<String, String>	LABELS				= new HashMap<String, String>();
	static {
		LABELS.put("filestatefactory", "com.almende.eve.state.FileStateFactory");
		LABELS.put("memorystatefactory",
				"com.almende.eve.state.MemoryStateFactory");
		LABELS.put("datastorestatefactory",
				"com.almende.eve.state.google.DatastoreStateFactory");
		LABELS.put("runnableschedulerfactory",
				"com.almende.eve.scheduler.RunnableSchedulerFactory");
		LABELS.put("clockschedulerfactory",
				"com.almende.eve.scheduler.ClockSchedulerFactory");
		LABELS.put("gaeschedulerfactory",
				"com.almende.eve.scheduler.google.GaeSchedulerFactory");
		LABELS.put("xmppservice", "com.almende.eve.transport.xmpp.XmppService");
		LABELS.put("httpservice", "com.almende.eve.transport.http.HttpService");
	}
	
	public Config() {
		super();
	}
	
	public Config(String filename) throws FileNotFoundException {
		super(filename);
	}
	
	public Config(InputStream inputStream) {
		super(inputStream);
	}
	
	@SuppressWarnings("unchecked")
	public <T> T get(String... params) {
		ArrayList<String> envParams = new ArrayList<String>(params.length + 2);
		envParams.add("environment");
		envParams.add(getEnvironment());
		envParams.addAll(Arrays.asList(params));
		T result = super.get(envParams.toArray(new String[0]));
		if (result == null) {
			result = super.get(params);
		}

		if (result != null && String.class.isAssignableFrom(result.getClass())) {
			result = (T) map((String) result);
		}
		return result;
	}
	
	public static String map(String result){
		if (LABELS.containsKey(result.toLowerCase())) {
			result = LABELS.get(result.toLowerCase());
		}
		return result;
	}
	
	public static String getEnvironment() {
		if (environment == null) {
			for (String path : ENVIRONMENTPATH) {
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
				for (String path : ENVIRONMENTPATH) {
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
