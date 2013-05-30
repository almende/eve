/**
 * @file ChatAgent.java
 * 
 * @brief 
 * A peer to peer chat agent.
 * Usage:
 * 
 * Set username: 
 *     HTTP POST http://localhost:8080/EveCore/agents/chatagent/1
 *     {
 *         "id": 1,
 *         "method": "setUsername",
 *         "params":{"username":"Agent1"}
 *     }
 * 
 * Connect two agents: 
 *     HTTP POST http://localhost:8080/EveCore/agents/chatagent/1
 *     {
 *         "id": 1,
 *         "method": "connect",
 *         "params": {
 *             "url": "http://localhost:8080/EveCore/agents/chatagent/2"
 *         }
 *     }
 *
 * Post a message:
 *     HTTP POST http://localhost:8080/EveCore/agents/chatagent/1
 *     {
 *         "id": 1,
 *         "method": "post",
 *         "params": {
 *             "message": "hello world"
 *         }
 *     }
 * 
 * Disconnect an agent: 
 *     HTTP POST http://localhost:8080/EveCore/agents/chatagent/1
 *     {
 *         "id": 1,
 *         "method": "disconnect",
 *         "params": {}
 *     }
 *
 *
 * @license
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy 
 * of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 *
 * Copyright © 2011-2012 Almende B.V.
 *
 * @author 	Jos de Jong, <jos@almende.org>
 * @date	  2011-04-02
 */

package com.almende.eve.agent.example;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import com.almende.eve.agent.Agent;
import com.almende.eve.rpc.annotation.Name;
import com.almende.eve.rpc.jsonrpc.jackson.JOM;
import com.fasterxml.jackson.databind.node.ObjectNode;

public class ChatAgent extends Agent {
	/**
	 * Get the username
	 * @return
	 * @throws Exception 
	 */
	public String getUsername() throws Exception {
		String username = (String) getState().get("username");
		return (username != null) ? username : getMyUrl();
	}

	private String getMyUrl() {
		List<String> urls = getUrls();
		return urls.size() > 0 ? urls.get(0): null;
	}
	
	/**
	 * Set the username, used for displaying messages
	 * @param username
	 */
	public void setUsername(@Name("username") String username) {
		getState().put("username", username);
	}
	
	/**
	 * Post a message to all registered agents (including itself).
	 * @param message 
	 * @throws Exception 
	 * @throws JSONException 
	 * @throws IOException 
	 * @throws RuntimeException 
	 */
	public void post(@Name("message") String message) throws Exception {
		List<String> connections = getConnections();

		// trigger a "post message"
		ObjectNode params = JOM.createObjectNode();
		params.put("url", getMyUrl());
		params.put("username", getUsername());
		params.put("message", message);
		eventsFactory.trigger("post", params);
		
		log(getUsername() + " posts message '" + message + "'" + 
				" to " + connections.size() + " agent(s)"); 

		for (int i = 0; i < connections.size(); i++) {
			String connection = connections.get(i);
			send(connection, "receive", params);
		}
	}	
	
	/**
	 * Receive a message from an agent
	 * @param url
	 * @param username
	 * @param message
	 * @throws Exception 
	 */
	public void receive(@Name("url") String url, 
			@Name("username") String username, 
			@Name("message") String message) throws Exception {
		// trigger a "receive" message
		ObjectNode params = JOM.createObjectNode();
		params.put("url", url);
		params.put("username", username);
		params.put("message", message);
		eventsFactory.trigger("receive", params);

		log(getUsername() + " received message from " + 
				username + ": " + message);	
	}
	
