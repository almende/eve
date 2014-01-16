/*
 * Copyright: Almende B.V. (2014), Rotterdam, The Netherlands
 * License: The Apache Software License, Version 2.0
 */
package com.almende.eve.agent;

import java.io.IOException;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.net.URI;

import com.almende.eve.agent.callback.AsyncCallback;
import com.almende.eve.agent.callback.AsyncCallbackQueue;
import com.almende.eve.agent.callback.SyncCallback;
import com.almende.eve.rpc.jsonrpc.JSONMessage;
import com.almende.eve.rpc.jsonrpc.JSONRPC;
import com.almende.eve.rpc.jsonrpc.JSONRPCException;
import com.almende.eve.rpc.jsonrpc.JSONRPCException.CODE;
import com.almende.eve.rpc.jsonrpc.JSONRequest;
import com.almende.eve.rpc.jsonrpc.JSONResponse;
import com.almende.util.TypeUtil;
import com.fasterxml.jackson.databind.JsonNode;

/**
 * A factory for creating AgentProxy objects.
 */
public class AgentProxyFactory {
	private AgentHost	host	= null;
	
	/**
	 * Instantiates a new agent proxy factory.
	 * 
	 * @param host
	 *            the host
	 */
	public AgentProxyFactory(final AgentHost host) {
		this.host = host;
	}
	
	/**
	 * Gen proxy.
	 * 
	 * @param <T>
	 *            the generic type
	 * @param sender
	 *            the sender
	 * @param receiverUrl
	 *            the receiver url
	 * @param agentInterface
	 *            the agent interface
	 * @param proxyId
	 *            the proxy id
	 * @return the t
	 */
	@SuppressWarnings("unchecked")
	public <T extends AgentInterface> T genProxy(final AgentInterface sender,
			final URI receiverUrl, final Class<T> agentInterface,
			final String proxyId) {
		// http://docs.oracle.com/javase/1.4.2/docs/guide/reflection/proxy.html
		final T proxy = (T) Proxy.newProxyInstance(
				agentInterface.getClassLoader(),
				new Class[] { agentInterface }, new InvocationHandler() {
					
					@Override
					public Object invoke(final Object proxy,
							final Method method, final Object[] args)
							throws JSONRPCException, IOException {
						
						AgentInterface agent = sender;
						if (agent == null) {
							agent = (T) proxy;
						}
						
						// TODO: if method calls for Namespace getter, return
						// new proxy for subtype. All calls to that proxy need
						// to add namespace to method name for JSON-RPC.
						if (method.getName().equals("getId")) {
							return proxyId;
						} else if (method.getName().equals("receive")
								&& args.length > 1) {
							JSONResponse response = null;
							if (args[0] != null) {
								response = receive(args[0]);
							}
							if (response != null) {
								JsonNode id = null;
								if (response.getId() != null) {
									id = response.getId();
								}
								final AsyncCallbackQueue<JSONResponse> cbs = host
										.getCallbackQueue(proxyId,
												JSONResponse.class);
								if (cbs != null) {
									final AsyncCallback<JSONResponse> callback = cbs
											.pull(id);
									if (callback != null) {
										if (response.getError() != null) {
											callback.onFailure(response
													.getError());
										} else {
											callback.onSuccess(response);
										}
									}
								}
							}
							return null;
						} else {
							
							final JSONRequest request = JSONRPC.createRequest(
									method, args);
							
							final SyncCallback<JSONResponse> callback = new SyncCallback<JSONResponse>();
							final AsyncCallbackQueue<JSONResponse> cbs = host
									.getCallbackQueue(proxyId,
											JSONResponse.class);
							if (cbs != null) {
								cbs.push(request.getId(), "", callback);
							}
							try {
								host.sendAsync(receiverUrl, request, agent,
										null);
							} catch (final IOException e1) {
								throw new JSONRPCException(
										CODE.REMOTE_EXCEPTION, "", e1);
							}
							
							JSONResponse response;
							try {
								response = callback.get();
							} catch (final Exception e) {
								throw new JSONRPCException(
										CODE.REMOTE_EXCEPTION, "", e);
							}
							final JSONRPCException err = response.getError();
							if (err != null) {
								throw err;
							} else if (response.getResult() != null
									&& !method.getReturnType()
											.equals(Void.TYPE)) {
								return TypeUtil.inject(response.getResult(),
										method.getGenericReturnType());
							} else {
								return null;
							}
						}
					}
				});
		return proxy;
	}
	
	/**
	 * Receive.
	 * 
	 * @param arg
	 *            the arg
	 * @return the jSON response
	 * @throws JSONRPCException
	 *             the jSONRPC exception
	 * @throws IOException
	 *             Signals that an I/O exception has occurred.
	 */
	private JSONResponse receive(final Object arg) throws JSONRPCException,
			IOException {
		final JSONMessage jsonMsg = Agent.jsonConvert(arg);
		if (jsonMsg instanceof JSONResponse) {
			return (JSONResponse) jsonMsg;
		}
		return null;
	}
}
