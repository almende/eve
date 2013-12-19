package com.almende.eve.agent;

import java.io.IOException;
import java.lang.reflect.Type;
import java.net.ProtocolException;
import java.net.URI;
import java.util.List;

import com.almende.eve.agent.annotation.Namespace;
import com.almende.eve.agent.callback.AsyncCallback;
import com.almende.eve.agent.proxy.AsyncProxy;
import com.almende.eve.event.EventsInterface;
import com.almende.eve.monitor.ResultMonitorFactoryInterface;
import com.almende.eve.rpc.jsonrpc.JSONAuthorizor;
import com.almende.eve.rpc.jsonrpc.JSONRPCException;
import com.almende.eve.rpc.jsonrpc.JSONRequest;
import com.almende.eve.scheduler.Scheduler;
import com.almende.eve.state.State;
import com.almende.eve.state.TypedKey;
import com.almende.util.TypeUtil;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.node.ObjectNode;

public interface AgentInterface extends JSONAuthorizor {
	/**
	 * Retrieve the agents id
	 * 
	 * @return id
	 */
	String getId();
	
	/**
	 * Retrieve the agents type (its simple class name)
	 * 
	 * @return version
	 */
	String getType();
	
	/**
	 * Retrieve the agents version number
	 * 
	 * @return version
	 */
	String getVersion();
	
	/**
	 * Retrieve a description of the agents functionality
	 * 
	 * @return description
	 */
	String getDescription();
	
	/**
	 * Retrieve an array with the agents urls (can be one or multiple), and
	 * depends on the configured transport services.
	 * 
	 * @return urls
	 */
	List<String> getUrls();
	
	/**
	 * Get the state of this agent. The state contains
	 * methods get, put, etc. to write properties into a persistent state.
	 * 
	 * 
	 */
	State getState();
	
	/**
	 * Get the associated AgentHost of this agent
	 * 
	 * @deprecated Use getAgentHost() instead
	 * 
	 */
	@Deprecated
	AgentHost getAgentFactory();
	
	/**
	 * Get the associated agentHost of this agent
	 * 
	 */
	AgentHost getAgentHost();
	
	/**
	 * Get the scheduler to schedule tasks for the agent to be executed later
	 * on.
	 * 
	 */
	@Namespace("scheduler")
	Scheduler getScheduler();
	
	/**
	 * Get the resultMonitorFactory, which can be used to register push/poll RPC
	 * result monitors.
	 */
	@Namespace("monitor")
	ResultMonitorFactoryInterface getResultMonitorFactory();
	
	/**
	 * Get the eventsFactory, which can be used to subscribe and trigger events.
	 */
	@Namespace("event")
	EventsInterface getEventsFactory();
	
	/**
	 * Retrieve a list with all the available methods.
	 * 
	 * @return methods
	 */
	List<Object> getMethods();
	
	/**
	 * This method is called every time something changes to the AgentHost, like
	 * booting, adding or removal of services, etc.
	 * 
	 * @param event
	 * @throws IOException
	 * @throws JSONRPCException
	 */
	void signalAgent(AgentSignal<?> event) throws JSONRPCException, IOException;
	
	/**
	 * Get the first url of the agents urls. Returns local:<agentId> if the
	 * agent does not
	 * have any urls.
	 * 
	 * @return firstUrl
	 */
	URI getFirstUrl();
	
	/**
	 * Do a RPC call to another agent.
	 * 
	 * @param url
	 * @param method
	 * @param params
	 * @param type
	 * @return
	 */
	<T> T send(URI url, String method, Object params, Class<T> type)
			throws ProtocolException, JSONRPCException;
	
	/**
	 * Do a RPC call to another agent.
	 * 
	 * @param url
	 * @param method
	 * @param params
	 * @param type
	 * @return
	 */
	<T> T send(URI url, String method, Object params, TypeUtil<T> type)
			throws ProtocolException, JSONRPCException;
	
	/**
	 * Do a RPC call to another agent.
	 * 
	 * @param url
	 * @param method
	 * @param params
	 * @param type
	 *            returntype
	 * @return
	 */
	<T> T send(URI url, String method, Object params, Type type)
			throws ProtocolException, JSONRPCException;
	
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
	<T> T send(URI url, String method, Object params, JavaType type)
			throws ProtocolException, JSONRPCException;
	
	/**
	 * Do a RPC call to another agent.
	 * 
	 * @param url
	 * @param method
	 * @param type
	 * @return
	 * @throws Exception
	 */
	<T> T send(URI url, String method, Type type) throws ProtocolException,
			JSONRPCException;
	
	/**
	 * Do a RPC call to another agent.
	 * 
	 * @param url
	 * @param method
	 * @param type
	 * @return
	 * @throws Exception
	 */
	<T> T send(URI url, String method, JavaType type) throws ProtocolException,
			JSONRPCException;
	
