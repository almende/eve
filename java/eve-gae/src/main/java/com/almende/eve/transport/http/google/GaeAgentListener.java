package com.almende.eve.transport.http.google;

import java.util.concurrent.ThreadFactory;

import javax.servlet.ServletContextEvent;

import com.almende.eve.config.Config;
import com.almende.eve.transport.http.AgentListener;
import com.google.appengine.api.ThreadManager;


public class GaeAgentListener extends AgentListener {
	
	/** 
	 * Set Google App Engine specific ThreadFactory, then initialize the listener
	 */
	@Override
	public void contextInitialized(final ServletContextEvent sce) {
		ThreadFactory factory = ThreadManager.currentRequestThreadFactory();
		Config.setThreadFactory(factory);
		
		super.contextInitialized(sce);
	}
}
