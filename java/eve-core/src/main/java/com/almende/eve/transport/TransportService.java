/*
 * Copyright: Almende B.V. (2014), Rotterdam, The Netherlands
 * License: The Apache Software License, Version 2.0
 */
package com.almende.eve.transport;

import java.io.IOException;
import java.net.URI;
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
	URI getAgentUrl(String agentId);
	
	/**
	 * Get the id of an agent from its url.
	 * If the id cannot be extracted, null is returned.
	 *
	 * @param agentUrl the agent url
	 * @return agentId
	 */
	String getAgentId(URI agentUrl);
	
	/**
	 * Asynchronously Send a message to an other agent.
	 *
	 * @param senderUri the sender url
	 * @param receiverUri the receiver url
	 * @param message the message
	 * @param tag the tag
	 * @throws IOException Signals that an I/O exception has occurred.
	 * @oaran receiverUrl
	 */
	void sendAsync(final URI senderUri, final URI receiverUri,
			final String message, final String tag) throws IOException;
	
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
