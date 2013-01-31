package com.almende.eve.transport.xmpp;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

import com.almende.eve.agent.AgentFactory;
import com.almende.eve.agent.annotation.Access;
import com.almende.eve.agent.annotation.AccessType;
import com.almende.eve.context.Context;
import com.almende.eve.rpc.jsonrpc.JSONRequest;
import com.almende.eve.rpc.jsonrpc.JSONResponse;
import com.almende.eve.transport.AsyncCallback;
import com.almende.eve.transport.TransportService;
import com.almende.eve.transport.SyncCallback;
import com.almende.util.EncryptionUtil;

public class XmppService extends TransportService {	
	public XmppService(AgentFactory agentFactory) {
		super(agentFactory);
	}

	/**
	 * Get the url of an agent from its id.
	 * If no agent with given id is connected via XMPP, null is returned.
	 * @param agentId      The id of the agent
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
	 * initialize the settings for the xmpp service
	 * @param params   Available parameters:
	 *                 {String} host
	 *                 {Integer} port
	 *                 {String} serviceName
	 *                 {String} id
     */
	@Override
	// TODO: make init private and call from the constructor.
	public void init(Map<String, Object> params) {
		String contextId = null;

		if (params != null) {
			host = (String) params.get("host");
			port = (Integer) params.get("port");
			service = (String) params.get("service");
			contextId = (String) params.get("id");
		}
		
		initContext(contextId);
		initConnections();
	}

	/**
	 * initialize the settings for the xmpp service
	 * @param host
	 * @param port
	 * @param service  service name
     */
	// TODO: make init private and call from the constructor.
	public void init(String host, Integer port, String service) {
		String id = null;
		init(host, port, service, id);
	}

	/**
	 * initialize the settings for the xmpp service
	 * @param host
	 * @param port
	 * @param service  service name
	 * @param id       context id, to persist the state
     */
	// TODO: make init private and call from the constructor.
	public void init(String host, Integer port, String service, String id) {
		this.host = host;
		this.port = port;
		this.service = service;
		
		initContext(id);
		initConnections();
	}
	
