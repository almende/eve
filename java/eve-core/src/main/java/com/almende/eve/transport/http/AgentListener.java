package com.almende.eve.transport.http;

import java.util.Map.Entry;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.ServletRegistration;

import com.almende.eve.agent.AgentHost;
import com.almende.eve.config.Config;

public class AgentListener implements ServletContextListener {
	private static final Logger		LOG	= Logger.getLogger(AgentListener.class
												.getSimpleName());
	private static ServletContext	c;
	
	@Override
	public void contextInitialized(final ServletContextEvent sce) {
		c = sce.getServletContext();
		init(c);
	}
	
	protected static String getParam(final String param) {
		return getParam(param, null);
	}
	
	protected static String getParam(final String param, final String defaultVal) {
		String result = c.getInitParameter(param);
		if (result == null && c.getMajorVersion() >= 3) {
			for (final Entry<String, ? extends ServletRegistration> ent : c
					.getServletRegistrations().entrySet()) {
				result = ent.getValue().getInitParameter(param);
				if (result != null) {
					LOG.warning("Context param '" + param
							+ "' should be migrated to <context-param>'");
					break;
				}
			}
		}
		if (result == null && c.getMajorVersion() < 3) {
			LOG.warning("Eve configuration in Servlet variables works only in Servlet 3+ (and is deprecated in that situation.)");
		}
		if (result == null && defaultVal != null) {
			
			result = defaultVal;
			LOG.warning("Context parameter '" + param
					+ "' missing in servlet configuration web.xml. "
					+ "Trying default value '" + defaultVal + "'.");
		}
		return result;
	}
	
	public static void init(final ServletContext ctx) {
		
		if (ctx != null) {
			c = ctx;
			
			String filename = getParam("eve_config");
			if (filename == null) {
				// TODO: config param is deprecated since v2.0. Cleanup some day
				filename = getParam("config");
				if (filename == null) {
					// fall back to default value
					filename = "eve.yaml";
					LOG.warning("Context param \"eve_config\" missing. Using default value '"
							+ filename + "'.");
				} else {
					LOG.warning("Context param \"config\" is deprecated. Use \"eve_config\" instead.");
				}
			}
			
			final String fullname = "/WEB-INF/" + filename;
			
			LOG.info("loading configuration file '" + c.getRealPath(fullname)
					+ "'...");
			
			final Config config = new Config(c.getResourceAsStream(fullname));
			try {
				AgentHost.getInstance().loadConfig(config);
			} catch (final Exception e) {
				LOG.log(Level.WARNING, "", e);
			}
		}
	}
	
	@Override
	public void contextDestroyed(final ServletContextEvent sce) {
	}
	
}
