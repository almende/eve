/*
 * Copyright: Almende B.V. (2014), Rotterdam, The Netherlands
 * License: The Apache Software License, Version 2.0
 */
package com.almende.eve.transport.xmpp;

import java.io.IOException;
import java.net.ProtocolException;
import java.net.URI;
import java.net.URISyntaxException;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;

import org.jivesoftware.smack.SmackConfiguration;

import com.almende.eve.agent.AgentHost;
import com.almende.eve.rpc.annotation.Access;
import com.almende.eve.rpc.annotation.AccessType;
import com.almende.eve.rpc.jsonrpc.jackson.JOM;
import com.almende.eve.state.State;
import com.almende.eve.transport.TransportService;
import com.almende.util.EncryptionUtil;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * The Class XmppService.
 */
public class XmppService implements TransportService {
	private static final String				CONNKEY				= "_XMPP_Connections";
	private AgentHost						agentHost			= null;
	private String							host				= null;
	private Integer							port				= null;
	private String							service				= null;
	/** The connections by url, xmpp url as key "xmpp:username@host". */
	private final Map<URI, AgentConnection>	connectionsByUrl	= new ConcurrentHashMap<URI, AgentConnection>();
	private static List<String>				protocols			= Arrays.asList("xmpp");
	private static final Logger				LOG					= Logger.getLogger(XmppService.class
																		.getSimpleName());
	
	/**
	 * Instantiates a new xmpp service.
	 */
	protected XmppService() {
	}
	
	// Needed to force Android loading the ReconnectionManager....
	static {
		try {
			Class.forName("org.jivesoftware.smack.ReconnectionManager");
		} catch (final ClassNotFoundException ex) {
			// problem loading reconnection manager
		}
	}
	
	/**
	 * Construct an XmppService
	 * This constructor is called when the TransportService is constructed
	 * by the AgentHost.
	 * 
	 * @param agentHost
	 *            the agent host
	 * @param params
	 *            Available parameters:
	 *            {String} host
	 *            {Integer} port
	 *            {String} serviceName
	 *            {String} id
	 */
	public XmppService(final AgentHost agentHost,
			final Map<String, Object> params) {
		this.agentHost = agentHost;
		
		if (params != null) {
			host = (String) params.get("host");
			port = (Integer) params.get("port");
			service = (String) params.get("service");
		}
		
		init();
	}
	
	/**
	 * initialize the settings for the xmpp service.
	 * 
	 * @param agentHost
	 *            the agent host
	 * @param host
	 *            the host
	 * @param port
	 *            the port
	 * @param service
	 *            service name
	 */
	public XmppService(final AgentHost agentHost, final String host,
			final Integer port, final String service) {
		this.agentHost = agentHost;
		this.host = host;
		this.port = port;
		this.service = service;
		
		init();
	}
	
	/**
	 * Gets the conns.
	 * 
	 * @param agentId
	 *            the agent id
	 * @return the conns
	 * @throws IOException
	 *             Signals that an I/O exception has occurred.
	 */
	private ArrayNode getConns(final String agentId) throws IOException {
		final State state = agentHost.getStateFactory().get(agentId);
		
		ArrayNode conns = null;
		if (state.containsKey(CONNKEY)) {
			conns = (ArrayNode) JOM.getInstance().readTree(
					state.get(CONNKEY, String.class));
		}
		return conns;
	}
	
	/**
	 * Get the first XMPP url of an agent from its id.
	 * If the agent exists (is not null) retrieve the current 'isConnected'
	 * status and return it.
	 * 
	 * @param agentUrl
	 *            The url of the agent
	 * @return connectionStatus
	 */
	public Boolean isConnected(final String agentUrl) {
		final AgentConnection connection = connectionsByUrl.get(agentUrl);
		
		if (connection == null) {
			return false;
		}
		
		LOG.info("Current connection of agent " + agentUrl + " is: "
				+ connection.isConnected());
		return connection.isConnected();
	}
	
