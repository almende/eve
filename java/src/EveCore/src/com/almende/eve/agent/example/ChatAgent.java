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

import net.sf.json.JSONArray;
import net.sf.json.JSONObject;

import com.almende.eve.agent.Agent;
import com.almende.eve.json.annotation.ParameterName;

@SuppressWarnings("serial")
public class ChatAgent extends Agent {
	/**
	 * Get the username
	 * @return
	 * @throws Exception 
	 */
	public String getUsername() throws Exception {
		Object username = getContext().get("username");
		return (username != null) ? (String)username : getUrl();
	}

	/**
	 * Set the username, used for displaying messages
	 * @param username
	 */
	public void setUsername(@ParameterName("username") String username) {
		getContext().put("username", username);
	}
	
	/**
	 * Post a message to all registered agents (including itself).
	 * @param message 
	 * @throws Exception 
	 * @throws JSONException 
	 * @throws IOException 
	 * @throws RuntimeException 
	 */
	public void post(@ParameterName("message") String message) throws Exception {
		JSONArray connections = getConnections();

		log(getUsername() + " posts message \"" + message + "\"" + 
				" to " + connections.size() + " agent(s)"); 

		JSONObject params = new JSONObject();
		params.put("url", getUrl());
		params.put("username", getUsername());
		params.put("message", message);
		for (int i = 0; i < connections.size(); i++) {
			String connection = connections.getString(i);
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
	public void receive(@ParameterName("url") String url, 
			@ParameterName("username") String username, 
			@ParameterName("message") String message) throws Exception {
		log(getUsername() + " received message from " + 
				username + ": " + message);
	}
	
	/**
	 * connect two agents with each other
	 * @param url   Url of an ChatAgent
	 * @throws Exception 
	 */
	public void connect(@ParameterName("url") String url) throws Exception {
		boolean otherAlreadyConnected = false;
		List<String> newConnections = new ArrayList<String>();

		// retrieve all connections that the other agent has, and synchronize
		// my own list with it.
		JSONArray otherConnections = (JSONArray)send(url, "getConnections", 
				new JSONObject());
		
		// get my own connections from the context
		String urlSelf = getUrl();
		Object obj = getContext().get("connections");
		JSONArray connections = (obj != null) ? (JSONArray)obj : new JSONArray();
		for (int i = 0; i < otherConnections.size(); i++) {
			String connection = otherConnections.getString(i);
			if (!connection.equals(urlSelf)) {
				// this agent is not me
				if (connections.indexOf(connection) == -1) {
					// this is an agent that I didn't knew before
					connections.add(connection);
					newConnections.add(connection);
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
				log(getUsername() + " connected to " + url);
		}
		if (!otherAlreadyConnected) {
			// the other agent doesn't know me
			newConnections.add(url);
		}

		// store the connection list
		getContext().put("connections", connections);

		// schedule tasks to connect to all newly connected agents
		for (String connection : newConnections) {
			JSONObject params = new JSONObject();
			params.put("url", urlSelf);
			send(connection, "connect", params);
		}
	}

	/**
	 * Disconnect this agent from all other agents in the chat room
	 * @throws Exception 
	 */
	public void disconnect() throws Exception {
		Object obj = getContext().get("connections");
		if (obj != null) {
			getContext().remove("connections");			

			JSONArray connections = (JSONArray) obj;
			
			log(getUsername() + " disconnecting " + connections.size() + " agent(s)"); 
			
			for (int i = 0; i < connections.size(); i++) {
				String url = connections.getString(i);
				String urlSelf = getUrl();
				String method = "removeConnection";
				JSONObject params = new JSONObject();
				params.put("url", urlSelf);
				send(url, method, params);
			}			
		}		
	}
	
	/**
	 * Remove an agent from connections list
	 * @param url  Url of a connected ChatAgent
	 * @throws Exception 
	 */
	public void removeConnection(@ParameterName("url") String url) throws Exception {
		Object obj = getContext().get("connections");
		if (obj != null) {
			JSONArray connections = (JSONArray) obj;
			connections.remove(url);
			getContext().put("connections", connections);	
			
			log(getUsername() + " disconnected from " + url); 
		}
	}

	/**
	 * Retrieve the urls of all agents that are connected
	 * @return
	 */
	public JSONArray getConnections() {
		Object connections = getContext().get("connections");
		if (connections != null) {
			return (JSONArray) connections;
		}
		else {
			return new JSONArray();
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
			"First call setUsername to set the agents usernames. " +
			"Then use connect to connect an agent to another agent. " + 
			"They will automatically synchronize their adress lists. " +
			"Then, use post to post a message.";
	}

	@Override
	public String getVersion() {
		return "1.0";
	}
	
}