	/**
	 * Do a RPC call to another agent.
	 * 
	 * @param url
	 * @param method
	 * @param type
	 * @return
	 * @throws Exception
	 */
	<T> T send(URI url, String method, Class<T> type) throws ProtocolException,
			JSONRPCException;
	
	/**
	 * Do a RPC call to another agent.
	 * 
	 * @param url
	 * @param method
	 * @param type
	 * @return
	 * @throws Exception
	 */
	<T> T send(URI url, String method, TypeUtil<T> type)
			throws ProtocolException, JSONRPCException;
	
	/**
	 * Do a RPC call to another agent, expecting no result (void)
	 * 
	 * @param url
	 * @param method
	 * @param params
	 * @throws Exception
	 */
	void send(URI url, String method, Object params) throws ProtocolException,
			JSONRPCException;
	
	/**
	 * Do a RPC call to another agent, expecting no result (void)
	 * 
	 * @param url
	 * @param method
	 * @throws Exception
	 */
	void send(URI url, String method) throws ProtocolException,
			JSONRPCException;
	
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
	<T> void sendAsync(URI url, String method, ObjectNode params,
			final AsyncCallback<T> callback, Class<T> type)
			throws ProtocolException, JSONRPCException;
	
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
	<T> void sendAsync(URI url, String method, ObjectNode params,
			final AsyncCallback<T> callback, Type type)
			throws ProtocolException, JSONRPCException;
	
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
	<T> void sendAsync(URI url, String method, ObjectNode params,
			final AsyncCallback<T> callback, final JavaType type)
			throws ProtocolException, JSONRPCException;
	
	/**
	 * Do an asynchronous RPC call to another agent.
	 * 
	 * 
	 * @param url
	 * @param method
	 * @param callback
	 * @param type
	 * @throws Exception
	 */
	<T> void sendAsync(final URI url, final JSONRequest request,
			final AsyncCallback<T> callback, Class<T> type)
			throws ProtocolException, JSONRPCException;
	
	/**
	 * Do an asynchronous RPC call to another agent.
	 * 
	 * @param url
	 * @param method
	 * @param callback
	 * @param type
	 * @throws Exception
	 */
	<T> void sendAsync(final URI url, final JSONRequest request,
			final AsyncCallback<T> callback, Type type)
			throws ProtocolException, JSONRPCException;
	
	/**
	 * Do an asynchronous RPC call to another agent.
	 * 
	 * @param url
	 * @param method
	 * @param callback
	 * @param type
	 * @throws Exception
	 */
	<T> void sendAsync(final URI url, final JSONRequest request,
			final AsyncCallback<T> callback, final JavaType type)
			throws ProtocolException, JSONRPCException;

	/**
	 * Do a RPC call to another agent, expecting no result (void)
	 * 
	 * @param url
	 * @param method
	 * @param params
	 * @throws Exception
	 */
	void sendAsync(URI url, String method, ObjectNode params) throws ProtocolException,
			JSONRPCException;
	
	/**
	 * Do a RPC call to another agent, expecting no result (void)
	 * 
	 * @param url
	 * @param method
	 * @throws Exception
	 */
	void sendAsync(URI url, String method) throws ProtocolException,
			JSONRPCException;
	
	/**
	 * Create a proxy to an other agent. Invoked methods will be send to the
	 * actual agent via the AgentHost.
	 * 
	 * @param url
	 * @param agentInterface
	 *            A Java Interface, extending AgentInterface
	 * @return agentProxy
	 */
	<T> T createAgentProxy(URI url, Class<T> agentInterface);
	
	/**
	 * Create a proxy to an other agent. Invoked methods will be send to the
	 * actual agent via the AgentHost.
	 * 
	 * @param url
	 * @param agentInterface
	 *            A Java Interface, extending AgentInterface
	 * @return agentProxy
	 */
	<T> AsyncProxy<T> createAsyncAgentProxy(URI url, Class<T> agentInterface);
	
	/**
	 * Utility method to get back references to in-memory objects from the
	 * agent. The references are stored as WeakReferences, so the life-cycle of
	 * the referenced objects will not be influenced by storing them. However,
	 * the return value of this method is the hard-reference again.
	 * If the object has been garbage collected, this method will return null.
	 * 
	 * @param key
	 * @return
	 */
	<T> T getRef(TypedKey<T> key);
	
	/**
	 * Utility method to keep reference of in-memory objects from the agent. The
	 * references are stored as WeakReferences, so the life-cycle of the
	 * referenced objects will not be influenced by storing them.
	 * This means that you can't use this method for storing objects you created
	 * in the agent, those still need to be stored in the agent's state.
	 * 
	 * Purpose for these methods is to allow agents to work with/for legacy
	 * software objects.
	 * 
	 * @param key
	 * @param value
	 */
	<T> void putRef(TypedKey<T> key, T value);
}