	/**
	 * Initialize a context for the service, to persist the parameters of all
	 * open connections.
	 * @param contextId
	 */
	private void initContext (String contextId) {
		// set a context for the service, where the service can 
		// persist its state.
		if (contextId == null) {
			contextId = ".xmppservice";
			logger.info("No id specified for XmppService. Using " + contextId + " as id.");
		}
		try {
			// TODO: dangerous to use a generic context (can possibly conflict with the id a regular agent)
			context = agentFactory.getContextFactory().get(contextId);
			if (context == null) {
				context = agentFactory.getContextFactory().create(contextId);
			}
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	/**
	 * Get the protocols supported by the XMPPService.
	 * Will return an array with one value, "xmpp"
	 * @return protocols
	 */
	@Override
	public List<String> getProtocols() {
		return protocols;
	}

	/**
	 * Connect to the configured messaging service (such as XMPP). The service
	 * must be configured in the Eve configuration
	 * @param agentUrl
	 * @param username
	 * @param password
	 * @param resource
	 * @throws Exception 
	 */
	@Access(AccessType.UNAVAILABLE)
	final public void connect (String agentId, String username, String password) 
			throws Exception {
		String resource = null;
		connect(agentId, username, password, resource);
	}
	
	/**
	 * Connect to the configured messaging service (such as XMPP). The service
	 * must be configured in the Eve configuration
	 * @param agentUrl
	 * @param username
	 * @param password
	 * @param resource  (optional)
	 * @throws Exception 
	 */
	@Access(AccessType.UNAVAILABLE)
	final public void connect (String agentId, String username, String password, 
			String resource) throws Exception {
		String agentUrl = generateUrl(username, host, resource);
		if (connectionsByUrl.containsKey(agentUrl)) {
			throw new Exception("Connection for url '" + agentUrl + "' is already open. " +
					"Close this connection first.");
		}
		if (connectionsById.containsKey(agentId)) {
			AgentConnection c = connectionsById.get(agentId);
			String url = generateUrl(c.getUsername(), host, c.getResource());
			throw new Exception("Agent with id '" + agentId + "' already has " +
					"an open xmpp connection (" + url + "). " +
					"Close this connection first.");
		}

		// instantiate open the connection		
		AgentConnection connection = new AgentConnection(agentFactory);
		connection.connect(agentId, host, port, service, username, password, resource);
		
		// register the connection
		connectionsById.put(agentId, connection);
		connectionsByUrl.put(agentUrl, connection);
		
		// persist the parameters for the connection
		if (context != null) {
			synchronized (context) {
				@SuppressWarnings("unchecked")
				Map<String, Map<String, String>> connections = 
						(Map<String, Map<String, String>>)context.get("connections");
				if (connections == null) {
					connections = new HashMap<String, Map<String, String>>();
				}
				Map<String, String> params = new HashMap<String, String>();
				params.put("username", EncryptionUtil.encrypt(username));
				params.put("password", EncryptionUtil.encrypt(password));
				if (resource != null && !resource.isEmpty()) {
					params.put("resource", EncryptionUtil.encrypt(resource));
				}
				connections.put(agentId, params);
				context.put("connections", connections);
			}
		}
	}
	
	/**
	 * Disconnect the agent from the connected messaging service (if any)
	 * @param agentId
	 */
	@Access(AccessType.UNAVAILABLE)
	final public void disconnect (String agentId) {
		AgentConnection connection = connectionsById.get(agentId);
		if (connection != null) {
			connection.disconnect();

			String agentUrl = generateUrl(connection.getUsername(), host, 
					connection.getResource());
			connectionsById.remove(agentId);
			connectionsByUrl.remove(agentUrl);

			// remove the connection parameters from the persisted state
			if (context != null) {
				synchronized (context) {
					@SuppressWarnings("unchecked")
					Map<String, Map<String, String>> connections = 
							(Map<String, Map<String, String>>)context.get("connections");
					if (connections != null && connections.containsKey(agentId)) {
						connections.remove(agentId);
						context.put("connections", connections);
					}
				}
			}
		}
	}
	
	/**
	 * Send a message to an other agent
	 * @param url
	 * @param request
	 * @param response
	 */
	@Override
	public JSONResponse send(String senderId, String receiver, 
			JSONRequest request) throws Exception {
		SyncCallback<JSONResponse> callback = new SyncCallback<JSONResponse>();
		sendAsync(senderId, receiver, request, callback);
		return callback.get();
	}

	/**
	 * Asynchronously Send a message to an other agent
	 * @param url
	 * @param request
	 * @param callback with a JSONResponse
	 */
	@Override
	public void sendAsync(String senderId, String receiver, JSONRequest request,
			AsyncCallback<JSONResponse> callback) throws Exception {
		AgentConnection connection = connectionsById.get(senderId);
		if (connection != null) {
			// remove the protocol from the receiver url
			String protocol = "xmpp:";
			if (!receiver.startsWith(protocol)) {
				throw new Exception("Receiver url must start with '" + protocol +
						"' (receiver='" + receiver + "')");
			}
			String fullUsername = receiver.substring(protocol.length()); // username@domain
			connection.send(fullUsername, request, callback);
		}
		else {
			// TODO: use an anonymous xmpp connection when the sender agent has no xmpp connection.
			throw new Exception("Cannot send an xmpp request, " +
					"agent is has no xmpp connection.");
		}
	}

	/**
	 * Get the url of an xmpp connection "xmpp:username@host" 
	 * @param username
	 * @param host
	 * @param resource     optional
	 * @return url
	 */
	private static String generateUrl(String username, String host, String resource) {
		String url = "xmpp:" + username + "@" + host;
		if (resource != null && !resource.isEmpty()) {
			url += "/" + resource;
		}
		return url;
	}

	/**
	 * initialize all connections stored in the services context
	 */
	private void initConnections() {
		if (context != null) {
			synchronized (context) {
				@SuppressWarnings("unchecked")
				Map<String, Map<String, String>> connections = 
						(Map<String, Map<String, String>>)context.get("connections");
				if (connections != null) {
					logger.info("Initializing " + connections.size() + " XMPP connections...");
					
					for (String agentId : connections.keySet()) {
						Map<String, String> params = connections.get(agentId);
						try {
							String encryptedUsername = params.get("username");
							String encryptedPassword = params.get("password");
							String encryptedResource = params.get("resource");
							if (encryptedUsername != null && encryptedPassword != null) {
								String username = EncryptionUtil.decrypt(encryptedUsername);
								String password = EncryptionUtil.decrypt(encryptedPassword);
								String resource = null;
								if (encryptedResource != null) {
									resource = EncryptionUtil.decrypt(encryptedResource);
								}
								connect(agentId, username, password, resource);
							}
						} catch (Exception e) {
							e.printStackTrace();
						}
					}
					
					logger.info("XMPP connections initialized");
				}
			}
		}
	}

	@Override
	public String toString() {
		Map<String, Object> data = new HashMap<String, Object>();
		data.put("class", this.getClass().getName());
		data.put("host", host);
		data.put("port", port);
		data.put("service", service);
		data.put("protocols", protocols);

		if (context != null) {
			data.put("id", context.getAgentId());
		}
		
		return data.toString();
	}	
	
	private String host = null;
	private Integer port = null;
	private String service = null;
	private Context context = null;	
	
	private Map<String, AgentConnection> connectionsById = 
			new ConcurrentHashMap<String, AgentConnection>();   // agentId as key
	private Map<String, AgentConnection> connectionsByUrl = 
			new ConcurrentHashMap<String, AgentConnection>();   // xmpp url as key "xmpp:username@host"
	List<String> protocols = Arrays.asList("xmpp");

	private Logger logger = Logger.getLogger(this.getClass().getSimpleName());
}
