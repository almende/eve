package com.almende.eve.messenger;

import com.almende.eve.agent.Agent;
import com.almende.eve.json.JSONRequest;
import com.almende.eve.json.JSONResponse;


public interface Messenger {
	/**
	 * Set the agent instance
	 * @param agent
	 */
	public void setAgent (Agent agent);
	
	/**
	 * Login and connect the agent to the messaging service
	 * @param username
	 * @param password
	 */
	public void connect(String username, String password) throws Exception;

	/**
	 * Disconnect the agent from the messaging service
	 */
	public void disconnect();

	/**
	 * Send a message to an other agent
	 * @param username
	 * @param request
	 * @param callback with a JSONResponse
	 */
	public void send (String username, JSONRequest request, 
			AsyncCallback<JSONResponse> callback) throws Exception;

	/**
	 * Check whether the agent is connected to the messaging service
	 * @return connected
	 */
	public boolean isConnected();
}

