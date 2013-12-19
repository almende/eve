package com.almende.eve.transport;

import java.io.IOException;
import java.util.List;

import com.almende.eve.agent.callback.AsyncCallback;
import com.almende.eve.rpc.jsonrpc.JSONRPCException;
import com.almende.eve.rpc.jsonrpc.JSONRequest;
import com.almende.eve.rpc.jsonrpc.JSONResponse;

public interface TransportService {
	/**
	 * Get the url of an agent from its id.
	 * 
	 * @param agentId
	 * @return agentUrl
	 */
	String getAgentUrl(String agentId);
	
	/**
	 * Get the id of an agent from its url.
	 * If the id cannot be extracted, null is returned.
	 * 
	 * @param agentUrl
	 * @return agentId
	 */
	String getAgentId(String agentUrl);
	
	/**
	 * Send a message to an other agent
	 * 
	 * @param senderUrl
	 * @oaran receiverUrl
	 * @param request
	 * @response response
	 */
	JSONResponse send(final String senderUrl, final String receiverUrl,
			final JSONRequest request) throws JSONRPCException;
	
	/**
	 * Asynchronously Send a message to an other agent
	 * 
	 * @param senderUrl
	 * @oaran receiverUrl
	 * @param request
	 * @param callback
	 *            with a JSONResponse
	 */
	void sendAsync(final String senderUrl, final String receiverUrl,
			final JSONRequest request,
			final AsyncCallback<JSONResponse> callback) throws JSONRPCException;
	
	/**
	 * Get the protocols supported by this service
	 * 
	 * @return protocols
	 */
	List<String> getProtocols();
	
	/**
	 * (re)Connect this url (if applicable for this transport type)
	 * 
	 * @param url
	 */
	void reconnect(String agentId) throws JSONRPCException, IOException;

	/**
	 * Generate unique key for this transport service
	 * @return
	 */
	String getKey();
	
}
