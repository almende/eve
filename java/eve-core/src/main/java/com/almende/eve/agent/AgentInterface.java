package com.almende.eve.agent;

import java.lang.reflect.Type;
import java.net.URI;
import java.util.List;

import com.almende.eve.agent.annotation.Namespace;
import com.almende.eve.agent.proxy.AsyncProxy;
import com.almende.eve.event.EventsInterface;
import com.almende.eve.monitor.ResultMonitorInterface;
import com.almende.eve.rpc.jsonrpc.JSONAuthorizor;
import com.almende.eve.rpc.jsonrpc.JSONRequest;
import com.almende.eve.scheduler.Scheduler;
import com.almende.eve.state.State;
import com.almende.eve.transport.AsyncCallback;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.node.ObjectNode;

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
	 * Get the state of this agent. Get the agents state. The state contains
	 * methods get, put, etc. to write properties into a persistent state.
	 * 
	 * 
	 */
	public State getState();
	
	/**
	 * Get the associated agentFactory of this agent
	 * 
	 */
	public AgentFactory getAgentFactory();
	
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
	
	/**
	 * This method is called once in the life time of an agent, at the moment
	 * the agent is being created by the AgentFactory.
	 * It can be overridden and used to perform some action when the agent
	 * is create, in that case super.create() should be called in
	 * the overridden create().
	 */
	public void create();
	
	/**
	 * This method is called once in the life time of an agent, at the moment
	 * the agent is being deleted by the AgentFactory.
	 * It can be overridden and used to perform some action when the agent
	 * is deleted, in that case super.delete() should be called in
	 * the overridden delete().
	 */
	public void delete();
	
	/**
	 * This method is called when the containing AgentFactory is started.
	 * It can be overridden and used to perform some action (like alerting
	 * owners about the reboot),
	 * in that case super.boot() should be called in
	 * the overridden boot().
	 * 
	 * @throws Exception
	 */
	public void boot() throws Exception;
	
	/**
	 * This method is called directly after the agent and its state is
	 * initiated.
	 * It can be overridden and used to perform some action when the agent
	 * is initialized, in that case super.init() should be called in
	 * the overridden init().
	 */
	public void init();
	
	/**
	 * This method can is called when the agent is uninitialized, and is
	 * needed finalize the state of the agent.
	 * It can be overridden and used to perform some action when the agent
	 * is uninitialized, in that case super.destroy() should be called in
	 * the overridden destroy().
	 */
	public void destroy();
	
	/**
	 * Get the first url of the agents urls. Returns local://<agentId> if the
	 * agent does not
	 * have any urls.
	 * 
	 * @return firstUrl
	 */
	public URI getFirstUrl();
	
	/**
	 * Do a RPC call to another agent.
	 * 
	 * @deprecated Please use one of the versions with can handle the more
	 *             complex generics returntypes. (e.g.
	 *             send(url,method,params,JOM.getSimpleType(type));)
	 * 
	 * @param url
	 * @param method
	 * @param params
	 * @param type
	 * @return
	 * @throws Exception
	 */
	@Deprecated
	public <T> T send(URI url, String method, Object params, Class<T> type)
			throws Exception;
	
	/**
	 * Do a RPC call to another agent.
	 * 
	 * @param url
	 * @param method
	 * @param params
	 * @param type
	 *            returntype
	 * @return
	 * @throws Exception
	 */
	public <T> T send(URI url, String method, Object params, Type type)
			throws Exception;
	
	/**
	 * Do a RPC call to another agent.
	 * 
	 * @param url
	 * @param method
	 * @param params
	 * @param type
	 *            returntype
	 * @return
	 * @throws Exception
	 */
	public <T> T send(URI url, String method, Object params, JavaType type)
			throws Exception;
	
	/**
	 * Do a RPC call to another agent.
	 * 
	 * @param ret  Object to put result in, will also be returned
	 * @param url
	 * @param method
	 * @param params
	 * @return
	 * @throws Exception
	 */
	public <T> T send(T ret, URI url, String method, Object params) throws Exception;


	/**
	 * Do a RPC call to another agent.
	 * 
	 * @param ret  Object to put result in, will also be returned
	 * @param url
	 * @param method
	 * @return ret
	 * @throws Exception
	 */
	public <T> T send(T ret, URI url, String method) throws Exception;

	
	/**
	 * Do a RPC call to another agent.
	 * 
	 * @param url
	 * @param method
	 * @param type
	 * @return
	 * @throws Exception
	 */
	public <T> T send(URI url, String method, Type type) throws Exception;
	
	/**
	 * Do a RPC call to another agent.
	 * 
	 * @param url
	 * @param method
	 * @param type
	 * @return
	 * @throws Exception
	 */
	public <T> T send(URI url, String method, JavaType type)
			throws Exception;
	
	
	/**
	 * Do a RPC call to another agent.
	 * 
	 * @deprecated Please use one of the versions with can handle the more
	 *             complex generics returntypes. (e.g.
	 *             send(url,method,JOM.getSimpleType(type));)
	 * 
	 * @param url
	 * @param method
	 * @param type
	 * @return
	 * @throws Exception
	 */
	@Deprecated
	public <T> T send(URI url, String method, Class<T> type)
			throws Exception;
	
	/**
	 * Do a RPC call to another agent, expecting no result (void)
	 * 
	 * @param url
	 * @param method
	 * @param params
	 * @throws Exception
	 */
	public void send(URI url, String method, Object params) throws Exception;
	
	/**
	 * Do a RPC call to another agent, expecting no result (void)
	 * 
	 * @param url
	 * @param method
	 * @throws Exception
	 */
	public void send(URI url, String method) throws Exception;
	
	/**
	 * Do an asynchronous RPC call to another agent.
	 * 
	 * @deprecated Please use one of the versions with can handle the more
	 *             complex generics returntypes. (e.g.
	 *             sendAsync(url,method,params
	 *             ,callback,JOM.getSimpleType(type));)
	 * 
	 * @param url
	 * @param method
	 * @param params
	 * @param callback
	 * @param type
	 * @throws Exception
	 */
	@Deprecated
	public <T> void sendAsync(URI url, String method, ObjectNode params,
			final AsyncCallback<T> callback, Class<T> type) throws Exception;
	
	/**
	 * Do an asynchronous RPC call to another agent.
	 * 
	 * @param url
	 * @param method
	 * @param params
	 * @param callback
	 * @param type
	 * @throws Exception
	 */
	public <T> void sendAsync(URI url, String method, ObjectNode params,
			final AsyncCallback<T> callback, Type type) throws Exception;
	
	/**
	 * Do an asynchronous RPC call to another agent.
	 * 
	 * @param url
	 * @param method
	 * @param params
	 * @param callback
	 * @param type
	 * @throws Exception
	 */
	public <T> void sendAsync(URI url, String method, ObjectNode params,
			final AsyncCallback<T> callback, final JavaType type)
			throws Exception;
	
	/**
	 * Do an asynchronous RPC call to another agent.
	 * 
	 * @deprecated Please use one of the versions with can handle the more
	 *             complex generics returntypes. (e.g.
	 *             sendAsync(url,method,callback,JOM.getSimpleType(type));)
	 * 
	 * @param url
	 * @param method
	 * @param callback
	 * @param type
	 * @throws Exception
	 */
	@Deprecated
	public <T> void sendAsync(final URI url, final JSONRequest request,
			final AsyncCallback<T> callback, Class<T> type) throws Exception;
	
	/**
	 * Do an asynchronous RPC call to another agent.
	 * 
	 * @param url
	 * @param method
	 * @param callback
	 * @param type
	 * @throws Exception
	 */
	public <T> void sendAsync(final URI url, final JSONRequest request,
			final AsyncCallback<T> callback, Type type) throws Exception;
	
	/**
	 * Do an asynchronous RPC call to another agent.
	 * 
	 * @param url
	 * @param method
	 * @param callback
	 * @param type
	 * @throws Exception
	 */
	public <T> void sendAsync(final URI url, final JSONRequest request,
			final AsyncCallback<T> callback, final JavaType type)
			throws Exception;
	
	/**
	 * Create a proxy to an other agent. Invoked methods will be send to the
	 * actual agent via the AgentFactory.
	 * 
	 * @param url
	 * @param agentInterface
	 *            A Java Interface, extending AgentInterface
	 * @return agentProxy
	 */
	public <T> T createAgentProxy(URI url, Class<T> agentInterface);
	
	/**
	 * Create a proxy to an other agent. Invoked methods will be send to the
	 * actual agent via the AgentFactory.
	 * 
	 * @param url
	 * @param agentInterface
	 *            A Java Interface, extending AgentInterface
	 * @return agentProxy
	 */
	public <T> AsyncProxy<T> createAsyncAgentProxy(URI url,
			Class<T> agentInterface);

	
}
