package com.almende.eve.config;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.logging.Logger;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;

import org.yaml.snakeyaml.Yaml;

public class Config {
	private Logger logger = Logger.getLogger(this.getClass().getSimpleName());
	Map<String, Object> config = null;
	
	public Config() {}

	/**
	 * Load the configuration file
	 * The filename is read from the init-parameter "config" in the servlet
	 * configuration, and is supposed to be located in war/WEB-INF/
	 * @param servlet
	 * @return
	 * @throws ServletException 
	 * @throws IOException 
	 */
	public Config(HttpServlet servlet) throws ServletException, IOException {
		load(servlet);
	}
	
	/**
	 * Load the configuration file by filename (absolute path)
	 * @param filename
	 * @return
	 * @throws FileNotFoundException 
	 */
	public Config(String filename) throws ServletException, IOException {
		load(filename);
	}
	
	/**
	 * Load the configuration file from input stream
	 * @param filename
	 * @return
	 * @throws FileNotFoundException 
	 */
	public Config(InputStream inputStream) throws ServletException, IOException {
		load(inputStream);
	}
	
	/**
	 * Load the configuration file
	 * The filename is read from the init-parameter "config" in the servlet
	 * configuration, and is supposed to be located in war/WEB-INF/
	 * @param servlet
	 * @return
	 * @throws ServletException 
	 * @throws IOException 
	 */
	public void load(HttpServlet servlet) throws ServletException, IOException {
		if (servlet == null) {
			throw new ServletException("No servlet specified to load configuration");
		}
		
		String filename = servlet.getInitParameter("config");
		if (filename == null) {
			filename = "eve.yaml";
			logger.warning(
				"Init parameter 'config' missing in servlet configuration web.xml. " +
				"Trying default filename '" + filename + "'.");
		}
		
		String fullname = "/WEB-INF/" + filename;
		logger.info("Loading configuration file " + fullname);
		
		InputStream in = servlet.getServletContext().getResourceAsStream(fullname);
		load(in);
	}
	
	/**
	 * Load the configuration file by filename (absolute path)
	 * @param filename
	 * @return
	 * @throws FileNotFoundException 
	 */
	public void load(String filename) throws FileNotFoundException{
		File file = new File(filename);
		logger.info("Loading configuration file " + file.getAbsoluteFile());

		FileInputStream in = new FileInputStream(filename);
		load(in);
	}
	
	/**
	 * Load the configuration file from input stream
	 * @param filename
	 * @return
	 * @throws FileNotFoundException 
	 */
	@SuppressWarnings("unchecked")
	public void load(InputStream inputStream) {
		Yaml yaml = new Yaml();
		config = yaml.loadAs(inputStream, Map.class);
	}
	
	/**
	 * Get the full configuration
	 * returns null if no configuration file is loaded
	 * @return
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
	 * @param params    One or multiple (nested) parameters
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public <T> T get(String ... params) {
		if (config == null) {
			return null;
		}
		
		Map<String, Object> c = config;
		for (int i = 0; i < params.length - 1; i++) {
			String key = params[i];
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
