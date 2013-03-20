package com.almende.eve.transport.http;

import java.util.Map.Entry;
import java.util.logging.Logger;

import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.ServletRegistration;

import com.almende.eve.agent.AgentFactory;
import com.almende.eve.config.Config;

public class AgentListener implements ServletContextListener {
	private final static Logger logger = Logger.getLogger(AgentListener.class
			.getSimpleName());
	private static ServletContext c;
	
	@Override
	public void contextInitialized(ServletContextEvent sce) {
		c = sce.getServletContext();
		init(c);
	}

	protected static String getParam(String param){
		return getParam(param,null);
	}
	protected static String getParam(String param, String default_val){
		String result = c.getInitParameter(param);
		if (result == null) {
			for (Entry<String, ? extends ServletRegistration> ent : c.getServletRegistrations().entrySet()){
				result = ent.getValue().getInitParameter(param);
				if (result != null){
					logger.warning("Init param '"+param+"' should be migrated to <context-param>'");
					break;
				}
			}
		}
		if (result == null && default_val != null) {
			
			result = default_val;
			logger.warning("Init parameter '"+param+"' missing in servlet configuration web.xml. "
					+ "Trying default value '" + default_val + "'.");
		}
		return result;
	}
	
	public static void init(ServletContext ctx) {
		if (ctx != null) {
			c=ctx;
			
			String filename = getParam("config","eve.yaml");
			
			String fullname = "/WEB-INF/" + filename;

			logger.info("loading configuration file '"
					+ c.getRealPath(fullname) + "'...");

			Config config = new Config(c.getResourceAsStream(fullname));
			try {
				AgentFactory.createInstance(config);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	@Override
	public void contextDestroyed(ServletContextEvent sce) {
	}

}
