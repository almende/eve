package com.almende.eve.transport.xmpp;

import java.io.IOException;
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
import com.almende.eve.rpc.jsonrpc.JSONRPCException;

public class AgentConnection {
	private static final Logger					LOG			= Logger.getLogger(AgentConnection.class
																	.getCanonicalName());
	private AgentHost							agentHost	= null;
	private String								agentId		= null;
	private String								username	= null;
	private String								host		= null;
	private String								resource	= null;
	private String								password	= null;
	private String								serviceName	= null;
	private Integer								port		= 5222;
	private XMPPConnection						conn		= null;
	
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
	
	public void connect() throws IOException {
		connect(agentId, host, port, serviceName, username, password, resource);
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
			String resource) throws IOException {
		
		if (isConnected()) {
			// this is a reconnect.
			disconnect();
		}
		this.agentId = agentId;
		this.username = username;
		this.resource = resource;
		this.host = host;
		this.port = port;
		this.serviceName = serviceName;
		this.password = password;
		
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
			conn.addPacketListener(new JSONRPCListener(agentHost,
					agentId, resource), null);
			
		} catch (XMPPException e) {
			LOG.log(Level.WARNING, "", e);
			throw new IOException("Failed to connect to messenger", e);
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
	public void send(String username, Object message) throws IOException {
		if (!isConnected()) {
			disconnect();
			connect();
		}
		if (isConnected()) {
			
			// send the message
			Message reply = new Message();
			reply.setTo(username);
			reply.setBody(message.toString());
			conn.sendPacket(reply);
		} else {
			throw new IOException("Cannot send request, not connected");
		}
	}
	
	/**
	 * A class to listen for incoming JSON-RPC messages.
	 * The listener will invoke the JSON-RPC message on the agent and
	 * reply the result.
	 */
	private static class JSONRPCListener implements PacketListener {
		private AgentHost							host		= null;
		private String								agentId		= null;
		private String								resource	= null;
		
		public JSONRPCListener(AgentHost agentHost,
				String agentId, String resource) {
			this.host = agentHost;
			this.agentId = agentId;
			this.resource = resource;
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
			
			// Check if resource is given and matches local resource. If not
			// equal, silently drop packet.
			String to = message.getTo();
			if (resource != null && to != null) {
				int index = to.indexOf('/');
				if (index > 0) {
					String resource = to.substring(index + 1);
					if (!this.resource.equals(resource)) {
						LOG.warning("Received stanza meant for another agent, disregarding. "
								+ resource);
						return;
					}
				}
			}
			String body = message.getBody();
			String senderUrl = "xmpp:" + message.getFrom();
			
			if (body != null) {
				invoke(senderUrl, body);
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
		
		// TODO: refactor this to reuse the ZmqConnection structure (with a
		// AsyncCallback)
		private void invoke(final String senderUrl, final Object message) {
			new Thread(new Runnable() {
				@Override
				public void run() {
					host.receive(agentId, message, senderUrl,null);
				}
			}).start();
		}
	}
	
	public boolean isAvailable(String receiver) {
		// split url (xmpp:user/resource) into parts
		if (receiver.startsWith("xmpp:")) receiver = receiver.substring(5,
				receiver.length());
		int slash = receiver.indexOf("/");
		if (slash <= 0) return false;
		String resource = receiver.substring(slash + 1, receiver.length());
		String user = receiver.substring(0, slash);
		
		Roster roster = this.conn.getRoster();
		
		org.jivesoftware.smack.RosterEntry re = roster.getEntry(user);
		if (re == null) {
			LOG.info("subscribing to " + receiver);
			Presence subscribe = new Presence(Presence.Type.subscribe);
			subscribe.setTo(receiver);
			conn.sendPacket(subscribe);
		}
		
		Presence p = roster.getPresenceResource(user + "/" + resource);
		LOG.info("Presence for " + user + "/" + resource + " : " + p.getType());
		if (p.isAvailable()) return true;
		
		/*
		 * try resubscribe?
		 * Presence unsubscribe = new Presence(Presence.Type.unsubscribe);
		 * unsubscribe.setTo( receiver );
		 * conn.sendPacket(unsubscribe);
		 * 
		 * Presence subscribe = new Presence(Presence.Type.subscribe);
		 * subscribe.setTo( receiver );
		 * conn.sendPacket(subscribe);
		 */
		
		return false;
	}
	
}
