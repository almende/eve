package com.almende.eve.agent;

import java.io.IOException;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.net.URI;

import com.almende.eve.agent.callback.AsyncCallback;
import com.almende.eve.agent.callback.CallbackInterface;
import com.almende.eve.agent.callback.SyncCallback;
import com.almende.eve.rpc.jsonrpc.JSONMessage;
import com.almende.eve.rpc.jsonrpc.JSONRPC;
import com.almende.eve.rpc.jsonrpc.JSONRPCException;
import com.almende.eve.rpc.jsonrpc.JSONRPCException.CODE;
import com.almende.eve.rpc.jsonrpc.JSONRequest;
import com.almende.eve.rpc.jsonrpc.JSONResponse;
import com.almende.util.TypeUtil;
import com.fasterxml.jackson.databind.JsonNode;

public class AgentProxyFactory {
	private AgentHost			host	= null;
	
	public AgentProxyFactory(AgentHost host) {
		this.host = host;
	}
	
	@SuppressWarnings("unchecked")
	public <T extends AgentInterface> T genProxy(final AgentInterface sender,
			final URI receiverUrl, final Class<T> agentInterface,
			final String proxyId) {
		// http://docs.oracle.com/javase/1.4.2/docs/guide/reflection/proxy.html
		T proxy = (T) Proxy.newProxyInstance(agentInterface.getClassLoader(),
				new Class[] { agentInterface }, new InvocationHandler() {
					
					public Object invoke(Object proxy, Method method,
							Object[] args) throws JSONRPCException, IOException {
						
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
								CallbackInterface<JSONResponse> cbs = host
										.getCallbackService(proxyId,
												JSONResponse.class);
								if (cbs != null) {
									AsyncCallback<JSONResponse> callback = cbs
											.get(id);
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
							
							JSONRequest request = JSONRPC.createRequest(method,
									args);
							
							SyncCallback<JSONResponse> callback = new SyncCallback<JSONResponse>();
							CallbackInterface<JSONResponse> cbs = host
									.getCallbackService(proxyId,
											JSONResponse.class);
							if (cbs != null) {
								cbs.store(request.getId(), callback);
							}
							try {
								host.sendAsync(receiverUrl, request, agent,
										null);
							} catch (IOException e1) {
								throw new JSONRPCException(
										CODE.REMOTE_EXCEPTION, "", e1);
							}
							
							JSONResponse response;
							try {
								response = callback.get();
							} catch (Exception e) {
								throw new JSONRPCException(
										CODE.REMOTE_EXCEPTION, "", e);
							}
							JSONRPCException err = response.getError();
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
	private JSONResponse receive(Object arg)
			throws JSONRPCException, IOException {
		JSONMessage jsonMsg = Agent.jsonConvert(arg);
		if (jsonMsg instanceof JSONResponse){
			return (JSONResponse) jsonMsg;
		}
		return null;
	}
}
