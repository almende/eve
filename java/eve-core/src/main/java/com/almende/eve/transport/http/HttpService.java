package com.almende.eve.transport.http;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.util.EntityUtils;

import com.almende.eve.agent.AgentHost;
import com.almende.eve.agent.callback.AsyncCallback;
import com.almende.eve.agent.callback.CallbackInterface;
import com.almende.eve.transport.TransportService;
import com.almende.util.tokens.TokenStore;

public class HttpService implements TransportService {
	private static final Logger	LOG			= Logger.getLogger(HttpService.class
													.getCanonicalName());
	private String				servletUrl	= null;
	private AgentHost			host		= null;
	private final List<String>		protocols	= Arrays.asList("http", "https",
													"web");
	
	public HttpService(final AgentHost agentHost) {
		host = agentHost;
	}
	
	/**
	 * Construct an HttpService This constructor is called when the
	 * TransportService is constructed by the AgentHost
	 * 
	 * @param agentHost
	 * @param params
	 *            Available parameters: {String} servlet_url
	 */
	public HttpService(final AgentHost agentHost, final Map<String, Object> params) {
		host = agentHost;
		if (params != null) {
			setServletUrl((String) params.get("servlet_url"));
		}
	}
	
	/**
	 * Construct an HttpService This constructor is called when the
	 * TransportService is constructed by the AgentHost
	 * 
	 * @param agentHost
	 * @param params
	 *            Available parameters: {String} servlet_url
	 */
	public HttpService(final AgentHost agentHost, final String servletUrl) {
		host = agentHost;
		setServletUrl(servletUrl);
	}
	
	/**
	 * Set the servlet url for the transport service. This determines the
	 * mapping between an agentId and agentUrl.
	 * 
	 * @param servletUrl
	 */
	private void setServletUrl(final String servletUrl) {
		this.servletUrl = servletUrl;
		if (!this.servletUrl.endsWith("/")) {
			this.servletUrl += "/";
		}
		final int separator = this.servletUrl.indexOf(':');
		if (separator != -1) {
			final String protocol = this.servletUrl.substring(0, separator);
			if (!protocols.contains(protocol)) {
				protocols.add(protocol);
			}
		}
	}
	
	/**
	 * Return the configured servlet url corresponding to this transport
	 * service. The servlet url is loaded from the parameter servlet_url in the
	 * configuration.
	 * 
	 * @return servletUrl
	 */
	public String getServletUrl() {
		return servletUrl;
	}
	
	/**
	 * Retrieve the protocols supported by the transport service. This can be
	 * "http" or "https", depending on the configuration.
	 * 
	 * @return protocols
	 */
	@Override
	public List<String> getProtocols() {
		return protocols;
	}
	
	/**
	 * Send a JSON-RPC request to an agent via HTTP
	 * 
	 * @param senderUrl
	 * @param receiverUrl
	 * @param request
	 * @return response
	 * @throws Exception
	 */
	@Override
	public void sendAsync(final String senderUrl, final String receiverUrl,
			final Object message, final String tag) throws IOException {
		
		host.getPool().execute(new Runnable() {
			
			@Override
			public void run() {
				HttpPost httpPost = null;
				try {
					
					if (tag != null) {
						// This is a reply to a synchronous inbound call, get
						// callback
						// and use it to send the message
						final CallbackInterface<Object> callbacks = host
								.getCallbackService("HttpTransport",
										Object.class);
						if (callbacks != null) {
							final AsyncCallback<Object> callback = callbacks.get(tag);
							if (callback != null) {
								callback.onSuccess(message);
								return;
							} else {
								LOG.warning("Tag set, but no callback found! "
										+ callback);
							}
						} else {
							LOG.warning("Tag set, but no callbacks found!");
						}
					}
					httpPost = new HttpPost(receiverUrl);
					// invoke via Apache HttpClient request:
					httpPost.setEntity(new StringEntity(message.toString()));
					
					// Add token for HTTP handshake
					httpPost.addHeader("X-Eve-Token", TokenStore.create()
							.toString());
					httpPost.addHeader("X-Eve-SenderUrl", senderUrl);
					
					final HttpResponse webResp = ApacheHttpClient.get().execute(
							httpPost);
					final String result = EntityUtils.toString(webResp.getEntity());
					host.receive(senderUrl, result, receiverUrl, null);
				} catch (final Exception e) {
					LOG.log(Level.WARNING,
							"HTTP roundtrip resulted in exception!", e);
				} finally {
					if (httpPost != null) {
						httpPost.reset();
					}
				}
			}
		});
	}
	
