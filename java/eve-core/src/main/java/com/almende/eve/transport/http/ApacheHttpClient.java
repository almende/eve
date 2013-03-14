package com.almende.eve.transport.http;

import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;

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

import com.almende.eve.state.FileStateFactory;
import com.almende.eve.state.State;

public class ApacheHttpClient {
	static DefaultHttpClient httpClient = null;

	private ApacheHttpClient() throws KeyManagementException, UnrecoverableKeyException, NoSuchAlgorithmException, KeyStoreException{
		// Allow self-signed SSL certificates:
		TrustStrategy trustStrategy = new TrustSelfSignedStrategy();
		X509HostnameVerifier hostnameVerifier = new AllowAllHostnameVerifier();
		SSLSocketFactory sslSf = new SSLSocketFactory(trustStrategy,
					hostnameVerifier);
		Scheme https = new Scheme("https", 443, sslSf);

		SchemeRegistry schemeRegistry = SchemeRegistryFactory
				.createDefault();
		schemeRegistry.register(https);

		// Work with PoolingClientConnectionManager
		ClientConnectionManager connection = new PoolingClientConnectionManager(
				schemeRegistry);

		// generate httpclient
		httpClient = new DefaultHttpClient(connection);
		
		//Set cookie policy and persistent cookieStore
		try {
			httpClient.setCookieStore(new MyCookieStore());
		} catch (Exception e) {
			System.err.println("Failed to initialize persistent cookieStore!");
			e.printStackTrace();
		}
		httpClient.getParams().setParameter(ClientPNames.COOKIE_POLICY,
				CookiePolicy.BROWSER_COMPATIBILITY);
	}
	static DefaultHttpClient get() throws KeyManagementException, UnrecoverableKeyException, NoSuchAlgorithmException, KeyStoreException {
		if (httpClient == null) {
			new ApacheHttpClient();
		}
		return httpClient;
	}
	class MyCookieStore implements CookieStore {
		static final String COOKIESTORE = "_CookieStore";
		State myState = null;
		
		MyCookieStore() throws Exception{
			FileStateFactory factory = new FileStateFactory(".evecookies");
			if (factory.exists(COOKIESTORE)){
				myState = factory.get(COOKIESTORE);
			} else {
				myState = factory.create(COOKIESTORE);
			}
		}
		
		@Override
		public void addCookie(Cookie cookie) {
			myState.put(new Integer(COOKIESTORE.hashCode()).toString(),cookie);
		}
		@Override
		public List<Cookie> getCookies() {
			List<Cookie> result = new ArrayList<Cookie>(myState.size());
			for (Entry<String,Object> entry : myState.entrySet()){
				result.add((Cookie) entry.getValue());
			}
			return result;
		}
		@Override
		public boolean clearExpired(Date date) {
			Iterator<Entry<String, Object>> iter = myState.entrySet().iterator();
			boolean result=false;
			while (iter.hasNext()){
				Entry<String,Object> next= iter.next();
				if (((Cookie)next.getValue()).isExpired(date)){
					iter.remove();
					result=true;
				}
			}
			return result;
		}

		@Override
		public void clear() {
			myState.clear();
		}
	}
}

