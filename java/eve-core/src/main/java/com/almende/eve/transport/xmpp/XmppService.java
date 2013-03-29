package com.almende.eve.transport.xmpp;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

import org.jivesoftware.smack.SmackConfiguration;

import com.almende.eve.agent.AgentFactory;
import com.almende.eve.agent.annotation.Access;
import com.almende.eve.agent.annotation.AccessType;
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
	private AgentFactory					agentFactory		= null;
	private String							host				= null;
	private Integer							port				= null;
	private String							service				= null;
	
	private Map<String, AgentConnection>	connectionsById		= new ConcurrentHashMap<String, AgentConnection>(); // agentId
																													// as
																													// key
	private Map<String, AgentConnection>	connectionsByUrl	= new ConcurrentHashMap<String, AgentConnection>(); // xmpp
																													// url
																													// as
																													// key
																													// "xmpp:username@host"
	private static List<String>				protocols			= Arrays.asList("xmpp");
	
	private Logger							logger				= Logger.getLogger(this
																		.getClass()
																		.getSimpleName());
	
	protected XmppService() {
	}
	
	/**
	 * Construct an XmppService
	 * This constructor is called when the TransportService is constructed
	 * by the AgentFactory
	 * 
	 * @param params
	 *            Available parameters:
	 *            {String} host
	 *            {Integer} port
	 *            {String} serviceName
	 *            {String} id
	 */
	public XmppService(AgentFactory agentFactory, Map<String, Object> params) {
		this.agentFactory = agentFactory;
		
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
	public XmppService(AgentFactory agentFactory, String host, Integer port,
			String service) {
		this.agentFactory = agentFactory;
		this.host = host;
		this.port = port;
		this.service = service;
		
		init();
	}
	
	
	/**
	 * Get the url of an agent from its id.
	 * If no agent with given id is connected via XMPP, null is returned.
	 * 
	 * @param agentId
	 *            The id of the agent
	 * @return agentUrl
	 */
	@Override
	public String getAgentUrl(String agentId) {
		AgentConnection connection = connectionsById.get(agentId);
		if (connection != null) {
			return generateUrl(connection.getUsername(), host,
					connection.getResource());
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
	 * @throws Exception
	 */
	@Access(AccessType.UNAVAILABLE)
	final public void connect(String agentId, String username, String password)
			throws Exception {
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
	 * @throws Exception
	 */
	@Access(AccessType.UNAVAILABLE)
	final public void connect(String agentId, String username, String password,
			String resource) throws Exception {
		String agentUrl = generateUrl(username, host, resource);
		if (connectionsByUrl.containsKey(agentUrl)) {
			throw new Exception("Connection for url '" + agentUrl
					+ "' is already open. " + "Close this connection first.");
		}
		if (connectionsById.containsKey(agentId)) {
			AgentConnection c = connectionsById.get(agentId);
			String url = generateUrl(c.getUsername(), host, c.getResource());
			throw new Exception("Agent with id '" + agentId + "' already has "
					+ "an open xmpp connection (" + url + "). "
					+ "Close this connection first.");
		}
		
		// instantiate open the connection
		AgentConnection connection = new AgentConnection(agentFactory);
		connection.connect(agentId, host, port, service, username, password,
				resource);
		
		// register the connection
		connectionsById.put(agentId, connection);
		connectionsByUrl.put(agentUrl, connection);
		storeConnection(agentId, username, password, resource);
	}
	
	private void storeConnection(String agentId, String username,
			String password, String resource) throws Exception {
		
		State state = agentFactory.getStateFactory().get(agentId);

		String conns = (String) state.get("_XMPP_Connections");
		ArrayNode newConns;
		if (conns != null){
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
		for (JsonNode item: newConns){
			if (item.get("username").equals(params.get("username"))){
				return; 
			}
		}
		newConns.add(params);
		if (!state.putIfUnchanged("_XMPP_Connections", JOM.getInstance().writeValueAsString(newConns), conns)) {
			// recursive retry
			storeConnection(agentId, username, password, resource);
		}
	}
	
	private void delConnections(String agentId) throws Exception {
		State state = agentFactory.getStateFactory().get(agentId);
		state.remove("_XMPP_Connections");
	}
	
	/**
	 * Disconnect the agent from the connected messaging service(s) (if any)
	 * 
	 * @param agentId
	 */
	@Access(AccessType.UNAVAILABLE)
	final public void disconnect(String agentId) {
		AgentConnection connection = connectionsById.get(agentId);
		if (connection != null) {
			connection.disconnect();
			
			String agentUrl = generateUrl(connection.getUsername(), host,
					connection.getResource());
			connectionsById.remove(agentId);
			connectionsByUrl.remove(agentUrl);
			
			// remove the connection parameters from the persisted state
			try {
				delConnections(agentId);
			} catch (Exception e) {
				e.printStackTrace();
			}
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
			JSONRequest request) throws Exception {
		SyncCallback<JSONResponse> callback = new SyncCallback<JSONResponse>();
		sendAsync(senderUrl, receiver, request, callback);
		return callback.get();
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
			throws Exception {
		
		AgentConnection connection = connectionsByUrl.get(senderUrl);
		
		if (connection != null) {
			// remove the protocol from the receiver url
			String protocol = "xmpp:";
			if (!receiver.startsWith(protocol)) {
				throw new Exception("Receiver url must start with '" + protocol
						+ "' (receiver='" + receiver + "')");
			}
			String fullUsername = receiver.substring(protocol.length()); // username@domain
			connection.send(fullUsername, request, callback);
		} else {
			// TODO: use an anonymous xmpp connection when the sender agent has
			// no xmpp connection.
			throw new Exception("Cannot send an xmpp request, "
					+ "agent is has no xmpp connection.");
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
	public void reconnect(String agentId) throws Exception {
		State state = agentFactory.getStateFactory().get(agentId);
		ArrayNode conns = (ArrayNode) JOM.getInstance().readTree((String)state.get("_XMPP_Connections"));

		if (conns != null) {
			for (JsonNode conn : conns) {
				ObjectNode params = (ObjectNode) conn;
				logger.info("Initializing connection:"+agentId+" --> "+params);
				try {
					String encryptedUsername = params.has("username")?params.get("username").textValue():null;
					String encryptedPassword = params.has("password")?params.get("password").textValue():null;
					String encryptedResource = params.has("resource")?params.get("resource").textValue():null;
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
					e.printStackTrace();
				}
			}
		}
	}
}
