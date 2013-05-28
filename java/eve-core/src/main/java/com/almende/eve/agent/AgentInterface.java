package com.almende.eve.agent;

import java.util.List;

import com.almende.eve.agent.annotation.Namespace;
import com.almende.eve.event.EventsInterface;
import com.almende.eve.monitor.ResultMonitorInterface;
import com.almende.eve.rpc.jsonrpc.JSONAuthorizor;
import com.almende.eve.scheduler.Scheduler;

public interface AgentInterface extends JSONAuthorizor {
	/**
	 * Retrieve the agents id
	 * 
	 * @return id
	 */
	public String getId();
	
	/**
	 * Retrieve the agents type (its simple class name)
	 * 
	 * @return version
	 */
	public String getType();
	
	/**
	 * Retrieve the agents version number
	 * 
	 * @return version
	 */
	public String getVersion();
	
	/**
	 * Retrieve a description of the agents functionality
	 * 
	 * @return description
	 */
	public String getDescription();
	
	/**
	 * Retrieve an array with the agents urls (can be one or multiple), and
	 * depends on the configured transport services.
	 * 
	 * @return urls
	 */
	public List<String> getUrls();
	

	/**
	 * Get the scheduler to schedule tasks for the agent to be executed later
	 * on.
	 * 
	 */
	@Namespace("scheduler")
	public Scheduler getScheduler();
	
	/**
	 * Get the resultMonitorFactory, which can be used to register push/poll RPC
	 * result monitors.
	 */
	@Namespace("monitor")
	public ResultMonitorInterface getResultMonitorFactory();
	
	/**
	 * Get the eventsFactory, which can be used to subscribe and trigger events.
	 */
	@Namespace("event")
	public EventsInterface getEventsFactory();
	
	/**
	 * Retrieve a list with all the available methods.
	 * 
	 * @return methods
	 */
	public List<Object> getMethods();
	
}
