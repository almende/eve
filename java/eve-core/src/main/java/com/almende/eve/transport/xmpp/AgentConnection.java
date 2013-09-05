package com.almende.eve.transport.xmpp;

import java.util.logging.Level;
import java.util.logging.Logger;

import org.jivesoftware.smack.ConnectionConfiguration;
import org.jivesoftware.smack.PacketListener;
import org.jivesoftware.smack.Roster;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smack.packet.Packet;
import org.jivesoftware.smack.packet.Presence;

import com.almende.eve.agent.AgentHost;
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
	private static final Logger					LOG			= Logger.getLogger(AgentConnection.class
																	.getCanonicalName());
	private AgentHost							agentHost	= null;
	private String								agentId		= null;
	private String								username	= null;
	private String								resource	= null;
	private XMPPConnection						conn		= null;
	private AsyncCallbackQueue<JSONResponse>	callbacks	= new AsyncCallbackQueue<JSONResponse>();
	
	public AgentConnection(AgentHost agentHost) {
		this.agentHost = agentHost;
	}
	
	/**
	 * Get the id of the agent linked to this connection
	 * 
	 * @return agentId
	 */
	public String getAgentId() {
		return agentId;
	}
	
	/**
	 * Get the username of the connection (without host)
	 * 
	 * @return username
	 */
	public String getUsername() {
		return username;
	}
	
	/**
	 * Get the resource of the connection. Returns null if no resource is set
	 * 
	 * @return resource
	 */
	public String getResource() {
		return resource;
	}
	
	/**
	 * Login and connect the agent to the messaging service
	 * 
	 * @param agentId
	 * @param host
	 * @param port
	 * @param serviceName
	 * @param username
	 * @param password
	 * @param resource
	 *            optional
	 * @throws JSONRPCException
	 */
	public void connect(String agentId, String host, Integer port,
			String serviceName, String username, String password,
			String resource) throws JSONRPCException {
		
		if (isConnected()) {
			// this is a reconnect.
			disconnect();
		}
		this.agentId = agentId;
		this.username = username;
		this.resource = resource;
		
		try {
			// configure and connect
			ConnectionConfiguration connConfig = new ConnectionConfiguration(
					host, port, serviceName);
			
			connConfig.setSASLAuthenticationEnabled(true);
			connConfig.setReconnectionAllowed(true);
			connConfig.setCompressionEnabled(true);
			connConfig.setRosterLoadedAtLogin(false);
			conn = new XMPPConnection(connConfig);
			conn.connect();
			
			// login
			if (resource == null) {
				conn.login(username, password);
			} else {
				conn.login(username, password, resource);
			}
			
			// set presence to available
			Presence presence = new Presence(Presence.Type.available);
			conn.sendPacket(presence);
			
			// set acceptance to all
			conn.getRoster().setSubscriptionMode(
					Roster.SubscriptionMode.accept_all);
			
			// instantiate a packet listener
			conn.addPacketListener(new JSONRPCListener(conn, agentHost,
					agentId, resource, callbacks), null);
		} catch (XMPPException e) {
			LOG.log(Level.WARNING, "", e);
			throw new JSONRPCException("Failed to connect to messenger", e);
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
	 * 
	 * @return connected
	 */
	public boolean isConnected() {
		return (conn != null) ? conn.isConnected() : false;
	}
	
	/**
	 * Send a message to an other agent
	 * 
	 * @param username
	 * @param message
	 * @throws JSONRPCException
	 */
	public void send(String username, JSONRequest request,
			AsyncCallback<JSONResponse> callback) throws JSONRPCException {
		try {
			if (isConnected()) {
				// create a unique id
				final String id = (String) request.getId();
				
				
				String description = username + " -> "+ request.getMethod();
				// queue the response callback
				callbacks.push(id, description, callback);
				
				// send the message
				Message reply = new Message();
				reply.setTo(username);
				reply.setBody(request.toString());
				conn.sendPacket(reply);
			} else {
				throw new Exception("Cannot send request, not connected");
			}
		} catch (Exception e) {
			throw new JSONRPCException("Failed to send RPC through XMPP.", e);
		}
	}
	
	/**
	 * A class to listen for incoming JSON-RPC messages.
	 * The listener will invoke the JSON-RPC message on the agent and
	 * reply the result.
	 */
	private static class JSONRPCListener implements PacketListener {
		private XMPPConnection						conn			= null;
		private AgentHost							host			= null;
		private String								agentId			= null;
		private AsyncCallbackQueue<JSONResponse>	callbacks		= null;
		private String								resource		= null;
		
		public JSONRPCListener(XMPPConnection conn, AgentHost agentHost,
				String agentId, String resource, AsyncCallbackQueue<JSONResponse> callbacks) {
			this.conn = conn;
			this.host = agentHost;
			this.agentId = agentId;
			this.callbacks = callbacks;
			this.resource = resource;
		}
		
		/**
		 * Check if given json object contains all fields required for a
		 * json-rpc request (id, method, params)
		 * 
		 * @param json
		 * @return
		 */
		private boolean isRequest(ObjectNode json) {
			return json.has("method");
		}
		
		/**
		 * Check if given json object contains all fields required for a
		 * json-rpc response (id, result or error)
		 * 
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
		 * 
		 * @param packet
		 */
		public void processPacket(Packet packet) {
			Message message = (Message) packet;
			
			//Check if resource is given and matches local resource. If not equal, silently drop packet.
			String to = message.getTo();
			if (resource != null && to != null){
				int index = to.indexOf('/');
				if (index > 0){
					String resource = to.substring(index+1);
					if (!this.resource.equals(resource)){
						LOG.warning("Received stanza meant for another agent, disregarding.");
						return;
					}
				}
			}
			String body = message.getBody();
			
			if (body != null && body.startsWith("{")
					|| body.trim().startsWith("{")) {
				// the body contains a JSON object
				ObjectNode json = null;
				try {
					json = JOM.getInstance().readValue(body, ObjectNode.class);
					if (isResponse(json)) {
						// this is a response
						// Find and execute the corresponding callback
						String id = json.has("id") ? json.get("id").asText()
								: null;
						AsyncCallback<JSONResponse> callback = (id != null) ? callbacks
								.pull(id) : null;
						if (callback != null) {
							callback.onSuccess(new JSONResponse(body));
						}
					} else if (isRequest(json)) {
						// this is a request
						String senderUrl = "xmpp:"+message.getFrom();
						JSONRequest request = new JSONRequest(json);
						invoke(senderUrl, request);
					} else {
						throw new Exception(
								"Request does not contain a valid JSON-RPC request or response");
					}
				} catch (Exception e) {
					// generate JSON error response
					JSONRPCException jsonError = new JSONRPCException(
							JSONRPCException.CODE.INTERNAL_ERROR,
							e.getMessage(), e);
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
		 * 
		 * @param senderUrl
		 * @param request
		 */
		private void invoke(final String senderUrl, final JSONRequest request) {
			new Thread(new Runnable() {
				@Override
				public void run() {
					JSONResponse response;
					try {
						// append the sender to the request parameters
						RequestParams params = new RequestParams();
						params.put(Sender.class, senderUrl);
						
						// invoke the agent
						response = host.receive(agentId, request,
								params);
					} catch (Exception err) {
						// generate JSON error response
						JSONRPCException jsonError = new JSONRPCException(
								JSONRPCException.CODE.INTERNAL_ERROR,
								err.getMessage(), err);
						response = new JSONResponse(jsonError);
					}
					
					if (response != null) {
						Message reply = new Message();
						String sender = senderUrl.replaceFirst("xmpps?:", "");
						reply.setTo(sender);
						reply.setBody(response.toString());
						conn.sendPacket(reply);
					} else {
						LOG.severe("XMPP response is null? This shouldn't happen...");
					}
				}
			}).start();
		}
	}
}
