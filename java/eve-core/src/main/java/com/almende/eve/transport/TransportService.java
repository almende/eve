package com.almende.eve.transport;

import java.util.List;

import com.almende.eve.rpc.jsonrpc.JSONRequest;
import com.almende.eve.rpc.jsonrpc.JSONResponse;

public interface TransportService {
	/**
	 * Get the url of an agent from its id.
	 * @param agentId
	 * @return agentUrl
	 */
	public abstract String getAgentUrl(String agentId);

	/**
	 * Get the id of an agent from its url. 
	 * If the id cannot be extracted, null is returned.
	 * @param agentUrl
	 * @return agentId
	 */
	public abstract String getAgentId(String agentUrl);
	
	/**
	 * Send a message to an other agent
	 * @param senderId
	 * @oaran receiverUrl
	 * @param request
	 * @response response
	 */
	public abstract JSONResponse send (final String senderId, final String receiver, 
			final JSONRequest request) throws Exception;
	
	/**
	 * Asynchronously Send a message to an other agent
	 * @param senderId
	 * @oaran receiverUrl
	 * @param request
	 * @param callback with a JSONResponse
	 */
	public abstract void sendAsync (final String senderId, final String receiver, 
			final JSONRequest request, 
			final AsyncCallback<JSONResponse> callback) throws Exception;
	

	/**
	 * Get the protocols supported by this service
	 * @return protocols
	 */
	public abstract List<String> getProtocols();
	
	/**
	 * (re)Connect this url (if applicable for this transport type)
	 * @param url
	 */
	public abstract void reconnect(String agentId) throws Exception;

}