	/**
	 * Get the first XMPP url of an agent from its id.
	 * If no agent with given id is connected via XMPP, null is returned.
	 * 
	 * @param agentId
	 *            The id of the agent
	 * @return agentUrl
	 */
	@Override
	public URI getAgentUrl(final String agentId) {
		try {
			final ArrayNode conns = getConns(agentId);
			if (conns != null) {
				for (final JsonNode conn : conns) {
					final ObjectNode params = (ObjectNode) conn;
					
					final String encryptedUsername = params.has("username") ? params
							.get("username").textValue() : null;
					final String encryptedResource = params.has("resource") ? params
							.get("resource").textValue() : null;
					if (encryptedUsername != null) {
						final String username = EncryptionUtil
								.decrypt(encryptedUsername);
						String resource = null;
						if (encryptedResource != null) {
							resource = EncryptionUtil
									.decrypt(encryptedResource);
						}
						return generateUrl(username, host, resource);
					}
				}
			}
		} catch (final Exception e) {
			LOG.log(Level.WARNING, "", e);
		}
		return null;
	}
	
	/**
	 * Get the id of an agent from its url.
	 * If no agent with given id is connected via XMPP, null is returned.
	 * 
	 * @param agentUrl
	 *            the agent url
	 * @return agentId
	 */
	@Override
	public String getAgentId(final URI agentUrl) {
		final AgentConnection connection = connectionsByUrl.get(agentUrl
				.toString());
		if (connection != null) {
			return connection.getAgentId();
		}
		return null;
	}
	
	/**
	 * initialize the transport service.
	 */
	private void init() {
		SmackConfiguration.setPacketReplyTimeout(15000);
	}
	
	/**
	 * Get the protocols supported by the XMPPService.
	 * Will return an array with one value, "xmpp"
	 * 
	 * @return protocols
	 */
	@Override
	public List<String> getProtocols() {
		return protocols;
	}
	
	/**
	 * Connect to the configured messaging service (such as XMPP). The service
	 * must be configured in the Eve configuration
	 * 
	 * @param agentId
	 *            the agent id
	 * @param username
	 *            the username
	 * @param password
	 *            the password
	 * @throws IOException
	 *             Signals that an I/O exception has occurred.
	 */
	@Access(AccessType.UNAVAILABLE)
	public final void connect(final String agentId, final String username,
			final String password) throws IOException {
		final String resource = null;
		connect(agentId, username, password, resource);
	}
	
	/**
	 * Connect to the configured messaging service (such as XMPP). The service
	 * must be configured in the Eve configuration
	 * 
	 * @param agentId
	 *            the agent id
	 * @param username
	 *            the username
	 * @param password
	 *            the password
	 * @param resource
	 *            (optional)
	 * @throws IOException
	 *             Signals that an I/O exception has occurred.
	 */
	@Access(AccessType.UNAVAILABLE)
	public final void connect(final String agentId, final String username,
			final String password, final String resource) throws IOException {
		// First store the connection info for later reconnection.
		try {
			storeConnection(agentId, username, password, resource);
		} catch (final Exception e) {
			LOG.log(Level.SEVERE, "Failed to store XMPP Connection.", e);
		}
		
		final URI agentUrl = generateUrl(username, host, resource);
		AgentConnection connection;
		if (connectionsByUrl.containsKey(agentUrl)) {
			LOG.warning("Warning, agent was already connected, reconnecting.");
			connection = connectionsByUrl.get(agentUrl);
		} else {
			// instantiate open the connection
			connection = new AgentConnection(agentHost);
		}
		
		if (username.indexOf('@') > 0) {
			LOG.warning("Warning: Username should not contain a domain! "
					+ username);
		}
		connection.connect(agentId, host, port, service, username, password,
				resource);
		connectionsByUrl.put(agentUrl, connection);
	}
	
