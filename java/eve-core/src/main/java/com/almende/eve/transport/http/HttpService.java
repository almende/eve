/*
 * Copyright: Almende B.V. (2014), Rotterdam, The Netherlands
 * License: The Apache Software License, Version 2.0
 */
package com.almende.eve.transport.http;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
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
import com.almende.eve.agent.callback.AsyncCallbackQueue;
import com.almende.eve.transport.TransportService;
import com.almende.util.tokens.TokenStore;

/**
 * The Class HttpService.
 */
public class HttpService implements TransportService {
	private static final Logger	LOG			= Logger.getLogger(HttpService.class
													.getCanonicalName());
	private String				servletUrl	= null;
	private AgentHost			host		= null;
	private final List<String>	protocols	= Arrays.asList("http", "https",
													"web");
	
	/**
	 * Instantiates a new http service.
	 * 
	 * @param agentHost
	 *            the agent host
	 */
	public HttpService(final AgentHost agentHost) {
		host = agentHost;
	}
	
	/**
	 * Construct an HttpService This constructor is called when the
	 * TransportService is constructed by the AgentHost.
	 * 
	 * @param agentHost
	 *            the agent host
	 * @param params
	 *            Available parameters: {String} servlet_url
	 */
	public HttpService(final AgentHost agentHost,
			final Map<String, Object> params) {
		host = agentHost;
		if (params != null) {
			setServletUrl((String) params.get("servlet_url"));
		}
	}
	
	/**
	 * Construct an HttpService This constructor is called when the
	 * TransportService is constructed by the AgentHost.
	 * 
	 * @param agentHost
	 *            the agent host
	 * @param servletUrl
	 *            the servlet url
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
	 *            the new servlet url
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
	 * Send a JSON-RPC request to an agent via HTTP.
	 * 
	 * @param senderUrl
	 *            the sender url
	 * @param receiverUrl
	 *            the receiver url
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
		
		host.getPool().execute(new Runnable() {
			
			@Override
			public void run() {
				HttpPost httpPost = null;
				try {
					
					if (tag != null) {
						// This is a reply to a synchronous inbound call, get
						// callback
						// and use it to send the message
						final AsyncCallbackQueue<String> callbacks = host
								.getCallbackQueue("HttpTransport",
										String.class);
						if (callbacks != null) {
							final AsyncCallback<String> callback = callbacks
									.pull(tag);
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
					httpPost.setEntity(new StringEntity(message));
					
					// Add token for HTTP handshake
					httpPost.addHeader("X-Eve-Token", TokenStore.create()
							.toString());
					httpPost.addHeader("X-Eve-SenderUrl", senderUrl.toString());
					
					final HttpResponse webResp = ApacheHttpClient.get()
							.execute(httpPost);
					final String result = EntityUtils.toString(webResp
							.getEntity());
					host.receive(getAgentId(senderUrl), result, receiverUrl,
							null);
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
	 *            the agent id
	 * @return agentUrl
	 */
	@Override
	public URI getAgentUrl(final String agentId) {
		if (servletUrl != null) {
			try {
				try {
					return new URI(servletUrl
							+ URLEncoder.encode(agentId, "UTF-8") + "/");
				} catch (final UnsupportedEncodingException e) {
					return new URI(servletUrl + agentId + "/");
				}
			} catch (URISyntaxException e) {
				LOG.log(Level.WARNING, "Strange, couldn't generate agentUrl:"
						+ agentId, e);
			}
		}
		return null;
	}
	
	/**
	 * Get the id of an agent from its url. If the id cannot be extracted, null
	 * is returned. A typical url is "http://myserver.com/agents/agentid/"
	 * 
	 * @param agentUri
	 *            the agent url
	 * @return agentId
	 */
	@Override
	public String getAgentId(URI agentUri) {
		if (servletUrl != null) {
			String agentUrl = agentUri.toString();
			// add domain when missing
			final String domain = getDomain(agentUrl);
			if (domain.equals("")) {
				// provided url is only containing the path (not the domain)
				agentUrl = getDomain(servletUrl) + agentUrl;
			}
			
			if (agentUrl.startsWith(servletUrl)) {
				final int separator = agentUrl
						.indexOf('/', servletUrl.length());
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
	 *            the agent url
	 * @return the agent resource
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
				final int separator = agentUrl
						.indexOf('/', servletUrl.length());
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
	 *            the url
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
	
	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		final Map<String, Object> data = new HashMap<String, Object>();
		data.put("class", this.getClass().getName());
		data.put("servlet_url", servletUrl);
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
	public void reconnect(final String agentId) {
		// Nothing todo at this point
	}
	
	/*
	 * (non-Javadoc)
	 * 
	 * @see com.almende.eve.transport.TransportService#getKey()
	 */
	@Override
	public String getKey() {
		return "http://"
				+ (getServletUrl() == null ? "outbound" : getServletUrl());
	}
	
}
