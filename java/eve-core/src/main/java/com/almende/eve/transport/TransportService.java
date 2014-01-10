/*
 * Copyright: Almende B.V. (2014), Rotterdam, The Netherlands
 * License: The Apache Software License, Version 2.0
 */
package com.almende.eve.transport;

import java.io.IOException;
import java.util.List;

/**
 * The Interface TransportService.
 */
public interface TransportService {
	
	/**
	 * Get the url of an agent from its id.
	 *
	 * @param agentId the agent id
	 * @return agentUrl
	 */
	String getAgentUrl(String agentId);
	
	/**
	 * Get the id of an agent from its url.
	 * If the id cannot be extracted, null is returned.
	 *
	 * @param agentUrl the agent url
	 * @return agentId
	 */
	String getAgentId(String agentUrl);
	
	/**
	 * Asynchronously Send a message to an other agent.
	 *
	 * @param senderUrl the sender url
	 * @param receiverUrl the receiver url
	 * @param request the request
	 * @param tag the tag
	 * @throws IOException Signals that an I/O exception has occurred.
	 * @oaran receiverUrl
	 */
	void sendAsync(final String senderUrl, final String receiverUrl,
			final Object request, String tag) throws IOException;
	
	/**
	 * Get the protocols supported by this service.
	 *
	 * @return protocols
	 */
	List<String> getProtocols();
	
	/**
	 * (re)Connect this url (if applicable for this transport type).
	 *
	 * @param agentId the agent id
	 * @throws IOException Signals that an I/O exception has occurred.
	 */
	void reconnect(String agentId) throws IOException;
	
	/**
	 * Generate unique key for this transport service.
	 *
	 * @return the key
	 */
	String getKey();
	
}