	/**
	 * connect two agents with each other
	 * @param url   Url of an ChatAgent
	 * @throws Exception 
	 */
	@SuppressWarnings("unchecked")
	public void connect(@Name("url") String url) throws Exception {
		boolean otherAlreadyConnected = false;
		ArrayList<String> newConnections = new ArrayList<String>();
		ArrayList<String> otherConnections = send(url, "getConnections", JOM.getTypeFactory().constructArrayType(String.class));

		// get my own connections from the state
		String urlSelf = getMyUrl();
		ArrayList<String> connections = (ArrayList<String>) getState().get("connections"); 
		if (connections == null) {	
			connections = new ArrayList<String>();
		}

		for (int i = 0; i < otherConnections.size(); i++) {
			String connection = otherConnections.get(i);
			if (!connection.equals(urlSelf)) {
				// this agent is not me
				if (connections.indexOf(connection) == -1) {
					// this is an agent that I didn't knew before
					connections.add(connection);
					newConnections.add(connection);

					// trigger a "connected" event
					ObjectNode params = JOM.createObjectNode();
					params.put("url", connection);
					eventsFactory.trigger("connected", params);

					log(getUsername() + " connected to " + connection);
				}
			}
			else {
				// this agent is me. So, the other agent already knows me
				// (-> thus I don't have to connect to him again)
				otherAlreadyConnected = true;
			}
		}

		// add the agent that triggered the connect
		if (connections.indexOf(url) == -1) {
				connections.add(url);
				
				// trigger a "connected" event
				ObjectNode params = JOM.createObjectNode();
				params.put("url", url);
				eventsFactory.trigger("connected", params);
				
				log(getUsername() + " connected to " + url);
		}
		if (!otherAlreadyConnected) {
			// the other agent doesn't know me
			newConnections.add(url);
		}

		// store the connection list
		getState().put("connections", connections);

		// schedule tasks to connect to all newly connected agents
		for (String connection : newConnections) {
			ObjectNode params = JOM.createObjectNode();
			params.put("url", urlSelf);
			send(connection, "connect", params);
		}		
	}

	/**
	 * Disconnect this agent from all other agents in the chat room
	 * @throws Exception 
	 */
	@SuppressWarnings("unchecked")
	public void disconnect() throws Exception {
		List<String> connections = (List<String>) getState().get("connections");
		if (connections != null) {
			getState().remove("connections");			

			log(getUsername() + " disconnecting " + connections.size() + " agent(s)"); 
			
			for (int i = 0; i < connections.size(); i++) {
				String url = connections.get(i);
				String urlSelf = getMyUrl();
				String method = "removeConnection";
				ObjectNode params = JOM.createObjectNode();
				params.put("url", urlSelf);
				send(url, method, params);
				
				// trigger a "disconnected" event
				ObjectNode triggerParams = JOM.createObjectNode();
				triggerParams.put("url", url);
				eventsFactory.trigger("disconnected", triggerParams);
			}			
		}		
	}
	
	/**
	 * Remove an agent from connections list
	 * @param url  Url of a connected ChatAgent
	 * @throws Exception 
	 */
	@SuppressWarnings("unchecked")
	public void removeConnection(@Name("url") String url) throws Exception {
		ArrayList<String> connections = (ArrayList<String>) getState().get("connections");
		if (connections != null) {
			connections.remove(url);
			getState().put("connections", connections);	
			
			log(getUsername() + " disconnected from " + url); 
			// trigger a "connected" event
			ObjectNode params = JOM.createObjectNode();
			params.put("url", url);
			eventsFactory.trigger("disconnected", params);			
		}
	}

	/**
	 * Retrieve the urls of all agents that are connected
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public List<String> getConnections() {
		List<String> connections = (List<String>) getState().get("connections");
		if (connections != null) {
			return connections;
		}
		else {
			return new ArrayList<String>();
		}
	}

	/**
	 * Log a message
	 * @param message
	 */
	private void log(String message) {
		Logger logger = Logger.getLogger(this.getClass().getName());		
		logger.info(message);
		// System.out.println(message);
	}
	
	@Override
	public String getDescription() {
		return "A peer to peer chat agent. " +
			"First call setUsername to set the agents usernames (optional). " +
			"Then use connect to connect an agent to another agent. " + 
			"They will automatically synchronize their adress lists. " +
			"Then, use post to post a message.";
	}

	@Override
	public String getVersion() {
		return "1.0";
	}	
}