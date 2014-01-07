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
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.node.ObjectNode;

@Access(AccessType.PUBLIC)
public interface ResultMonitorFactoryInterface {
	/**
	 * Callback method for monitoring framework, doing the work for
	 * requester-side polling
	 * part of a connection.
	 * 
	 * @param monitorId
	 * @throws JSONRPCException
	 * @throws IOException
	 * @throws Exception
	 */
	void doPoll(@Name("monitorId") String monitorId) throws JSONRPCException,
			IOException;
	
	/**
	 * Callback method for monitoring framework, doing the work for pushing data
	 * back to the requester.
	 * 
	 * @param pushParams
	 * @throws JSONRPCException
	 * @throws IOException
	 * @throws
	 * @throws Exception
	 */
	void doPush(@Name("pushKey") String pushKey,
			@Optional @Name("params") ObjectNode triggerParams)
			throws JSONRPCException, IOException;
	
	/**
	 * Callback method for the monitoring framework, doing the work for
	 * receiving pushed data in the requester.
	 * 
	 * @param result
	 * @param monitorId
	 * @throws JSONRPCException
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
	 * @param pushParams
	 * @param senderUrl
	 * @return
	 */
	void registerPush(@Name("pushId") String id,
			@Name("params") ObjectNode pushParams, @Sender String senderUrl);
	
	/**
	 * Unregister a Push request, part of the monitoring framework.
	 * 
	 * @param id
	 * @throws IOException
	 * @throws JsonProcessingException
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
	 * @param monitorId
	 * @param url
	 * @param method
	 * @param params
	 * @param callbackMethod
	 * @param confs
	 * @return
	 */
	@Access(AccessType.UNAVAILABLE)
	String create(String monitorId, URI url, String method, ObjectNode params,
			String callbackMethod, ResultMonitorConfigType... confs);
	
	/**
	 * Cancels a running monitor subscription.
	 * 
	 * @param monitorId
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
	 * @param monitorId
	 * @param filterParms
	 * @param returnType
	 * @return
	 * @throws JSONRPCException
	 * @throws IOException
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
	 * @param monitorId
	 * @param filterParms
	 * @param returnType
	 * @return
	 * @throws JSONRPCException
	 * @throws IOException
	 */
	@Access(AccessType.UNAVAILABLE)
	<T> T getResult(String monitorId, ObjectNode filterParms,
			Class<T> returnType) throws IOException, JSONRPCException;
	
	/**
	 * Store/update a monitor
	 * 
	 * @param monitor
	 * @return
	 */
	@Access(AccessType.UNAVAILABLE)
	String store(ResultMonitor monitor);
	
	/**
	 * Delete a monitor by its id
	 * 
	 * @param monitorId
	 */
	@Access(AccessType.UNAVAILABLE)
	void delete(String monitorId);
	
	/**
	 * Cancel all running monitors
	 */
	@Access(AccessType.UNAVAILABLE)
	void cancelAll();
	
	/**
	 * Find an existing monitor by its id
	 * 
	 * @param monitorId
	 * @return
	 */
	@Access(AccessType.UNAVAILABLE)
	ResultMonitor getMonitorById(String monitorId);
	
	/**
	 * Return a map of all existing monitors
	 * 
	 * @return
	 */
	@Access(AccessType.PUBLIC)
	List<ResultMonitor> getMonitors();
}