	/**
	 * Store connection.
	 * 
	 * @param agentId
	 *            the agent id
	 * @param username
	 *            the username
	 * @param password
	 *            the password
	 * @param resource
	 *            the resource
	 * @throws IOException
	 *             Signals that an I/O exception has occurred.
	 * @throws InvalidKeyException
	 *             the invalid key exception
	 * @throws InvalidAlgorithmParameterException
	 *             the invalid algorithm parameter exception
	 * @throws NoSuchAlgorithmException
	 *             the no such algorithm exception
	 * @throws InvalidKeySpecException
	 *             the invalid key spec exception
	 * @throws NoSuchPaddingException
	 *             the no such padding exception
	 * @throws IllegalBlockSizeException
	 *             the illegal block size exception
	 * @throws BadPaddingException
	 *             the bad padding exception
	 */
	private void storeConnection(final String agentId, final String username,
			final String password, final String resource) throws IOException,
			InvalidKeyException, InvalidAlgorithmParameterException,
			NoSuchAlgorithmException, InvalidKeySpecException,
			NoSuchPaddingException, IllegalBlockSizeException,
			BadPaddingException {
		
		final State state = agentHost.getStateFactory().get(agentId);
		
		final String conns = state.get(CONNKEY, String.class);
		ArrayNode newConns;
		if (conns != null) {
			newConns = (ArrayNode) JOM.getInstance().readTree(conns);
		} else {
			newConns = JOM.createArrayNode();
		}
		
		final ObjectNode params = JOM.createObjectNode();
		params.put("username", EncryptionUtil.encrypt(username));
		params.put("password", EncryptionUtil.encrypt(password));
		if (resource != null && !resource.equals("")) {
			params.put("resource", EncryptionUtil.encrypt(resource));
		}
		for (final JsonNode item : newConns) {
			if (item.get("username").equals(params.get("username"))) {
				return;
			}
		}
		newConns.add(params);
		if (!state.putIfUnchanged(CONNKEY, JOM.getInstance()
				.writeValueAsString(newConns), conns)) {
			// recursive retry
			storeConnection(agentId, username, password, resource);
		}
	}
	
	/**
	 * Del connections.
	 * 
	 * @param agentId
	 *            the agent id
	 */
	private void delConnections(final String agentId) {
		final State state = agentHost.getStateFactory().get(agentId);
		if (state != null) {
			state.remove(CONNKEY);
		}
	}
	
	/**
	 * Disconnect the agent from the connected messaging service(s) (if any).
	 * 
	 * @param agentId
	 *            the agent id
	 * @throws InvalidKeyException
	 *             the invalid key exception
	 * @throws InvalidAlgorithmParameterException
	 *             the invalid algorithm parameter exception
	 * @throws NoSuchAlgorithmException
	 *             the no such algorithm exception
	 * @throws InvalidKeySpecException
	 *             the invalid key spec exception
	 * @throws NoSuchPaddingException
	 *             the no such padding exception
	 * @throws IllegalBlockSizeException
	 *             the illegal block size exception
	 * @throws BadPaddingException
	 *             the bad padding exception
	 * @throws IOException
	 *             Signals that an I/O exception has occurred.
	 */
	@Access(AccessType.UNAVAILABLE)
	public final void disconnect(final String agentId)
			throws InvalidKeyException, InvalidAlgorithmParameterException,
			NoSuchAlgorithmException, InvalidKeySpecException,
			NoSuchPaddingException, IllegalBlockSizeException,
			BadPaddingException, IOException {
		
		final ArrayNode conns = getConns(agentId);
		if (conns != null) {
			for (final JsonNode conn : conns) {
				final ObjectNode params = (ObjectNode) conn;
				
				final String encryptedUsername = params.has("username") ? params
						.get("username").textValue() : null;
				final String encryptedResource = params.has("resource") ? params
						.get("resource").textValue() : null;
				if (encryptedUsername != null) {
					final String username = EncryptionUtil
							.decrypt(encryptedUsername);
					String resource = null;
					if (encryptedResource != null) {
						resource = EncryptionUtil.decrypt(encryptedResource);
					}
					
					final URI url = generateUrl(username, host, resource);
					final AgentConnection connection = connectionsByUrl
							.get(url);
					if (connection != null) {
						connection.disconnect();
						connectionsByUrl.remove(url);
					}
				}
			}
		}
		delConnections(agentId);
	}
	
