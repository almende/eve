/*
 * Copyright: Almende B.V. (2014), Rotterdam, The Netherlands
 * License: The Apache Software License, Version 2.0
 */
package com.almende.eve.config;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.Map;
import java.util.logging.Logger;

import org.yaml.snakeyaml.Yaml;

/**
 * The Class YamlConfig.
 */
public class YamlConfig {
	private final Logger		logger	= Logger.getLogger(this.getClass()
												.getSimpleName());
	private Map<String, Object>	config	= null;
	
	/**
	 * Instantiates a new yaml config.
	 */
	public YamlConfig() {
	}
	
	/**
	 * Load the configuration file by filename (absolute path)
	 * Default filename is /WEB-INF/eve.yaml
	 *
	 * @param filename the filename
	 * @throws FileNotFoundException the file not found exception
	 */
	public YamlConfig(final String filename) throws FileNotFoundException {
		load(filename);
	}
	
	/**
	 * Load the configuration file from input stream.
	 *
	 * @param inputStream the input stream
	 */
	public YamlConfig(final InputStream inputStream) {
		load(inputStream);
	}
	
	/**
	 * Load the configuration from a map.
	 *
	 * @param config the config
	 */
	public YamlConfig(final Map<String, Object> config) {
		this.config = config;
	}
	
	/**
	 * Load the configuration file by filename (absolute path)
	 * Default filename is /WEB-INF/eve.yaml
	 *
	 * @param filename the filename
	 * @throws FileNotFoundException the file not found exception
	 */
	public final void load(final String filename) throws FileNotFoundException {
		final File file = new File(filename);
		logger.info("Loading configuration file " + file.getAbsoluteFile()
				+ "...");
		
		final FileInputStream in = new FileInputStream(filename);
		load(in);
	}
	
	/**
	 * Load the configuration file from input stream.
	 *
	 * @param inputStream the input stream
	 */
	@SuppressWarnings("unchecked")
	public final void load(final InputStream inputStream) {
		final Yaml yaml = new Yaml();
		config = yaml.loadAs(inputStream, Map.class);
	}
	
	/**
	 * Get the full configuration
	 * returns null if no configuration file is loaded.
	 *
	 * @return the map
	 */
	public Map<String, Object> get() {
		return config;
	}
	
	/**
	 * retrieve a (nested) parameter from the config
	 * the parameter name can be a simple name like config.get("url"),
	 * or nested parameter like config.get("servlet", "config", "url")
	 * null is returned when the parameter is not found, or when no
	 * configuration file is loaded.
	 *
	 * @param <T> the generic type
	 * @param params One or multiple (nested) parameters
	 * @return the t
	 */
	@SuppressWarnings("unchecked")
	public <T> T get(final String... params) {
		if (config == null) {
			return null;
		}
		
		Map<String, Object> c = config;
		for (int i = 0; i < params.length - 1; i++) {
			final String key = params[i];
			// FIXME: check instance
			c = (Map<String, Object>) c.get(key);
			if (c == null) {
				return null;
			}
		}
		
		// FIXME: check instance
		return (T) c.get(params[params.length - 1]);
	}
}
