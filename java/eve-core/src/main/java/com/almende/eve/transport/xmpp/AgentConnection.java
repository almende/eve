/*
 * Copyright: Almende B.V. (2014), Rotterdam, The Netherlands
 * License: The Apache Software License, Version 2.0
 */
package com.almende.eve.transport.xmpp;

import java.io.IOException;
import java.net.URI;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.jivesoftware.smack.ConnectionConfiguration;
import org.jivesoftware.smack.PacketListener;
import org.jivesoftware.smack.Roster;
import org.jivesoftware.smack.SASLAuthentication;
import org.jivesoftware.smack.SmackException;
import org.jivesoftware.smack.SmackException.NotConnectedException;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.ConnectionConfiguration.SecurityMode;
import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smack.packet.Packet;
import org.jivesoftware.smack.packet.Presence;
import org.jivesoftware.smack.tcp.XMPPTCPConnection;

import com.almende.eve.agent.AgentHost;

/**
 * @author Almende
 * 
 */
/**
 * The Class AgentConnection.
 */
public class AgentConnection {
	
	private static final Logger	LOG			= Logger.getLogger(AgentConnection.class
													.getCanonicalName());
	private AgentHost			agentHost	= null;
	private String				agentId		= null;
	private String				username	= null;
	private String				host		= null;
	private String				resource	= null;
	private String				password	= null;
	private String				serviceName	= null;
	private Integer				port		= 5222;
	private XMPPConnection		conn		= null;
	
	/**
	 * Instantiates a new agent connection.
	 * 
	 * @param agentHost
	 *            the agent host
	 */
	public AgentConnection(final AgentHost agentHost) {
		this.agentHost = agentHost;
	}
	
	/**
	 * Get the id of the agent linked to this connection.
	 * 
	 * @return agentId
	 */
	public String getAgentId() {
		return agentId;
	}
	
	/**
	 * Get the username of the connection (without host).
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
	 * Connect.
	 * 
	 * @throws IOException
	 *             Signals that an I/O exception has occurred.
	 */
	public void connect() throws IOException {
		connect(agentId, host, port, serviceName, username, password, resource);
	}
	
