/*
 * Copyright: Almende B.V. (2014), Rotterdam, The Netherlands
 * License: The Apache Software License, Version 2.0
 */
package com.almende.eve.transport.http;

import java.io.IOException;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.http.client.CookieStore;
import org.apache.http.client.params.ClientPNames;
import org.apache.http.client.params.CookiePolicy;
import org.apache.http.conn.ClientConnectionManager;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.scheme.SchemeRegistry;
import org.apache.http.conn.ssl.AllowAllHostnameVerifier;
import org.apache.http.conn.ssl.SSLSocketFactory;
import org.apache.http.conn.ssl.TrustSelfSignedStrategy;
import org.apache.http.conn.ssl.TrustStrategy;
import org.apache.http.conn.ssl.X509HostnameVerifier;
import org.apache.http.cookie.Cookie;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.conn.PoolingClientConnectionManager;
import org.apache.http.impl.conn.SchemeRegistryFactory;
import org.apache.http.params.CoreConnectionPNames;
import org.apache.http.params.HttpParams;

import com.almende.eve.agent.AgentHost;
import com.almende.eve.state.State;
import com.almende.eve.state.StateFactory;

/**
 * The Class ApacheHttpClient.
 */
public final class ApacheHttpClient {
	private static final Logger			LOG			= Logger.getLogger(ApacheHttpClient.class
															.getCanonicalName());
	private static DefaultHttpClient	httpClient	= null;
	
	/**
	 * Instantiates a new apache http client.
	 *
	 * @throws KeyManagementException the key management exception
	 * @throws UnrecoverableKeyException the unrecoverable key exception
	 * @throws NoSuchAlgorithmException the no such algorithm exception
	 * @throws KeyStoreException the key store exception
	 */
	private ApacheHttpClient() throws KeyManagementException,
			UnrecoverableKeyException, NoSuchAlgorithmException,
			KeyStoreException {
		// Allow self-signed SSL certificates:
		final TrustStrategy trustStrategy = new TrustSelfSignedStrategy();
		final X509HostnameVerifier hostnameVerifier = new AllowAllHostnameVerifier();
		final SSLSocketFactory sslSf = new SSLSocketFactory(trustStrategy,
				hostnameVerifier);
		final Scheme https = new Scheme("https", 443, sslSf);
		
		final SchemeRegistry schemeRegistry = SchemeRegistryFactory.createDefault();
		schemeRegistry.register(https);
		
		// Work with PoolingClientConnectionManager
		final ClientConnectionManager connection = new PoolingClientConnectionManager(
				schemeRegistry);
		
		// Provide eviction thread to clear out stale threads.
		new Thread(new Runnable() {
			@Override
			public void run() {
				try {
					while (true) {
						synchronized (this) {
							wait(5000);
							connection.closeExpiredConnections();
							connection.closeIdleConnections(30,
									TimeUnit.SECONDS);
						}
					}
				} catch (final InterruptedException ex) {
				}
			}
		}).start();
		
		// generate httpclient
		httpClient = new DefaultHttpClient(connection);
		
		// Set cookie policy and persistent cookieStore
		try {
			httpClient.setCookieStore(new MyCookieStore());
		} catch (final Exception e) {
			LOG.log(Level.WARNING,
					"Failed to initialize persistent cookieStore!", e);
		}
		final HttpParams params = httpClient.getParams();
		
		params.setParameter(ClientPNames.COOKIE_POLICY,
				CookiePolicy.BROWSER_COMPATIBILITY);
		params.setParameter(CoreConnectionPNames.SO_TIMEOUT, 60000);
		params.setParameter(CoreConnectionPNames.CONNECTION_TIMEOUT, 10000);
		params.setParameter(CoreConnectionPNames.STALE_CONNECTION_CHECK, false);
		httpClient.setParams(params);
	}
	
	/**
	 * Gets the.
	 *
	 * @return the default http client
	 * @throws KeyManagementException the key management exception
	 * @throws UnrecoverableKeyException the unrecoverable key exception
	 * @throws NoSuchAlgorithmException the no such algorithm exception
	 * @throws KeyStoreException the key store exception
	 */
	static DefaultHttpClient get() throws KeyManagementException,
			UnrecoverableKeyException, NoSuchAlgorithmException,
			KeyStoreException {
		if (httpClient == null) {
			new ApacheHttpClient();
		}
		return httpClient;
	}
	
	/**
	 * The Class MyCookieStore.
	 */
	class MyCookieStore implements CookieStore {
		// TODO: make StateFactory and COOKIESTORE config parameters
		
		/** The Constant COOKIESTORE. */
		static final String	COOKIESTORE	= "_CookieStore";
		
		/** The my state. */
		private State		myState		= null;
		
		/**
		 * Instantiates a new my cookie store.
		 *
		 * @throws IOException Signals that an I/O exception has occurred.
		 */
		MyCookieStore() throws IOException {
			final AgentHost host = AgentHost.getInstance();
			StateFactory factory = null;
			if (host.getConfig() != null) {
				factory = host.getStateFactoryFromConfig(host.getConfig(),
						"cookies");
			}
			if (factory == null) {
				factory = host.getStateFactory();
			}
			if (factory.exists(COOKIESTORE)) {
				myState = factory.get(COOKIESTORE);
			} else {
				myState = factory.create(COOKIESTORE);
				myState.setAgentType(CookieStore.class);
			}
		}
		
		/* (non-Javadoc)
		 * @see org.apache.http.client.CookieStore#addCookie(org.apache.http.cookie.Cookie)
		 */
		@Override
		public void addCookie(final Cookie cookie) {
			myState.put(Integer.valueOf(COOKIESTORE.hashCode()).toString(),
					cookie);
		}
		
		/* (non-Javadoc)
		 * @see org.apache.http.client.CookieStore#getCookies()
		 */
		@Override
		public List<Cookie> getCookies() {
			final List<Cookie> result = new ArrayList<Cookie>(myState.size());
			for (final String entryKey : myState.keySet()) {
				if (!entryKey.equals(State.KEY_AGENT_TYPE)) {
					result.add(myState.get(entryKey, Cookie.class));
				}
			}
			return result;
		}
		
		/* (non-Javadoc)
		 * @see org.apache.http.client.CookieStore#clearExpired(java.util.Date)
		 */
		@Override
		public boolean clearExpired(final Date date) {
			boolean result = false;
			
			for (final String entryKey : myState.keySet()) {
				if (!entryKey.equals(State.KEY_AGENT_TYPE)) {
					final Cookie cookie = myState.get(entryKey, Cookie.class);
					if (cookie.isExpired(date)) {
						myState.remove(entryKey);
						result = true;
					}
				}
			}
			return result;
		}
		
		/* (non-Javadoc)
		 * @see org.apache.http.client.CookieStore#clear()
		 */
		@Override
		public void clear() {
			myState.clear();
		}
		
		/**
		 * Gets the my state.
		 *
		 * @return the my state
		 */
		public State getMyState() {
			return myState;
		}
		
		/**
		 * Sets the my state.
		 *
		 * @param myState the new my state
		 */
		public void setMyState(final State myState) {
			this.myState = myState;
		}
	}
}
