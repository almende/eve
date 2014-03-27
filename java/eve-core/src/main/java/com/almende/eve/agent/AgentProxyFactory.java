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

import com.almende.eve.agent.callback.SyncCallback;
import com.almende.eve.rpc.jsonrpc.JSONRPC;
import com.almende.eve.rpc.jsonrpc.JSONRPCException;
import com.almende.eve.rpc.jsonrpc.JSONRPCException.CODE;
import com.almende.eve.rpc.jsonrpc.JSONRequest;
import com.almende.eve.rpc.jsonrpc.JSONResponse;
import com.almende.util.TypeUtil;

/**
 * A factory for creating AgentProxy objects.
 */
public class AgentProxyFactory {
	
	/**
	 * Gen proxy.
	 * 
	 * @param <T>
	 *            the generic type
	 * @param sender 
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
						
						final JSONRequest request = JSONRPC.createRequest(
								method, args);
						
						final SyncCallback<JSONResponse> callback = new SyncCallback<JSONResponse>();
						try {
							sender.send(request, receiverUrl, callback, null);
						} catch (final IOException e1) {
							throw new JSONRPCException(CODE.REMOTE_EXCEPTION,
									"", e1);
						}
						
						JSONResponse response;
						try {
							response = callback.get();
						} catch (final Exception e) {
							throw new JSONRPCException(CODE.REMOTE_EXCEPTION,
									"", e);
						}
						final JSONRPCException err = response.getError();
						if (err != null) {
							throw err;
						} else if (response.getResult() != null
								&& !method.getReturnType().equals(Void.TYPE)) {
							return TypeUtil.inject(response.getResult(),
									method.getGenericReturnType());
						} else {
							return null;
						}
					}
				});
		return proxy;
	}
}
