package com.almende.eve.transport.http;

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
import com.almende.eve.config.Config;
import com.almende.eve.rpc.jsonrpc.JSONRPCException;
import com.almende.eve.rpc.jsonrpc.JSONRequest;
import com.almende.eve.rpc.jsonrpc.JSONResponse;
import com.almende.eve.transport.AsyncCallback;
import com.almende.eve.transport.TransportService;

public class HttpService implements TransportService {
	private static final Logger	LOG			= Logger.getLogger(HttpService.class
													.getCanonicalName());
	private String				servletUrl	= null;
	private List<String>		protocols	= Arrays.asList("http", "https");
	
	public HttpService() {
	}
	
	/**
	 * Construct an HttpService This constructor is called when the
	 * TransportService is constructed by the AgentHost
	 * 
	 * @param agentHost
	 * @param params
	 *            Available parameters: {String} servlet_url
	 */
	public HttpService(AgentHost agentHost, Map<String, Object> params) {
		if (params != null) {
			setServletUrl((String) params.get("servlet_url"));
		}
	}
	
	/**
	 * Construct an HttpService from a config The config can contain parameters:
	 * environment.Production.servlet_url environment.Development.servlet_url
	 * servlet_url The HttpService will select the parameter based on the
	 * current environment
	 * 
	 * @param params
	 *            Available parameters: {String} servlet_url
	 */
	public HttpService(Config config) {
	}
	
	/**
	 * Construct an HttpService
	 * 
	 * @param servletUrl
	 */
	public HttpService(String servletUrl) {
		setServletUrl(servletUrl);
	}
	
	/**
	 * Set the servlet url for the transport service. This determines the
	 * mapping between an agentId and agentUrl.
	 * 
	 * @param servletUrl
	 */
	private void setServletUrl(String servletUrl) {
		this.servletUrl = servletUrl;
		if (!this.servletUrl.endsWith("/")) {
			this.servletUrl += "/";
		}
		int separator = this.servletUrl.indexOf(':');
		if (separator != -1) {
			String protocol = this.servletUrl.substring(0, separator);
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
	 * @param senderId
	 *            Unused in the case of a HttpTransport
	 * @param receiverUrl
	 * @param request
	 * @return response
	 * @throws Exception
	 */
	@Override
	public JSONResponse send(final String senderUrl, final String receiverUrl,
			final JSONRequest request) throws JSONRPCException {
		try {
			JSONResponse response;
			String req = request.toString();
			
			// invoke via Apache HttpClient request:
			HttpPost httpPost = new HttpPost(receiverUrl);
			httpPost.setEntity(new StringEntity(req));
			
			// Add token for HTTP handshake
			httpPost.addHeader("X-Eve-Token", TokenStore.create().toString());
			httpPost.addHeader("X-Eve-SenderUrl", senderUrl);
			
			HttpResponse webResp = ApacheHttpClient.get().execute(httpPost);
			try {
				String result = EntityUtils.toString(webResp.getEntity());
				if (result != null) {
					response = new JSONResponse(result);
				} else {
					response = new JSONResponse();
				}
			} catch (JSONRPCException err) {
				response = new JSONResponse(err);
			} finally {
				httpPost.reset();
			}
			return response;
		} catch (Exception e) {
			throw new JSONRPCException("Failed to send RPC call through HTTP",
					e);
		}
	}
	
	/**
	 * Send an asynchronous JSON-RPC request to an agent via HTTP
	 * 
	 * @param senderId
	 * @param receiver
	 * @param receiverUrl
	 * @return response
	 * @throws IOException
	 */
	@Override
	public void sendAsync(final String senderUrl, final String receiverUrl,
			final JSONRequest request,
			final AsyncCallback<JSONResponse> callback) {
		new Thread(new Runnable() {
			@Override
			public void run() {
				JSONResponse response;
				try {
					response = send(senderUrl, receiverUrl, request);
					callback.onSuccess(response);
				} catch (Exception e) {
					callback.onFailure(e);
				}
			}
		}).start();
	}
	
	/**
	 * Get the url of an agent from its id.
	 * 
	 * @param agentId
	 * @return agentUrl
	 */
	@Override
	public String getAgentUrl(String agentId) {
		if (servletUrl != null) {
			try {
				return servletUrl + URLEncoder.encode(agentId,"UTF-8") + "/";
			} catch (UnsupportedEncodingException e) {
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
			String domain = getDomain(agentUrl);
			if (domain.isEmpty()) {
				// provided url is only containing the path (not the domain)
				agentUrl = getDomain(servletUrl) + agentUrl;
			}
			
			if (agentUrl.startsWith(servletUrl)) {
				int separator = agentUrl.indexOf('/', servletUrl.length());
				try {
					if (separator != -1) {
						return URLDecoder.decode(agentUrl.substring(
								servletUrl.length(), separator), "UTF-8");
					} else {
						return URLDecoder.decode(
								agentUrl.substring(servletUrl.length()),
								"UTF-8");
					}
				} catch (UnsupportedEncodingException e) {
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
			String domain = getDomain(agentUrl);
			if (domain.isEmpty()) {
				// provided url is only containing the path (not the domain)
				agentUrl = getDomain(servletUrl) + agentUrl;
			}
			
			if (agentUrl.startsWith(servletUrl)) {
				int separator = agentUrl.indexOf('/', servletUrl.length());
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
	public String getDomain(String url) {
		int protocolSeparator = url.indexOf("://");
		if (protocolSeparator != -1) {
			int fromIndex = (protocolSeparator != -1) ? protocolSeparator + 3
					: 0;
			int pathSeparator = url.indexOf('/', fromIndex);
			if (pathSeparator != -1) {
				return url.substring(0, pathSeparator);
			}
		}
		return "";
	}
	
	@Override
	public String toString() {
		Map<String, Object> data = new HashMap<String, Object>();
		data.put("class", this.getClass().getName());
		data.put("servlet_url", servletUrl);
		data.put("protocols", protocols);
		return data.toString();
	}
	
	@Override
	public void reconnect(String agentId) {
		// Nothing todo at this point
	}
	
	@Override
	public String getKey() {
		return "http://"
				+ (this.getServletUrl() == null ? "outbound" : this
						.getServletUrl());
	}
	
}
