package com.almende.eve.transport.xmpp;

import java.io.IOException;
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
import com.almende.eve.rpc.jsonrpc.JSONRPCException;
import com.almende.eve.rpc.jsonrpc.JSONRequest;
import com.almende.eve.rpc.jsonrpc.JSONResponse;
import com.almende.eve.rpc.jsonrpc.jackson.JOM;
import com.almende.eve.state.State;
import com.almende.eve.transport.AsyncCallback;
import com.almende.eve.transport.SyncCallback;
import com.almende.eve.transport.TransportService;
import com.almende.util.EncryptionUtil;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

public class XmppService implements TransportService {
	private static final String				CONNKEY				= "_XMPP_Connections";
	private AgentHost						agentHost			= null;
	private String							host				= null;
	private Integer							port				= null;
	private String							service				= null;
	
	// xmpp url as key "xmpp:username@host"
	private Map<String, AgentConnection>	connectionsByUrl	= new ConcurrentHashMap<String, AgentConnection>();
	private static List<String>				protocols			= Arrays.asList("xmpp");
	
	private static final Logger				LOG					= Logger.getLogger(XmppService.class
																		.getSimpleName());
	
	protected XmppService() {
	}
	
	/**
	 * Construct an XmppService
	 * This constructor is called when the TransportService is constructed
	 * by the AgentHost
	 * 
	 * @param params
	 *            Available parameters:
	 *            {String} host
	 *            {Integer} port
	 *            {String} serviceName
	 *            {String} id
	 */
	public XmppService(AgentHost agentHost, Map<String, Object> params) {
		this.agentHost = agentHost;
		
		if (params != null) {
			host = (String) params.get("host");
			port = (Integer) params.get("port");
			service = (String) params.get("service");
		}
		
		init();
	}
	
	/**
	 * initialize the settings for the xmpp service
	 * 
	 * @param host
	 * @param port
	 * @param service
	 *            service name
	 */
	public XmppService(AgentHost agentHost, String host, Integer port,
			String service) {
		this.agentHost = agentHost;
		this.host = host;
		this.port = port;
		this.service = service;
		
		init();
	}
	
