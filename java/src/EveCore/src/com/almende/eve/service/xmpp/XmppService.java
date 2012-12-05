package com.almende.eve.service.xmpp;

import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.almende.eve.agent.AgentFactory;
import com.almende.eve.agent.annotation.Access;
import com.almende.eve.agent.annotation.AccessType;
import com.almende.eve.json.JSONRequest;
import com.almende.eve.json.JSONResponse;
import com.almende.eve.service.AsyncCallback;
import com.almende.eve.service.Service;

public class XmppService extends Service {	
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
		XmppAgentConnection connection = connectionsById.get(agentId);
		if (connection != null) {
			String username = connection.getUsername();
			return generateUrl(username, host);
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
		XmppAgentConnection connection = connectionsByUrl.get(agentUrl);
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
     */
	@Override
	public void init(Map<String, Object> params) {
		if (params != null) {
			host = (String) params.get("host");
			port = (Integer) params.get("port");
			serviceName = (String) params.get("serviceName");
		}
	}

	/**
	 * initialize the settings for the xmpp service
	 * @param host
	 * @param port
	 * @param serviceName
     */
	public void init(String host, Integer port, String serviceName) {
		this.host = host;
		this.port = port;
		this.serviceName = serviceName;
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
	 * @throws NoSuchMethodException 
	 * @throws InvocationTargetException 
	 * @throws IllegalAccessException 
	 * @throws InstantiationException 
	 * @throws SecurityException 
	 * @throws IllegalArgumentException 
	 */
	@Access(AccessType.UNAVAILABLE)
	final public void connect (String agentId, String username, String password) 
			throws Exception {
		String url = generateUrl(username, host);
		if (connectionsByUrl.containsKey(url)) {
			throw new Exception("Connection for url '" + url + "' is already open.");
		}

		// instantiate open the connection		
		XmppAgentConnection connection = new XmppAgentConnection(agentFactory);
		connection.connect(agentId, host, port, serviceName, username, password);
		
		// register the connection
		connectionsById.put(agentId, connection);
		connectionsByUrl.put(generateUrl(username, host), connection);
	}
	
	/**
	 * Disconnect the agent from the connected messaging service (if any)
	 */
	@Access(AccessType.UNAVAILABLE)
	final public void disconnect (String agentId) {
		XmppAgentConnection connection = connectionsById.get(agentId);
		if (connection != null) {
			connection.disconnect();

			String url = generateUrl(connection.getUsername(), host);
			connectionsById.remove(agentId);
			connectionsByUrl.remove(url);
		}
	}

	@Override
	public JSONResponse send(String sender, String receiver, JSONRequest request)
			throws Exception {
		// TODO: implement synchronous xmpp request
		throw new Exception("Synchronous xmpp requests are not yet supported");
	}

	@Override
	public void sendAsync(String senderId, String receiver, JSONRequest request,
			AsyncCallback<JSONResponse> callback) throws Exception {
		XmppAgentConnection connection = connectionsById.get(senderId);
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
			throw new Exception("Cannot send an xmpp request, " +
					"agent is has no xmpp connection.");
		}
	}

	/**
	 * Get the url of an xmpp connection "xmpp:username@host" 
	 * @return url
	 */
	private static String generateUrl(String username, String host) {
		return "xmpp:" + username + "@" + host; 
	}

	private String host = null;
	private Integer port = null;
	private String serviceName = null;

	private Map<String, XmppAgentConnection> connectionsById = 
			new HashMap<String, XmppAgentConnection>();   // agentId as key
	private Map<String, XmppAgentConnection> connectionsByUrl = 
			new HashMap<String, XmppAgentConnection>();   // xmpp url as key "xmpp:username@host"
	List<String> protocols = Arrays.asList("xmpp");
}
