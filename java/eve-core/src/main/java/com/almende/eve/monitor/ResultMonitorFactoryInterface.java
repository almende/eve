package com.almende.eve.monitor;

import java.io.IOException;
import java.net.ProtocolException;
import java.net.URI;
import java.util.Map;

import com.almende.eve.rpc.annotation.Access;
import com.almende.eve.rpc.annotation.AccessType;
import com.almende.eve.rpc.annotation.Name;
import com.almende.eve.rpc.annotation.Required;
import com.almende.eve.rpc.annotation.Sender;
import com.almende.eve.rpc.jsonrpc.JSONRPCException;
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
	void doPoll(@Name("monitorId") String monitorId) throws JSONRPCException, IOException;
	
	/**
	 * Callback method for monitoring framework, doing the work for pushing data
	 * back to the requester.
	 * 
	 * @param pushParams
	 * @throws JSONRPCException
	 * @throws ProtocolException
	 * @throws Exception
	 */
	void doPush(@Name("pushParams") ObjectNode pushParams,
			@Required(false) @Name("triggerParams") ObjectNode triggerParams)
			throws ProtocolException, JSONRPCException;
	
	/**
	 * Callback method for the monitoring framework, doing the work for
	 * receiving pushed data in the requester.
	 * 
	 * @param result
	 * @param monitorId
	 */
	void callbackPush(@Name("result") Object result,
			@Name("monitorId") String monitorId,
			@Name("callbackParams") ObjectNode callbackParams);
	
	/**
	 * Register a Push request as part of the monitoring framework. The sender
	 * in this case is the requesting agent, the receiver has the requested RPC
	 * method.
	 * 
	 * @param pushParams
	 * @param senderUrl
	 * @return
	 */
	void registerPush(@Name("pushId") String id, @Name("params") ObjectNode pushParams,
			@Sender String senderUrl);
	
	/**
	 * Unregister a Push request, part of the monitoring framework.
	 * 
	 * @param id
	 */
	void unregisterPush(@Name("pushId") String id);
	
	/**
	 * Sets up a monitored RPC call subscription. Conveniency method, which can
	 * also be expressed as:
	 * new ResultMonitor(monitorId, getId(), url,method,params).add(ResultMonitorConfigType
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
	 * available,
	 * this will return the cached value if the maxAge filter allows this.
	 * Otherwise it will run the actual RPC call (similar to "send");
	 * 
	 * @param monitorId
	 * @param filterParms
	 * @param returnType
	 * @return
	 * @throws JSONRPCException
	 * @throws IOException
	 * @throws Exception
	 */
	@Access(AccessType.UNAVAILABLE)
	<T> T getResult(String monitorId, ObjectNode filterParms,
			JavaType returnType) throws IOException, JSONRPCException;
	
	/**
	 * Gets an actual return value of this monitor subscription. If a cache is
	 * available,
	 * this will return the cached value if the maxAge filter allows this.
	 * Otherwise it will run the actual RPC call (similar to "send");
	 * 
	 * @param monitorId
	 * @param filterParms
	 * @param returnType
	 * @return
	 * @throws JSONRPCException
	 * @throws IOException
	 * @throws Exception
	 */
	@Access(AccessType.UNAVAILABLE)
	<T> T getResult(String monitorId, ObjectNode filterParms,
			Class<T> returnType) throws IOException, JSONRPCException;
	
	
	@Access(AccessType.UNAVAILABLE)
	String store(ResultMonitor monitor);
	
	@Access(AccessType.UNAVAILABLE)
	void delete(String monitorId);
	
	@Access(AccessType.UNAVAILABLE)
	void cancelAll();

	@Access(AccessType.UNAVAILABLE)
	ResultMonitor getMonitorById(String monitorId);
	
	@Access(AccessType.PUBLIC)
	Map<String, ResultMonitor> getMonitors();
}
