package com.almende.eve.agent;

import java.io.IOException;
import java.lang.reflect.Type;
import java.net.URI;
import java.util.List;

import com.almende.eve.agent.annotation.Namespace;
import com.almende.eve.agent.callback.AsyncCallback;
import com.almende.eve.event.EventsInterface;
import com.almende.eve.monitor.ResultMonitorFactoryInterface;
import com.almende.eve.rpc.jsonrpc.JSONRPCException;
import com.almende.eve.rpc.jsonrpc.JSONRequest;
import com.almende.eve.scheduler.Scheduler;
import com.almende.eve.state.State;
import com.almende.eve.state.TypedKey;
import com.almende.util.TypeUtil;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * 
 * The Interface AgentInterface, This interface extends the core Eve internal
 * API with the declaration of all methods in the default Agent class.
 * 
 * @author Almende
 * 
 */
public interface AgentInterface extends AgentBaseInterface {
	
	/**
	 * Retrieve the agents id.
	 * 
	 * @return id
	 */
	String getId();
	
	/**
	 * Retrieve the agents type (its simple class name).
	 * 
	 * @return version
	 */
	String getType();
	
	/**
	 * Retrieve the agents version number.
	 * 
	 * @return version
	 */
	String getVersion();
	
	/**
	 * Retrieve a description of the agents functionality.
	 * 
	 * @return description
	 */
	String getDescription();
	
	/**
	 * Get the state of this agent. The state contains
	 * methods get, put, etc. to write properties into a persistent state.
	 * 
	 * @return the state
	 */
	State getState();
	
	/**
	 * Get the associated agentHost of this agent.
	 * 
	 * @return the agent host
	 */
	AgentHost getAgentHost();
	
	/**
	 * Get the scheduler to schedule tasks for the agent to be executed later
	 * on.
	 * 
	 * @return the scheduler
	 */
	@Namespace("scheduler")
	Scheduler getScheduler();
	
	/**
	 * Get the resultMonitorFactory, which can be used to register push/poll RPC
	 * result monitors.
	 * 
	 * @return the result monitor factory
	 */
	@Namespace("monitor")
	ResultMonitorFactoryInterface getResultMonitorFactory();
	
	/**
	 * Get the eventsFactory, which can be used to subscribe and trigger events.
	 * 
	 * @return the events factory
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
	 * Retrieve an array with the agents urls (can be one or multiple), and
	 * depends on the configured transport services.
	 * 
	 * @return urls
	 */
	List<String> getUrls();
	
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
	 * @param <T>
	 *            the generic type
	 * @param url
	 *            the url
	 * @param method
	 *            the method
	 * @param params
	 *            the params
	 * @param type
	 *            the type
	 * @return the t
	 * @throws IOException
	 *             Signals that an I/O exception has occurred.
	 * @throws JSONRPCException
	 *             the jSONRPC exception
	 */
	<T> T send(URI url, String method, Object params, Class<T> type)
			throws IOException, JSONRPCException;
	
	/**
	 * Do a RPC call to another agent.
	 * 
	 * @param <T>
	 *            the generic type
	 * @param url
	 *            the url
	 * @param method
	 *            the method
	 * @param params
	 *            the params
	 * @param type
	 *            the type
	 * @return the t
	 * @throws IOException
	 *             Signals that an I/O exception has occurred.
	 * @throws JSONRPCException
	 *             the jSONRPC exception
	 */
	<T> T send(URI url, String method, Object params, TypeUtil<T> type)
			throws IOException, JSONRPCException;
	
	/**
	 * Do a RPC call to another agent.
	 * 
	 * @param <T>
	 *            the generic type
	 * @param url
	 *            the url
	 * @param method
	 *            the method
	 * @param params
	 *            the params
	 * @param type
	 *            returntype
	 * @return the t
	 * @throws IOException
	 *             Signals that an I/O exception has occurred.
	 * @throws JSONRPCException
	 *             the jSONRPC exception
	 */
	<T> T send(URI url, String method, Object params, Type type)
			throws IOException, JSONRPCException;
	