	/**
	 * Get the url of an agent from its id.
	 * 
	 * @param agentId
	 * @return agentUrl
	 */
	@Override
	public String getAgentUrl(final String agentId) {
		if (servletUrl != null) {
			try {
				return servletUrl + URLEncoder.encode(agentId, "UTF-8") + "/";
			} catch (final UnsupportedEncodingException e) {
				return servletUrl + agentId + "/";
			}
		} else {
			return null;
		}
	}
	
	/**
	 * Get the id of an agent from its url. If the id cannot be extracted, null
	 * is returned. A typical url is "http://myserver.com/agents/agentid/"
	 * 
	 * @param agentUrl
	 * @return agentId
	 */
	@Override
	public String getAgentId(String agentUrl) {
		if (servletUrl != null) {
			// add domain when missing
			final String domain = getDomain(agentUrl);
			if (domain.equals("")) {
				// provided url is only containing the path (not the domain)
				agentUrl = getDomain(servletUrl) + agentUrl;
			}
			
			if (agentUrl.startsWith(servletUrl)) {
				final int separator = agentUrl.indexOf('/', servletUrl.length());
				try {
					if (separator != -1) {
						return URLDecoder.decode(agentUrl.substring(
								servletUrl.length(), separator), "UTF-8");
					} else {
						return URLDecoder.decode(
								agentUrl.substring(servletUrl.length()),
								"UTF-8");
					}
				} catch (final UnsupportedEncodingException e) {
					LOG.log(Level.WARNING, "", e);
				}
			}
		}
		
		return null;
	}
	
	/**
	 * Get the resource from the end of an agentUrl, for example
	 * "http://myserver.com/agents/agentid/index.html" will return "index.html"
	 * The method will return null when the provided url does not match the
	 * configured url
	 * 
	 * @param agentUrl
	 * @return
	 */
	public String getAgentResource(String agentUrl) {
		if (servletUrl != null) {
			// add domain when missing
			final String domain = getDomain(agentUrl);
			if (domain.equals("")) {
				// provided url is only containing the path (not the domain)
				agentUrl = getDomain(servletUrl) + agentUrl;
			}
			
			if (agentUrl.startsWith(servletUrl)) {
				final int separator = agentUrl.indexOf('/', servletUrl.length());
				if (separator != -1) {
					return agentUrl.substring(separator + 1);
				} else {
					return "";
				}
			}
		}
		
		return null;
	}
	
	/**
	 * Get the domain part of given url. For example
	 * "http://localhost:8080/EveCore/agents/testagent/1/" will return
	 * "http://localhost:8080", and "/EveCore/agents/testagent/1/" will return
	 * "".
	 * 
	 * @param url
	 * @return domain
	 */
	public String getDomain(final String url) {
		final int protocolSeparator = url.indexOf("://");
		if (protocolSeparator != -1) {
			final int fromIndex = (protocolSeparator != -1) ? protocolSeparator + 3
					: 0;
			final int pathSeparator = url.indexOf('/', fromIndex);
			if (pathSeparator != -1) {
				return url.substring(0, pathSeparator);
			}
		}
		return "";
	}
	
	@Override
	public String toString() {
		final Map<String, Object> data = new HashMap<String, Object>();
		data.put("class", this.getClass().getName());
		data.put("servlet_url", servletUrl);
		data.put("protocols", protocols);
		return data.toString();
	}
	
	@Override
	public void reconnect(final String agentId) {
		// Nothing todo at this point
	}
	
	@Override
	public String getKey() {
		return "http://"
				+ (getServletUrl() == null ? "outbound" : getServletUrl());
	}
	
}
