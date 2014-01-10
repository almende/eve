/*
 * Copyright: Almende B.V. (2014), Rotterdam, The Netherlands
 * License: The Apache Software License, Version 2.0
 */
package com.almende.eve.config;

import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

/**
 * The Class Config.
 */
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
		LABELS.put("couchdbstatefactory",
				"com.almende.eve.state.couchdb.CouchDBStateFactory");
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
		LABELS.put("zmqservice", "com.almende.eve.transport.zmq.ZmqService");
	}
	
	/**
	 * Instantiates a new config.
	 */
	public Config() {
		super();
	}
	
	/**
	 * Instantiates a new config.
	 *
	 * @param filename the filename
	 * @throws FileNotFoundException the file not found exception
	 */
	public Config(final String filename) throws FileNotFoundException {
		super(filename);
	}
	
	/**
	 * Instantiates a new config.
	 *
	 * @param inputStream the input stream
	 */
	public Config(final InputStream inputStream) {
		super(inputStream);
	}
	
	/* (non-Javadoc)
	 * @see com.almende.eve.config.YamlConfig#get(java.lang.String[])
	 */
	@Override
	@SuppressWarnings("unchecked")
	public <T> T get(final String... params) {
		final ArrayList<String> envParams = new ArrayList<String>(params.length + 2);
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
	
	/**
	 * Map.
	 *
	 * @param result the result
	 * @return the string
	 */
	public static String map(String result) {
		if (LABELS.containsKey(result.toLowerCase())) {
			result = LABELS.get(result.toLowerCase());
		}
		return result;
	}
	
	/**
	 * Gets the environment.
	 *
	 * @return the environment
	 */
	public static String getEnvironment() {
		if (environment == null) {
			for (final String path : ENVIRONMENTPATH) {
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
				for (final String path : ENVIRONMENTPATH) {
					msg += path + ", ";
				}
				LOG.warning(msg);
			}
		}
		
		return environment;
	}
	
	/**
	 * Sets the environment.
	 *
	 * @param env the new environment
	 */
	public static final void setEnvironment(final String env) {
		environment = env;
	}
}