	/**
	 * Do a RPC call to another agent.
	 * 
	 * @param <T>
	 *            the generic type
	 * @param url
	 *            the url
	 * @param method
	 *            the method
	 * @param params
	 *            the params
	 * @param type
	 *            returntype
	 * @return the t
	 * @throws IOException
	 *             Signals that an I/O exception has occurred.
	 * @throws JSONRPCException
	 *             the jSONRPC exception
	 */
	<T> T send(URI url, String method, Object params, JavaType type)
			throws IOException, JSONRPCException;
	
	/**
	 * Do a RPC call to another agent.
	 * 
	 * @param <T>
	 *            the generic type
	 * @param url
	 *            the url
	 * @param method
	 *            the method
	 * @param type
	 *            the type
	 * @return the t
	 * @throws IOException
	 *             Signals that an I/O exception has occurred.
	 * @throws JSONRPCException
	 *             the jSONRPC exception
	 */
	<T> T send(URI url, String method, Type type) throws IOException,
			JSONRPCException;
	
	/**
	 * Do a RPC call to another agent.
	 * 
	 * @param <T>
	 *            the generic type
	 * @param url
	 *            the url
	 * @param method
	 *            the method
	 * @param type
	 *            the type
	 * @return the t
	 * @throws IOException
	 *             Signals that an I/O exception has occurred.
	 * @throws JSONRPCException
	 *             the jSONRPC exception
	 */
	<T> T send(URI url, String method, JavaType type) throws IOException,
			JSONRPCException;
	
	/**
	 * Do a RPC call to another agent.
	 * 
	 * @param <T>
	 *            the generic type
	 * @param url
	 *            the url
	 * @param method
	 *            the method
	 * @param type
	 *            the type
	 * @return the t
	 * @throws IOException
	 *             Signals that an I/O exception has occurred.
	 * @throws JSONRPCException
	 *             the jSONRPC exception
	 */
	<T> T send(URI url, String method, Class<T> type) throws IOException,
			JSONRPCException;
	
	/**
	 * Do a RPC call to another agent.
	 * 
	 * @param <T>
	 *            the generic type
	 * @param url
	 *            the url
	 * @param method
	 *            the method
	 * @param type
	 *            the type
	 * @return the t
	 * @throws IOException
	 *             Signals that an I/O exception has occurred.
	 * @throws JSONRPCException
	 *             the jSONRPC exception
	 */
	<T> T send(URI url, String method, TypeUtil<T> type) throws IOException,
			JSONRPCException;
	
	/**
	 * Do a RPC call to another agent, expecting no result (void).
	 * 
	 * @param url
	 *            the url
	 * @param method
	 *            the method
	 * @param params
	 *            the params
	 * @throws IOException
	 *             Signals that an I/O exception has occurred.
	 * @throws JSONRPCException
	 *             the jSONRPC exception
	 */
	void send(URI url, String method, Object params) throws IOException,
			JSONRPCException;
	
	/**
	 * Do a RPC call to another agent, expecting no result (void).
	 * 
	 * @param url
	 *            the url
	 * @param method
	 *            the method
	 * @throws IOException
	 *             Signals that an I/O exception has occurred.
	 * @throws JSONRPCException
	 *             the jSONRPC exception
	 */
	void send(URI url, String method) throws IOException, JSONRPCException;
	
	/**
	 * Do an asynchronous RPC call to another agent.
	 * 
	 * @param <T>
	 *            the generic type
	 * @param url
	 *            the url
	 * @param method
	 *            the method
	 * @param params
	 *            the params
	 * @param callback
	 *            the callback
	 * @param type
	 *            the type
	 * @throws IOException
	 *             Signals that an I/O exception has occurred.
	 */
	<T> void sendAsync(URI url, String method, ObjectNode params,
			final AsyncCallback<T> callback, Class<T> type) throws IOException;
	
	/**
	 * Do an asynchronous RPC call to another agent.
	 * 
	 * @param <T>
	 *            the generic type
	 * @param url
	 *            the url
	 * @param method
	 *            the method
	 * @param params
	 *            the params
	 * @param callback
	 *            the callback
	 * @param type
	 *            the type
	 * @throws IOException
	 *             Signals that an I/O exception has occurred.
	 */
	<T> void sendAsync(URI url, String method, ObjectNode params,
			final AsyncCallback<T> callback, Type type) throws IOException;
	