	/**
	 * Login and connect the agent to the messaging service.
	 * 
	 * @param agentId
	 *            the agent id
	 * @param host
	 *            the host
	 * @param port
	 *            the port
	 * @param serviceName
	 *            the service name
	 * @param username
	 *            the username
	 * @param password
	 *            the password
	 * @param resource
	 *            optional
	 * @throws IOException
	 *             Signals that an I/O exception has occurred.
	 */
	public void connect(final String agentId, final String host,
			final Integer port, final String serviceName,
			final String username, final String password, final String resource)
			throws IOException {
		
		if (isConnected()) {
			// this is a reconnect.
			try {
                            disconnect();
                        }catch (NotConnectedException e) {
                            e.printStackTrace();
                        }
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
			final ConnectionConfiguration connConfig = new ConnectionConfiguration(
					host, port, serviceName);
			
			//connConfig.setSASLAuthenticationEnabled(true);
			connConfig.setSecurityMode(SecurityMode.disabled);
			connConfig.setReconnectionAllowed(true);
			connConfig.setCompressionEnabled(true);
			connConfig.setRosterLoadedAtLogin(false);
			conn = new XMPPTCPConnection(connConfig);
			conn.connect();
			
			SASLAuthentication.supportSASLMechanism("PLAIN");
			
			// login
			if (resource == null) {
				conn.login(username, password);
			} else {
				conn.login(username, password, resource);
			}
			
			// set presence to available
			final Presence presence = new Presence(Presence.Type.available);
			conn.sendPacket(presence);
			
			// set acceptance to all
			conn.getRoster().setSubscriptionMode(
					Roster.SubscriptionMode.accept_all);
			
			// instantiate a packet listener
			conn.addPacketListener(new JSONRPCListener(agentHost, agentId,
					resource), null);
			
		} catch (final XMPPException e) {
			LOG.log(Level.WARNING, "", e);
			throw new IOException("Failed to connect to messenger", e);
		}
        catch (SmackException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
	}
	
	/**
	 * Disconnect the agent from the messaging service.
	 * @throws NotConnectedException 
	 */
	public void disconnect() throws NotConnectedException {
		if (isConnected()) {
			conn.disconnect();
			conn = null;
		}
	}
	
	/**
	 * Check whether the agent is connected to the messaging service.
	 * 
	 * @return connected
	 */
	public boolean isConnected() {
		return (conn != null) ? conn.isConnected() : false;
	}
	
	/**
	 * Send a message to an other agent.
	 * 
	 * @param username
	 *            the username
	 * @param message
	 *            the message
	 * @throws IOException
	 *             Signals that an I/O exception has occurred.
	 * @throws NotConnectedException 
	 */
	public void send(final String username, final String message)
			throws IOException, NotConnectedException {
		if (!isConnected()) {
			disconnect();
			connect();
		}
		if (isConnected()) {
			
			// send the message
			final Message reply = new Message();
			reply.setTo(username);
			reply.setBody(message);
			conn.sendPacket(reply);
		} else {
			throw new IOException("Cannot send request, not connected");
		}
	}
	
	/**
	 * A class to listen for incoming JSON-RPC messages.
	 * The listener will invoke the JSON-RPC message on the agent and
	 * reply the result.
	 * 
	 * @see JSONRPCEvent
	 */
	private static class JSONRPCListener implements PacketListener {
		private AgentHost	host		= null;
		private String		agentId		= null;
		private String		resource	= null;
		
		/**
		 * Instantiates a new jSONRPC listener.
		 * 
		 * @param agentHost
		 *            the agent host
		 * @param agentId
		 *            the agent id
		 * @param resource
		 *            the resource
		 */
		public JSONRPCListener(final AgentHost agentHost, final String agentId,
				final String resource) {
			host = agentHost;
			this.agentId = agentId;
			this.resource = resource;
		}
		
		/**
		 * process an incoming xmpp message.
		 * If the message contains a valid JSON-RPC request or response,
		 * the message will be processed.
		 * 
		 * @param packet
		 *            the packet
		 */
		@Override
		public void processPacket(final Packet packet) {
			final Message message = (Message) packet;
			
			// Check if resource is given and matches local resource. If not
			// equal, silently drop packet.
			final String to = message.getTo();
			if (resource != null && to != null) {
				final int index = to.indexOf('/');
				if (index > 0) {
					final String res = to.substring(index + 1);
					if (!resource.equals(res)) {
						LOG.warning("Received stanza meant for another agent, disregarding. "
								+ res);
						return;
					}
				}
			}
			final String body = message.getBody();
			final URI senderUrl = URI.create("xmpp:" + message.getFrom());
			
			if (body != null) {
				try {
					host.receive(agentId, body, senderUrl, null);
				} catch (final IOException e) {
					LOG.log(Level.WARNING,
							"Host threw an IOException, probably agent '"
									+ agentId + "' doesn't exist? ", e);
					return;
				}
			}
		}
	}
	
	/**
	 * Checks if is available.
	 * 
	 * @param receiver
	 *            the receiver
	 * @return true, if is available
	 * @throws NotConnectedException 
	 */
	public boolean isAvailable(String receiver) throws NotConnectedException {
		// split url (xmpp:user/resource) into parts
		if (receiver.startsWith("xmpp:")) {
			receiver = receiver.substring(5, receiver.length());
		}
		final int slash = receiver.indexOf('/');
		if (slash <= 0) {
			return false;
		}
		final String res = receiver.substring(slash + 1, receiver.length());
		final String user = receiver.substring(0, slash);
		
		final Roster roster = conn.getRoster();
		
		final org.jivesoftware.smack.RosterEntry re = roster.getEntry(user);
		if (re == null) {
			LOG.info("subscribing to " + receiver);
			final Presence subscribe = new Presence(Presence.Type.subscribe);
			subscribe.setTo(receiver);
			conn.sendPacket(subscribe);
		}
		
		final Presence p = roster.getPresenceResource(user + "/" + res);
		LOG.info("Presence for " + user + "/" + res + " : " + p.getType());
		if (p.isAvailable()) {
			return true;
		}
		return false;
	}
}
