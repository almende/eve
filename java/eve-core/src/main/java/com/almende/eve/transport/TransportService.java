package com.almende.eve.transport;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.almende.eve.agent.AgentFactory;
import com.almende.eve.rpc.jsonrpc.JSONRequest;
import com.almende.eve.rpc.jsonrpc.JSONResponse;

public abstract class TransportService {
	public TransportService(AgentFactory agentFactory) {
		this.agentFactory = agentFactory;
	}
	
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
	 * Initialize the transport service with a set of parameters
	 * @param params
	 */
	public abstract void init(Map<String, Object> params);
	
	/**
	 * Get the protocols supported by this service
	 * @return protocols
	 */
	public abstract List<String> getProtocols();
	
	@Override
	public String toString() {
		Map<String, Object> data = new HashMap<String, Object>();
		data.put("class", this.getClass().getName());
		return data.toString();
	}
	
	protected AgentFactory agentFactory = null;
}