	/**
	 * Asynchronously Send a message to an other agent.
	 * 
	 * @param senderUrl
	 *            the sender url
	 * @param receiverUrl
	 *            the receiver
	 * @param message
	 *            the message
	 * @param tag
	 *            the tag
	 * @throws IOException
	 *             Signals that an I/O exception has occurred.
	 */
	@Override
	public void sendAsync(final URI senderUrl, final URI receiverUrl,
			final String message, final String tag) throws IOException {
		
		AgentConnection connection = null;
		
		if (senderUrl != null) {
			connection = connectionsByUrl.get(senderUrl);
		}
		if (connection != null) {
			// remove the protocol from the receiver url
			String receiver = receiverUrl.toString();
			final String protocol = "xmpp:";
			if (!receiver.startsWith(protocol)) {
				throw new ProtocolException("Receiver url must start with '"
						+ protocol + "' (receiver='" + receiver + "')");
			}
			// username@domain
			final String fullUsername = receiver.substring(protocol.length());
			connection.send(fullUsername, message);
		} else {
			// TODO: use an anonymous xmpp connection when the sender agent has
			// no xmpp connection.
			throw new IOException("Cannot send an xmpp request, "
					+ "agent has no xmpp connection.");
		}
	}
	
	/**
	 * Get the url of an xmpp connection "xmpp:username@host".
	 * 
	 * @param username
	 *            the username
	 * @param host
	 *            the host
	 * @param resource
	 *            optional
	 * @return url
	 * @throws URISyntaxException
	 */
	private static URI generateUrl(final String username, final String host,
			final String resource) {
		String url = "xmpp:" + username + "@" + host;
		if (resource != null && !resource.equals("")) {
			url += "/" + resource;
		}
		try {
			return new URI(url);
		} catch (URISyntaxException e) {
			LOG.warning("Strange, couldn't generate URI.");
			return null;
		}
	}
	
	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		final Map<String, Object> data = new HashMap<String, Object>();
		data.put("class", this.getClass().getName());
		data.put("host", host);
		data.put("port", port);
		data.put("service", service);
		data.put("protocols", protocols);
		
		return data.toString();
	}
	
	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * com.almende.eve.transport.TransportService#reconnect(java.lang.String)
	 */
	@Override
	public void reconnect(final String agentId) throws IOException {
		final ArrayNode conns = getConns(agentId);
		if (conns != null) {
			for (final JsonNode conn : conns) {
				final ObjectNode params = (ObjectNode) conn;
				LOG.info("Initializing connection:" + agentId + " --> "
						+ params);
				try {
					final String encryptedUsername = params.has("username") ? params
							.get("username").textValue() : null;
					final String encryptedPassword = params.has("password") ? params
							.get("password").textValue() : null;
					final String encryptedResource = params.has("resource") ? params
							.get("resource").textValue() : null;
					if (encryptedUsername != null && encryptedPassword != null) {
						final String username = EncryptionUtil
								.decrypt(encryptedUsername);
						final String password = EncryptionUtil
								.decrypt(encryptedPassword);
						String resource = null;
						if (encryptedResource != null) {
							resource = EncryptionUtil
									.decrypt(encryptedResource);
						}
						connect(agentId, username, password, resource);
					}
				} catch (final Exception e) {
					throw new IOException("Failed to connect XMPP.", e);
				}
			}
		}
	}
	
	/*
	 * (non-Javadoc)
	 * 
	 * @see com.almende.eve.transport.TransportService#getKey()
	 */
	@Override
	public String getKey() {
		return "xmpp://" + host + ":" + port + "/" + service;
	}
	
	/**
	 * Ping.
	 * 
	 * @param senderUrl
	 *            the sender url
	 * @param receiver
	 *            the receiver
	 * @return true, if successful
	 */
	public boolean ping(final String senderUrl, final String receiver) {
		final AgentConnection connection = connectionsByUrl.get(senderUrl);
		return connection.isAvailable(receiver);
	}
	
}
