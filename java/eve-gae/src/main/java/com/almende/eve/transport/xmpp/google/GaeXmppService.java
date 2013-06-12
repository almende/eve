package com.almende.eve.transport.xmpp.google;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import com.almende.eve.agent.AgentHost;
import com.almende.eve.rpc.jsonrpc.JSONRPCException;
import com.almende.eve.rpc.jsonrpc.JSONRequest;
import com.almende.eve.rpc.jsonrpc.JSONResponse;
import com.almende.eve.transport.AsyncCallback;
import com.almende.eve.transport.TransportService;
import com.google.apphosting.api.ApiProxy;
import com.google.apphosting.api.ApiProxy.Environment;

/**
 * Google App Engine XMPP Transport Service
 */
public class GaeXmppService implements TransportService {
	private static List<String> protocols = Arrays.asList("xmpp");
	private String host = null;

	/**
	 * This constructor is called when constructed by the AgentFactory
	 * @param agentFactory
	 * @param params
	 */
	public GaeXmppService(AgentHost agentFactory, Map<String, Object> params) {
		this();
	}

	public GaeXmppService() {
		// built up the host url
		Environment env = ApiProxy.getCurrentEnvironment();
		if (env != null) {
			String appId = env.getAppId();
			if (appId != null) {
				if (appId.startsWith("s~")) {
					// ids of apps with high replication have a "s~" prefix
					appId = appId.substring(2);
				}
				host = appId + ".appspotchat.com";
			}
		}
	}

	/**
	 * Build agentUrl from agentId. 
	 * agentUrl has the format "xmpp:agentid@myapp.appspot.com"
	 * @param agentId
	 * @return agentUrl  Returns the xmpp address, or null when agentId is null
	 *                   or no host is configured
	 */
	@Override
	public String getAgentUrl(String agentId) {
		if (agentId != null && host != null) {
			return "xmpp:" + agentId + "@" + host;
		}
		return null;
	}

	/**
	 * Extract agentId from agentUrl.
	 * agentUrl has the format "xmpp:agentid@myapp.appspotchat.com/resource"
	 * @param agentUrl
	 * @return agentId   The extracted agentId, or null when agentUrl does
	 *                   not match the configured host
	 */
	@Override
	public String getAgentId(String agentUrl) {
		if (agentUrl == null || host == null) {
			return null;
		}
		
		String prefix = "xmpp:";
		if (agentUrl.startsWith(prefix)) {
			// prefix matches
			int at = agentUrl.indexOf('@');
			if (at != -1) {
				int hostStart = at + 1;
				int slash = agentUrl.indexOf('/', at);
				if ((slash != -1 && host.equals(agentUrl.substring(hostStart, slash))) ||
						(slash == -1 && agentUrl.length() == hostStart + host.length())) {
					// host matches. extract agentId
					String agentId = agentUrl.substring(prefix.length(), at);
					return agentId;
				}
			}
		}

		return null;
	}
	
	@Override
	public JSONResponse send(String senderId, String receiver,
			JSONRequest request) throws JSONRPCException {
		throw new JSONRPCException("JSONResponse send(String senderId, String receiver, " +
				"JSONRequest request) not supported by GaeXmppService. " +
				"Use sendAsync(String senderId, String receiver, " +
				"JSONRequest request, String callback) instead.");
	}

	@Override
	public void sendAsync(String senderId, String receiver,
			JSONRequest request, AsyncCallback<JSONResponse> callback)
			throws UnsupportedOperationException {
		throw new UnsupportedOperationException("JSONResponse sendAsync(String senderId, " +
				"String receiver, JSONRequest request, " +
				"AsyncCallback<JSONResponse> callback) not supported by GaeXmppService. " +
				"Use sendAsync(String senderId, String receiver, " +
				"JSONRequest request, String callback) instead.");
	}

	@Override
	public List<String> getProtocols() {
		return protocols;
	}

	@Override
	public void reconnect(String agentId) {
		//Nothing todo here
	}

	@Override
	public String getKey() {
		return host;
	}
}
