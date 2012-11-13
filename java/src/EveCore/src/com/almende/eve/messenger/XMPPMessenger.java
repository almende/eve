package com.almende.eve.messenger;

import org.jivesoftware.smack.ConnectionConfiguration;
import org.jivesoftware.smack.PacketListener;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smack.packet.Packet;
import org.jivesoftware.smack.packet.Presence;
import org.jivesoftware.smack.util.StringUtils;

import com.almende.eve.agent.Agent;
import com.almende.eve.config.Config;
import com.almende.eve.context.Context;
import com.almende.eve.json.JSONRPC;
import com.almende.eve.json.JSONRPCException;
import com.almende.eve.json.JSONRequest;
import com.almende.eve.json.JSONResponse;
import com.almende.eve.json.jackson.JOM;
import com.fasterxml.jackson.databind.node.ObjectNode;

public class XMPPMessenger implements Messenger {
	private Agent agent = null;
	private XMPPConnection conn = null;
	private AsyncCallbackQueue<JSONResponse> callbacks = 
			new AsyncCallbackQueue<JSONResponse>();
	
	public XMPPMessenger () {
	}

	public XMPPMessenger (Agent agent) {
		setAgent(agent);
	}
	
	@Override
	public void setAgent(Agent agent) {
		this.agent = agent;
	};
	
	/**
	 * Login and connect the agent to the messaging service
	 * @param username
	 * @param password
	 * @throws Exception 
	 */
	@Override
	public void connect(String username, String password) throws Exception {
		if (agent == null) {
			throw new IllegalArgumentException("No agent instantiated to connect to messenger");
		}
		
		// get configuration
		Context context = agent.getContext();
		Config config = context.getConfig();
		String env = context.getEnvironment();
		String host        = config.get("environment", env, "messenger", "host");
		Integer port       = config.get("environment", env, "messenger", "port");
		String serviceName = config.get("environment", env, "messenger", "serviceName");

		try {
			// configure and connect
			ConnectionConfiguration connConfig = 
					new ConnectionConfiguration(host, port, serviceName);
			conn = new XMPPConnection(connConfig);
			conn.connect();

			// login
			conn.login(username, password);

			// set presence
			Presence presence = new Presence(Presence.Type.available);
			conn.sendPacket(presence);

			// instantiate a packet listener
			conn.addPacketListener(new JSONRPCListener(conn, agent, callbacks), null);            
			
		} catch (XMPPException err) {
			err.printStackTrace();
			throw new Exception("Failed to connect to messenger");
		}
	}
	
	/**
	 * Disconnect the agent from the messaging service
	 */
	@Override
	public void disconnect() {
		if (conn != null) {
			conn.disconnect();
			conn = null;
		}
		callbacks.clear();
	}

	/**
	 * Check whether the agent is connected to the messaging service
	 * @return connected
	 */
	@Override
	public boolean isConnected() {
		return (conn != null) ? conn.isConnected() : false;
	}

	/**
	 * Send a message to an other agent
	 * @param username
	 * @param message
	 * @throws Exception 
	 */
	@Override
	public void send (String username, JSONRequest request, 
			AsyncCallback<JSONResponse> callback) throws Exception {
		if (isConnected()) {
			// create a unique id
			final String id = (String) request.getId();
			
			// queue the response callback
			callbacks.push(id, callback);
			
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
		private XMPPConnection conn;
		private Agent agent;
		private AsyncCallbackQueue<JSONResponse> callbacks;

		public JSONRPCListener (XMPPConnection conn, Agent agent, 
				AsyncCallbackQueue<JSONResponse> callbacks) {
			this.conn = conn;
			this.agent = agent;
			this.callbacks = callbacks;
		}

		/**
		 * Check if given json object contains all fields required for a 
		 * json-rpc request (id, method, params)
		 * @param json
		 * @return
		 */
		private boolean isRequest(ObjectNode json) {
			return json.has("id") && json.has("method") && json.has("params");
		}

		/**
		 * Check if given json object contains all fields required for a 
		 * json-rpc response (id, result or error)
		 * @param json
		 * @return
		 */
		private boolean isResponse(ObjectNode json) {
			return json.has("id") && (json.has("result") || json.has("error"));
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
			if (body != null && body.startsWith("{") || body.trim().startsWith("{")) {
				// the body contains a JSON object
				ObjectNode json = null;
				JSONResponse response = null;					
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
							// TODO: is it needed to send this error back?
							throw new Exception("Callback with id '" + id + "' not found");
						}
					}
					else if (isRequest(json)) {
						// this is a request
						JSONRequest request = new JSONRequest(json);
						response = JSONRPC.invoke(agent, request);
					}
					else {
						throw new Exception("Request does not contain a valid JSON-RPC request or response");
					}
				}
				catch (Exception err) {
					// generate JSON error response
					JSONRPCException jsonError = new JSONRPCException(
							JSONRPCException.CODE.INTERNAL_ERROR, err.getMessage());
					response = new JSONResponse(jsonError);
				}

				// send a response (when needed)
				if (response != null) {
					String from = StringUtils.parseBareAddress(message.getFrom());
					Message reply = new Message();
					reply.setTo(from);
					reply.setBody(response.toString());
					conn.sendPacket(reply);
				}
			}
		}
	}
}

