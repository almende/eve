package com.almende.eve.transport.xmpp;

import org.jivesoftware.smack.ConnectionConfiguration;
import org.jivesoftware.smack.PacketListener;
import org.jivesoftware.smack.Roster;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smack.packet.Packet;
import org.jivesoftware.smack.packet.Presence;

import com.almende.eve.agent.AgentFactory;
import com.almende.eve.rpc.RequestParams;
import com.almende.eve.rpc.annotation.Sender;
import com.almende.eve.rpc.jsonrpc.JSONRPCException;
import com.almende.eve.rpc.jsonrpc.JSONRequest;
import com.almende.eve.rpc.jsonrpc.JSONResponse;
import com.almende.eve.rpc.jsonrpc.jackson.JOM;
import com.almende.eve.transport.AsyncCallback;
import com.almende.eve.transport.AsyncCallbackQueue;
import com.fasterxml.jackson.databind.node.ObjectNode;

public class AgentConnection {
	private AgentFactory agentFactory = null;
	private String agentId = null;
	private String username = null;
	private String resource = null;
	private XMPPConnection conn = null;
	private AsyncCallbackQueue<JSONResponse> callbacks = 
			new AsyncCallbackQueue<JSONResponse>();	
	
	public AgentConnection (AgentFactory agentFactory) {
		this.agentFactory = agentFactory;
	}
	
	/**
	 * Get the id of the agent linked to this connection
	 * @return agentId
	 */
	public String getAgentId() {
		return agentId;
	}
	
	/**
	 * Get the username of the connection (without host)
	 * @return username 
	 */
	public String getUsername() {
		return username;
	}
	
	/**
	 * Get the resource of the connection. Returns null if no resource is set
	 * @return resource
	 */
	public String getResource() {
		return resource;
	}
	/**
	 * Login and connect the agent to the messaging service
	 * @param agentId
	 * @param host
	 * @param port
	 * @param serviceName
	 * @param username
	 * @param password
	 * @param resource    optional
	 * @throws Exception 
	 */
	public void connect(String agentId, String host, Integer port, 
			String serviceName, String username, String password, 
			String resource) throws Exception {
		
		if (isConnected()){
			//this is a reconnect.
			disconnect();
		}
		this.agentId = agentId;
		this.username = username;
		this.resource = resource;
		
		try {
			// configure and connect
			ConnectionConfiguration connConfig = 
					new ConnectionConfiguration(host, port, serviceName);
			conn = new XMPPConnection(connConfig);
			conn.connect();

			// login
			if (resource == null) {
				conn.login(username, password);
			}
			else {
				conn.login(username, password, resource);
			}

			// set presence to available
			Presence presence = new Presence(Presence.Type.available);
			conn.sendPacket(presence);
			
			// set acceptance to all
			conn.getRoster().setSubscriptionMode(Roster.SubscriptionMode.accept_all);
			
			// instantiate a packet listener
			conn.addPacketListener(new JSONRPCListener(conn, agentFactory, 
					agentId, callbacks), null);            
		} catch (XMPPException err) {
			err.printStackTrace();
			throw new Exception("Failed to connect to messenger");
		}
	}
	
	/**
	 * Disconnect the agent from the messaging service
	 */
	public void disconnect() {
		if (isConnected()) {
			conn.disconnect();
			conn = null;
		}
		callbacks.clear();
	}

	/**
	 * Check whether the agent is connected to the messaging service
	 * @return connected
	 */
	public boolean isConnected() {
		return (conn != null) ? conn.isConnected() : false;
	}

	/**
	 * Send a message to an other agent
	 * @param username
	 * @param message
	 * @throws Exception 
	 */
	public void send (String username, JSONRequest request, 
			AsyncCallback<JSONResponse> callback) throws Exception {
		if (isConnected()) {
			// create a unique id
			final String id = (String) request.getId();
			
			// queue the response callback
			callbacks.push(id, callback);
			
			// System.out.println("send username=" + username + ", request=" + request); // TODO: cleanup
			
			// send the message
			Message reply = new Message();
			reply.setTo(username);
			reply.setBody(request.toString());
			conn.sendPacket(reply);
		}
		else {
			throw new Exception("Cannot send request, not connected");
		}
	}
	