	/**
	 * Do an asynchronous RPC call to another agent.
	 * 
	 * @param <T>
	 *            the generic type
	 * @param url
	 *            the url
	 * @param method
	 *            the method
	 * @param params
	 *            the params
	 * @param callback
	 *            the callback
	 * @param type
	 *            the type
	 * @throws IOException
	 *             Signals that an I/O exception has occurred.
	 */
	<T> void sendAsync(URI url, String method, ObjectNode params,
			final AsyncCallback<T> callback, final JavaType type)
			throws IOException;
	
	/**
	 * Do an asynchronous RPC call to another agent.
	 * 
	 * @param <T>
	 *            the generic type
	 * @param url
	 *            the url
	 * @param request
	 *            the request
	 * @param callback
	 *            the callback
	 * @param type
	 *            the type
	 * @throws IOException
	 *             Signals that an I/O exception has occurred.
	 */
	<T> void sendAsync(final URI url, final JSONRequest request,
			final AsyncCallback<T> callback, Class<T> type) throws IOException;
	
	/**
	 * Do an asynchronous RPC call to another agent.
	 * 
	 * @param <T>
	 *            the generic type
	 * @param url
	 *            the url
	 * @param request
	 *            the request
	 * @param callback
	 *            the callback
	 * @param type
	 *            the type
	 * @throws IOException
	 *             Signals that an I/O exception has occurred.
	 */
	<T> void sendAsync(final URI url, final JSONRequest request,
			final AsyncCallback<T> callback, Type type) throws IOException;
	
	/**
	 * Do an asynchronous RPC call to another agent.
	 * 
	 * @param <T>
	 *            the generic type
	 * @param url
	 *            the url
	 * @param request
	 *            the request
	 * @param callback
	 *            the callback
	 * @param type
	 *            the type
	 * @throws IOException
	 *             Signals that an I/O exception has occurred.
	 */
	<T> void sendAsync(final URI url, final JSONRequest request,
			final AsyncCallback<T> callback, final JavaType type)
			throws IOException;
	
	/**
	 * Do a RPC call to another agent, expecting no result (void).
	 * 
	 * @param url
	 *            the url
	 * @param method
	 *            the method
	 * @param params
	 *            the params
	 * @throws IOException
	 *             Signals that an I/O exception has occurred.
	 */
	void sendAsync(URI url, String method, ObjectNode params)
			throws IOException;
	
	/**
	 * Do a RPC call to another agent, expecting no result (void).
	 * 
	 * @param url
	 *            the url
	 * @param method
	 *            the method
	 * @throws IOException
	 *             Signals that an I/O exception has occurred.
	 */
	void sendAsync(URI url, String method) throws IOException;
	
	/**
	 * Create a proxy to an other agent. Invoked methods will be send to the
	 * actual agent via the AgentHost.
	 * 
	 * @param <T>
	 *            the generic type
	 * @param url
	 *            the url
	 * @param agentInterface
	 *            A Java Interface, extending AgentInterface
	 * @return agentProxy
	 */
	<T extends AgentInterface> T createAgentProxy(URI url,
			Class<T> agentInterface);
	
	/**
	 * Create a proxy to an other agent. Invoked methods will be send to the
	 * actual agent via the AgentHost.
	 * 
	 * @param <T>
	 *            the generic type
	 * @param url
	 *            the url
	 * @param agentInterface
	 *            A Java Interface, extending AgentInterface
	 * @return agentProxy
	 */
	<T extends AgentInterface> AsyncProxy<T> createAsyncAgentProxy(URI url,
			Class<T> agentInterface);
	
	/**
	 * Utility method to get back references to in-memory objects from the
	 * agent. The references are stored as WeakReferences, so the life-cycle of
	 * the referenced objects will not be influenced by storing them. However,
	 * the return value of this method is the hard-reference again.
	 * If the object has been garbage collected, this method will return null.
	 * 
	 * @param <T>
	 *            the generic type
	 * @param key
	 *            the key
	 * @return the ref
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
	 * @param <T>
	 *            the generic type
	 * @param key
	 *            the key
	 * @param value
	 *            the value
	 */
	<T> void putRef(TypedKey<T> key, T value);
	
	/**
	 * Checks for private.
	 * 
	 * @return true, if successful
	 */
	boolean hasPrivate();
}
