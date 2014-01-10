/*
 * Copyright: Almende B.V. (2014), Rotterdam, The Netherlands
 * License: The Apache Software License, Version 2.0
 */
package com.almende.eve.agent;

import java.io.IOException;
import java.net.URI;

import com.almende.eve.agent.callback.AsyncCallback;
import com.almende.eve.rpc.jsonrpc.JSONAuthorizor;
import com.almende.eve.rpc.jsonrpc.JSONRPCException;
import com.almende.eve.rpc.jsonrpc.JSONResponse;

/**
 * 
 * 
 * @author Almende
 */
public interface AgentBaseInterface extends JSONAuthorizor {
	
	/**
	 * This method is called every time something changes to the AgentHost, like
	 * booting, adding or removal of services, etc.
	 * 
	 * @param event
	 *            the event
	 * @throws JSONRPCException
	 *             the jSONRPC exception
	 * @throws IOException
	 *             Signals that an I/O exception has occurred.
	 */
	void signalAgent(AgentSignal<?> event) throws JSONRPCException, IOException;
	
	/**
	 * 
	 * The primary send method, all other send/sendAsync are mapping to this
	 * one. This method will interact with the transport layer (through the
	 * agentHost) to send the message to the given receiverUrl.
	 * 
	 * @param msg
	 *            the message to deliver. (this object should implement an
	 *            useful toString() as most transports will send the message as
	 *            a string)
	 * @param receiverUrl
	 *            the receiver url
	 * @param callback
	 *            An optional JSONResponse callback.
	 * @param tag
	 *            If this is a reply on a tagged receive, pass back the tag
	 * @throws IOException
	 *             Signals that an I/O exception has occurred.
	 */
	void send(Object msg, URI receiverUrl,
			AsyncCallback<JSONResponse> callback, String tag)
			throws IOException;
	
	/**
	 * This is the primary receive method of the agent. All incoming messages
	 * are delivered through this method.
	 * 
	 * @param msg
	 *            the message, mostly a string containing JSON-RPC. Can be other
	 *            types as well in various situations.
	 * @param senderUrl
	 *            the sender url
	 * @param tag
	 *            If set, this is a tagged message, meaning any replies should
	 *            also carry this tag. (see send())
	 */
	void receive(Object msg, URI senderUrl, String tag);
	
}
