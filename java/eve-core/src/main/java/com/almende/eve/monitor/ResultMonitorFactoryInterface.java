/*
 * Copyright: Almende B.V. (2014), Rotterdam, The Netherlands
 * License: The Apache Software License, Version 2.0
 */
package com.almende.eve.monitor;

import java.io.IOException;
import java.net.URI;
import java.util.List;

import com.almende.eve.rpc.annotation.Access;
import com.almende.eve.rpc.annotation.AccessType;
import com.almende.eve.rpc.annotation.Name;
import com.almende.eve.rpc.annotation.Optional;
import com.almende.eve.rpc.annotation.Sender;
import com.almende.eve.rpc.jsonrpc.JSONRPCException;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * The Interface ResultMonitorFactoryInterface.
 */
@Access(AccessType.PUBLIC)
public interface ResultMonitorFactoryInterface {
	
	/**
	 * Callback method for monitoring framework, doing the work for
	 * requester-side polling
	 * part of a connection.
	 *
	 * @param monitorId the monitor id
	 * @throws JSONRPCException the jSONRPC exception
	 * @throws IOException Signals that an I/O exception has occurred.
	 */
	void doPoll(@Name("monitorId") String monitorId) throws JSONRPCException,
			IOException;
	
	/**
	 * Callback method for monitoring framework, doing the work for pushing data
	 * back to the requester.
	 *
	 * @param pushKey the push key
	 * @param triggerParams the trigger params
	 * @throws JSONRPCException the jSONRPC exception
	 * @throws IOException Signals that an I/O exception has occurred.
	 */
	void doPush(@Name("pushKey") String pushKey,
			@Optional @Name("params") ObjectNode triggerParams)
			throws JSONRPCException, IOException;
	
	/**
	 * Callback method for the monitoring framework, doing the work for
	 * receiving pushed data in the requester.
	 *
	 * @param result the result
	 * @param monitorId the monitor id
	 * @param callbackParams the callback params
	 * @throws JSONRPCException the jSONRPC exception
	 */
	void callbackPush(@Name("result") Object result,
			@Name("monitorId") String monitorId,
			@Name("callbackParams") ObjectNode callbackParams)
			throws JSONRPCException;
	
	/**
	 * Register a Push request as part of the monitoring framework. The sender
	 * in this case is the requesting agent, the receiver has the requested RPC
	 * method.
	 *
	 * @param id the id
	 * @param pushParams the push params
	 * @param senderUrl the sender url
	 */
	void registerPush(@Name("pushId") String id,
			@Name("params") ObjectNode pushParams, @Sender String senderUrl);
	
	/**
	 * Unregister a Push request, part of the monitoring framework.
	 *
	 * @param id the id
	 * @param senderUrl the sender url
	 * @throws IOException Signals that an I/O exception has occurred.
	 */
	void unregisterPush(@Name("pushId") String id, @Sender String senderUrl)
			throws IOException;
	
	/**
	 * Sets up a monitored RPC call subscription. Conveniency method, which can
	 * also be expressed as:
	 * new ResultMonitor(monitorId, getId(),
	 * url,method,params).add(ResultMonitorConfigType
	 * config).add(ResultMonitorConfigType config).store();
	 *
	 * @param monitorId the monitor id
	 * @param url the url
	 * @param method the method
	 * @param params the params
	 * @param callbackMethod the callback method
	 * @param confs the confs
	 * @return the string
	 */
	@Access(AccessType.UNAVAILABLE)
	String create(String monitorId, URI url, String method, ObjectNode params,
			String callbackMethod, ResultMonitorConfigType... confs);
	
	/**
	 * Cancels a running monitor subscription.
	 *
	 * @param monitorId the monitor id
	 */
	@Access(AccessType.UNAVAILABLE)
	void cancel(String monitorId);
	
	/**
	 * Gets an actual return value of this monitor subscription. If a cache is
	 * available, this will return the cached value if the filter allows this.<br>
	 * <br>
	 * Otherwise it will run the actual RPC call (similar to "send");<br>
	 * <br>
	 * When using the default cache, filter params should contain an numeric
	 * maxAge field in miliseconds:<br>
	 * { "maxAge":1000 } -- maximally 1 second old data may be returned from
	 * cache.<br>
	 * <br>
	 *
	 * @param <T> the generic type
	 * @param monitorId the monitor id
	 * @param filterParms the filter parms
	 * @param returnType the return type
	 * @return the result
	 * @throws IOException Signals that an I/O exception has occurred.
	 * @throws JSONRPCException the jSONRPC exception
	 */
	@Access(AccessType.UNAVAILABLE)
	<T> T getResult(String monitorId, ObjectNode filterParms,
			JavaType returnType) throws IOException, JSONRPCException;
	
	/**
	 * Gets an actual return value of this monitor subscription. If a cache is
	 * available, this will return the cached value if the filter allows this.<br>
	 * <br>
	 * Otherwise it will run the actual RPC call (similar to "send");<br>
	 * <br>
	 * When using the default cache, filter params should contain an numeric
	 * maxAge field in miliseconds:<br>
	 * { "maxAge":1000 } -- maximally 1 second old data may be returned from
	 * cache.<br>
	 * <br>
	 *
	 * @param <T> the generic type
	 * @param monitorId the monitor id
	 * @param filterParms the filter parms
	 * @param returnType the return type
	 * @return the result
	 * @throws IOException Signals that an I/O exception has occurred.
	 * @throws JSONRPCException the jSONRPC exception
	 */
	@Access(AccessType.UNAVAILABLE)
	<T> T getResult(String monitorId, ObjectNode filterParms,
			Class<T> returnType) throws IOException, JSONRPCException;
	
	/**
	 * Store/update a monitor.
	 *
	 * @param monitor the monitor
	 * @return the string
	 */
	@Access(AccessType.UNAVAILABLE)
	String store(ResultMonitor monitor);
	
	/**
	 * Delete a monitor by its id.
	 *
	 * @param monitorId the monitor id
	 */
	@Access(AccessType.UNAVAILABLE)
	void delete(String monitorId);
	
	/**
	 * Cancel all running monitors.
	 */
	@Access(AccessType.UNAVAILABLE)
	void cancelAll();
	
	/**
	 * Find an existing monitor by its id.
	 *
	 * @param monitorId the monitor id
	 * @return the monitor by id
	 */
	@Access(AccessType.UNAVAILABLE)
	ResultMonitor getMonitorById(String monitorId);
	
	/**
	 * Return a map of all existing monitors.
	 *
	 * @return the monitors
	 */
	@Access(AccessType.PUBLIC)
	List<ResultMonitor> getMonitors();
}