	private ArrayNode getConns(String agentId) throws IOException,
			JSONRPCException {
		State state = agentHost.getStateFactory().get(agentId);
		
		ArrayNode conns = null;
		if (state.containsKey(CONNKEY)) {
			conns = (ArrayNode) JOM.getInstance().readTree(state.get(CONNKEY,String.class));
		}
		return conns;
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
	public String getAgentUrl(String agentId) {
		try {
			ArrayNode conns = getConns(agentId);
			if (conns != null) {
				for (JsonNode conn : conns) {
					ObjectNode params = (ObjectNode) conn;
					
					String encryptedUsername = params.has("username") ? params
							.get("username").textValue() : null;
					String encryptedResource = params.has("resource") ? params
							.get("resource").textValue() : null;
					if (encryptedUsername != null) {
						String username = EncryptionUtil
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
		} catch (Exception e) {
			LOG.log(Level.WARNING, "", e);
		}
		return null;
	}
	
	/**
	 * Get the id of an agent from its url.
	 * If no agent with given id is connected via XMPP, null is returned.
	 * 
	 * @param agentUrl
	 * @return agentId
	 */
	@Override
	public String getAgentId(String agentUrl) {
		AgentConnection connection = connectionsByUrl.get(agentUrl);
		if (connection != null) {
			return connection.getAgentId();
		}
		return null;
	}
	
	/**
	 * initialize the transport service
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
	 * @param agentUrl
	 * @param username
	 * @param password
	 * @param resource
	 * @throws IOException
	 * @throws JSONRPCException
	 * @throws BadPaddingException
	 * @throws IllegalBlockSizeException
	 * @throws NoSuchPaddingException
	 * @throws InvalidKeySpecException
	 * @throws NoSuchAlgorithmException
	 * @throws InvalidAlgorithmParameterException
	 * @throws InvalidKeyException
	 */
	@Access(AccessType.UNAVAILABLE)
	public final void connect(String agentId, String username, String password)
			throws InvalidKeyException, InvalidAlgorithmParameterException,
			NoSuchAlgorithmException, InvalidKeySpecException,
			NoSuchPaddingException, IllegalBlockSizeException,
			BadPaddingException, JSONRPCException, IOException {
		String resource = null;
		connect(agentId, username, password, resource);
	}
	
	/**
	 * Connect to the configured messaging service (such as XMPP). The service
	 * must be configured in the Eve configuration
	 * 
	 * @param agentUrl
	 * @param username
	 * @param password
	 * @param resource
	 *            (optional)
	 * @throws IOException
	 * @throws JSONRPCException
	 * @throws BadPaddingException
	 * @throws IllegalBlockSizeException
	 * @throws NoSuchPaddingException
	 * @throws InvalidKeySpecException
	 * @throws NoSuchAlgorithmException
	 * @throws InvalidAlgorithmParameterException
	 * @throws JsonProcessingException
	 * @throws InvalidKeyException
	 * @throws Exception
	 */
	@Access(AccessType.UNAVAILABLE)
	public final void connect(String agentId, String username, String password,
			String resource) throws InvalidKeyException,
			InvalidAlgorithmParameterException, NoSuchAlgorithmException,
			InvalidKeySpecException, NoSuchPaddingException,
			IllegalBlockSizeException, BadPaddingException, JSONRPCException,
			IOException {
		String agentUrl = generateUrl(username, host, resource);
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
		storeConnection(agentId, username, password, resource);
	}
	
	private void storeConnection(String agentId, String username,
			String password, String resource) throws JSONRPCException,
			IOException, InvalidKeyException,
			InvalidAlgorithmParameterException, NoSuchAlgorithmException,
			InvalidKeySpecException, NoSuchPaddingException,
			IllegalBlockSizeException, BadPaddingException {
		
		State state = agentHost.getStateFactory().get(agentId);
		
		String conns = state.get(CONNKEY,String.class);
		ArrayNode newConns;
		if (conns != null) {
			newConns = (ArrayNode) JOM.getInstance().readTree(conns);
		} else {
			newConns = JOM.createArrayNode();
		}
		
		ObjectNode params = JOM.createObjectNode();
		params.put("username", EncryptionUtil.encrypt(username));
		params.put("password", EncryptionUtil.encrypt(password));
		if (resource != null && !resource.isEmpty()) {
			params.put("resource", EncryptionUtil.encrypt(resource));
		}
		for (JsonNode item : newConns) {
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
	
	private void delConnections(String agentId) throws JSONRPCException {
		State state = agentHost.getStateFactory().get(agentId);
		state.remove(CONNKEY);
	}
	
	/**
	 * Disconnect the agent from the connected messaging service(s) (if any)
	 * 
	 * @param agentId
	 */
	@Access(AccessType.UNAVAILABLE)
	public final void disconnect(String agentId) {
		
		try {
			ArrayNode conns = getConns(agentId);
			if (conns != null) {
				for (JsonNode conn : conns) {
					ObjectNode params = (ObjectNode) conn;
					
					String encryptedUsername = params.has("username") ? params
							.get("username").textValue() : null;
					String encryptedResource = params.has("resource") ? params
							.get("resource").textValue() : null;
					if (encryptedUsername != null) {
						String username = EncryptionUtil
								.decrypt(encryptedUsername);
						String resource = null;
						if (encryptedResource != null) {
							resource = EncryptionUtil
									.decrypt(encryptedResource);
						}
						
						String url = generateUrl(username, host, resource);
						AgentConnection connection = connectionsByUrl.get(url);
						if (connection != null) {
							connection.disconnect();
							connectionsByUrl.remove(url);
						}
					}
				}
			}
			delConnections(agentId);
		} catch (Exception e) {
			LOG.log(Level.WARNING, "", e);
		}
	}
	
	/**
	 * Send a message to an other agent
	 * 
	 * @param url
	 * @param request
	 * @param response
	 */
	@Override
	public JSONResponse send(String senderUrl, String receiver,
			JSONRequest request) throws JSONRPCException {
		SyncCallback<JSONResponse> callback = new SyncCallback<JSONResponse>();
		sendAsync(senderUrl, receiver, request, callback);
		try {
			return callback.get();
		} catch (Exception e) {
			throw new JSONRPCException("Couldn't handle XMPP return.", e);
		}
	}
	
	/**
	 * Asynchronously Send a message to an other agent
	 * 
	 * @param url
	 * @param request
	 * @param callback
	 *            with a JSONResponse
	 */
	@Override
	public void sendAsync(String senderUrl, String receiver,
			JSONRequest request, AsyncCallback<JSONResponse> callback)
			throws JSONRPCException {
		
		AgentConnection connection = null;
		
		if (senderUrl != null) {
			connection = connectionsByUrl.get(senderUrl);
		}
		if (connection != null) {
			// remove the protocol from the receiver url
			String protocol = "xmpp:";
			if (!receiver.startsWith(protocol)) {
				throw new JSONRPCException("Receiver url must start with '"
						+ protocol + "' (receiver='" + receiver + "')");
			}
			// username@domain
			String fullUsername = receiver.substring(protocol.length());
			connection.send(fullUsername, request, callback);
		} else {
			// TODO: use an anonymous xmpp connection when the sender agent has
			// no xmpp connection.
			throw new JSONRPCException("Cannot send an xmpp request, "
					+ "agent has no xmpp connection.");
		}
	}
	
	/**
	 * Get the url of an xmpp connection "xmpp:username@host"
	 * 
	 * @param username
	 * @param host
	 * @param resource
	 *            optional
	 * @return url
	 */
	private static String generateUrl(String username, String host,
			String resource) {
		String url = "xmpp:" + username + "@" + host;
		if (resource != null && !resource.isEmpty()) {
			url += "/" + resource;
		}
		return url;
	}
	
	@Override
	public String toString() {
		Map<String, Object> data = new HashMap<String, Object>();
		data.put("class", this.getClass().getName());
		data.put("host", host);
		data.put("port", port);
		data.put("service", service);
		data.put("protocols", protocols);
		
		return data.toString();
	}
	
	@Override
	public void reconnect(String agentId) throws JSONRPCException, IOException {
		ArrayNode conns = getConns(agentId);
		if (conns != null) {
			for (JsonNode conn : conns) {
				ObjectNode params = (ObjectNode) conn;
				LOG.info("Initializing connection:" + agentId + " --> "
						+ params);
				try {
					String encryptedUsername = params.has("username") ? params
							.get("username").textValue() : null;
					String encryptedPassword = params.has("password") ? params
							.get("password").textValue() : null;
					String encryptedResource = params.has("resource") ? params
							.get("resource").textValue() : null;
					if (encryptedUsername != null && encryptedPassword != null) {
						String username = EncryptionUtil
								.decrypt(encryptedUsername);
						String password = EncryptionUtil
								.decrypt(encryptedPassword);
						String resource = null;
						if (encryptedResource != null) {
							resource = EncryptionUtil
									.decrypt(encryptedResource);
						}
						connect(agentId, username, password, resource);
					}
				} catch (Exception e) {
					throw new JSONRPCException("Failed to connect XMPP.", e);
				}
			}
		}
	}
	
	@Override
	public String getKey() {
		return "xmpp://" + host + ":" + port + "/" + service;
	}
}