	/**
	 * A class to listen for incoming JSON-RPC messages.
	 * The listener will invoke the JSON-RPC message on the agent and
	 * reply the result.
	 */
	private static class JSONRPCListener implements PacketListener {
		private XMPPConnection conn = null;
		private AgentFactory agentFactory = null; 
		private String agentId = null;
		private AsyncCallbackQueue<JSONResponse> callbacks = null;

		public JSONRPCListener (XMPPConnection conn, AgentFactory agentFactory,
				String agentId, AsyncCallbackQueue<JSONResponse> callbacks) {
			this.conn = conn;
			this.agentFactory = agentFactory;
			this.agentId = agentId;
			this.callbacks = callbacks;
		}

		/**
		 * Check if given json object contains all fields required for a 
		 * json-rpc request (id, method, params)
		 * @param json
		 * @return
		 */
		private boolean isRequest(ObjectNode json) {
			return json.has("method");
		}

		/**
		 * Check if given json object contains all fields required for a 
		 * json-rpc response (id, result or error)
		 * @param json
		 * @return
		 */
		private boolean isResponse(ObjectNode json) {
			return (json.has("result") || json.has("error"));
		}
		
		/**
		 * process an incoming xmpp message. 
		 * If the message contains a valid JSON-RPC request or response,
		 * the message will be processed.
		 * @param packet
		 */
		public void processPacket(Packet packet) {
			Message message = (Message)packet;
			String body = message.getBody();
			// System.out.println("recieve from=" + message.getFrom() + " to=" + message.getTo() + " body=" + body); // TODO: cleanup
			
			if (body != null && body.startsWith("{") || body.trim().startsWith("{")) {
				// the body contains a JSON object
				ObjectNode json = null;
				try {
					json = JOM.getInstance().readValue(body, ObjectNode.class);
					if (isResponse(json)) {
						// this is a response
						// Find and execute the corresponding callback
						String id = json.has("id") ? json.get("id").asText() : null;
						AsyncCallback<JSONResponse> callback = 
								(id != null) ? callbacks.pull(id) : null;
						if (callback != null) {
							callback.onSuccess(new JSONResponse(body));
						}
						else {
							/*
							// TODO: is it needed to send this error back? 
							// can possibly result in weird loops?
							throw new Exception("Callback with id '" + id + "' not found");
							*/
						}
					}
					else if (isRequest(json)) {
						// this is a request
						String senderUrl = message.getFrom();
						JSONRequest request = new JSONRequest(json);
						invoke(senderUrl, request);
					}
					else {
						throw new Exception("Request does not contain a valid JSON-RPC request or response");
					}
				}
				catch (Exception err) {
					// generate JSON error response
					JSONRPCException jsonError = new JSONRPCException(
							JSONRPCException.CODE.INTERNAL_ERROR, err.getMessage());
					JSONResponse response = new JSONResponse(jsonError);
					
					// send exception as response
					Message reply = new Message();
					reply.setTo(message.getFrom());
					reply.setBody(response.toString());
					conn.sendPacket(reply);
				}
			}
		}
	
		/**
		 * Invoke a JSON-RPC request
		 * Invocation is done in a separate thread to prevent blocking the
		 * single threaded XMPP PacketListener (which can cause deadlocks).
		 * @param senderUrl
		 * @param request
		 */
		private void invoke (final String senderUrl, final JSONRequest request) {
			new Thread(new Runnable () {
				@Override
				public void run() {
					JSONResponse response;
					try {
						// append the sender to the request parameters
						RequestParams params = new RequestParams();
						params.put(Sender.class, senderUrl);

						// invoke the agent
						response = agentFactory.receive(agentId, request, params);
					} catch (Exception err) {
						// generate JSON error response
						JSONRPCException jsonError = new JSONRPCException(
								JSONRPCException.CODE.INTERNAL_ERROR, err.getMessage());
						response = new JSONResponse(jsonError);
					}
					
					if (response != null) {
						//String from = StringUtils.parseBareAddress(senderUrl);
						Message reply = new Message();
						reply.setTo(senderUrl);
						reply.setBody(response.toString());
						conn.sendPacket(reply);
					}
				}
			}).start();		
		}
	}
}

