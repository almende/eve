package com.almende.eve.transport.http;

import java.io.IOException;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
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
import org.apache.http.impl.cookie.BasicClientCookie;
import org.apache.http.params.CoreConnectionPNames;
import org.apache.http.params.HttpParams;

import com.almende.eve.state.FileStateFactory;
import com.almende.eve.state.State;

public final class ApacheHttpClient {
	private static final Logger			LOG			= Logger.getLogger(ApacheHttpClient.class
															.getCanonicalName());
	private static DefaultHttpClient	httpClient	= null;
	
	private ApacheHttpClient() throws KeyManagementException,
			UnrecoverableKeyException, NoSuchAlgorithmException,
			KeyStoreException {
		// Allow self-signed SSL certificates:
		TrustStrategy trustStrategy = new TrustSelfSignedStrategy();
		X509HostnameVerifier hostnameVerifier = new AllowAllHostnameVerifier();
		SSLSocketFactory sslSf = new SSLSocketFactory(trustStrategy,
				hostnameVerifier);
		Scheme https = new Scheme("https", 443, sslSf);
		
		SchemeRegistry schemeRegistry = SchemeRegistryFactory.createDefault();
		schemeRegistry.register(https);
		
		// Work with PoolingClientConnectionManager
		ClientConnectionManager connection = new PoolingClientConnectionManager(
				schemeRegistry);
		
		// generate httpclient
		httpClient = new DefaultHttpClient(connection);
		
		// Set cookie policy and persistent cookieStore
		try {
			httpClient.setCookieStore(new MyCookieStore());
		} catch (Exception e) {
			LOG.log(Level.WARNING,
					"Failed to initialize persistent cookieStore!", e);
		}
		HttpParams params = httpClient.getParams();
		
		params.setParameter(ClientPNames.COOKIE_POLICY,
				CookiePolicy.BROWSER_COMPATIBILITY);
		params.setParameter(CoreConnectionPNames.SO_TIMEOUT, 60000);
		params.setParameter(CoreConnectionPNames.CONNECTION_TIMEOUT, 10000);
		params.setParameter(CoreConnectionPNames.STALE_CONNECTION_CHECK, false);
		httpClient.setParams(params);
	}
	
	static DefaultHttpClient get() throws KeyManagementException,
			UnrecoverableKeyException, NoSuchAlgorithmException,
			KeyStoreException {
		if (httpClient == null) {
			new ApacheHttpClient();
		}
		return httpClient;
	}
	
	class MyCookieStore implements CookieStore {
		// TODO: make StateFactory and COOKIESTORE config parameters
		
		static final String	COOKIESTORE	= "_CookieStore";
		private State		myState		= null;
		
		MyCookieStore() throws IOException {
			FileStateFactory factory = new FileStateFactory(".evecookies");
			if (factory.exists(COOKIESTORE)) {
				myState = factory.get(COOKIESTORE);
			} else {
				myState = factory.create(COOKIESTORE);
			}
		}
		
		@Override
		public void addCookie(Cookie cookie) {
			myState.put(Integer.valueOf(COOKIESTORE.hashCode()).toString(),
					(BasicClientCookie) cookie);
		}
		
		@Override
		public List<Cookie> getCookies() {
			List<Cookie> result = new ArrayList<Cookie>(myState.size());
			for (String entryKey : myState.keySet()) {
				result.add(myState.get(entryKey,Cookie.class));
			}
			return result;
		}
		
		@Override
		public boolean clearExpired(Date date) {
			boolean result = false;
			
			for (String entryKey : myState.keySet()) {
				Cookie cookie = myState.get(entryKey,Cookie.class);
				if (cookie.isExpired(date)){
					myState.remove(entryKey);
					result = true;
				}
			}
			return result;
		}
		
		@Override
		public void clear() {
			myState.clear();
		}

		public State getMyState() {
			return myState;
		}

		public void setMyState(State myState) {
			this.myState = myState;
		}
	}
}
