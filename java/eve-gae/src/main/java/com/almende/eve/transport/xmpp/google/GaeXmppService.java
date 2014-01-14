package com.almende.eve.transport.xmpp.google;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import com.almende.eve.agent.AgentHostDefImpl;
import com.almende.eve.transport.TransportService;
import com.google.apphosting.api.ApiProxy;
import com.google.apphosting.api.ApiProxy.Environment;

/**
 * Google App Engine XMPP Transport Service
 */
public class GaeXmppService implements TransportService {
	private static final Logger LOG = Logger.getLogger(GaeXmppService.class.getName());
	private static List<String> protocols = Arrays.asList("xmpp");
	private String host = null;

	/**
	 * This constructor is called when constructed by the AgentFactory
	 * @param agentFactory
	 * @param params
	 */
	public GaeXmppService(AgentHostDefImpl agentFactory, Map<String, Object> params) {
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
	public URI getAgentUrl(String agentId) {
		if (agentId != null && host != null) {
			try {
				return new URI("xmpp:" + agentId + "@" + host);
			} catch (URISyntaxException e) {
				LOG.warning("Couldn't form agentURL!");
				return null;
			}
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
	public String getAgentId(final URI agentUri) {
		if (agentUri == null || host == null) {
			return null;
		}
		if (agentUri.getScheme().equals("xmpp")) {
			//TODO: simplify by using the URI structure.
			String prefix = "xmpp:";
			String agentUrl = agentUri.toString();
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
	
	@Override
	public String toString() {
		Map<String, Object> data = new HashMap<String, Object>();
		data.put("class", this.getClass().getName());
		data.put("host", host);
		data.put("protocols", protocols);
		return data.toString();
	}

	@Override
	public void sendAsync(URI senderUri, URI receiverUri, String message,
			String tag) throws IOException {
		throw new IOException("Missing implemenation!");
		//TODO / FIXME: WHy is this missing????
	}


}
