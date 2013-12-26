package com.almende.eve.transport;

import java.io.IOException;
import java.util.List;

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
	 * Asynchronously Send a message to an other agent.
	 * 
	 * @param senderUrl
	 * @oaran receiverUrl
	 * @param request
	 */
	void sendAsync(final String senderUrl, final String receiverUrl,
			final Object request, String tag) throws IOException;
	
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
	void reconnect(String agentId) throws IOException;

	/**
	 * Generate unique key for this transport service
	 * @return
	 */
	String getKey();
	
}
