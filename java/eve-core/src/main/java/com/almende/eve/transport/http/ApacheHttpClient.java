package com.almende.eve.transport.http;

import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;

import org.apache.http.conn.ClientConnectionManager;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.scheme.SchemeRegistry;
import org.apache.http.conn.ssl.AllowAllHostnameVerifier;
import org.apache.http.conn.ssl.SSLSocketFactory;
import org.apache.http.conn.ssl.TrustSelfSignedStrategy;
import org.apache.http.conn.ssl.TrustStrategy;
import org.apache.http.conn.ssl.X509HostnameVerifier;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.conn.PoolingClientConnectionManager;
import org.apache.http.impl.conn.SchemeRegistryFactory;

public class ApacheHttpClient {

	static DefaultHttpClient get() throws KeyManagementException, UnrecoverableKeyException, NoSuchAlgorithmException, KeyStoreException{

		// Allow self-signed SSL certificates:
		TrustStrategy trustStrategy = new TrustSelfSignedStrategy();
		X509HostnameVerifier hostnameVerifier = new AllowAllHostnameVerifier();
		SSLSocketFactory sslSf = new SSLSocketFactory(trustStrategy,
				hostnameVerifier);
		Scheme https = new Scheme("https", 443, sslSf);
		
		SchemeRegistry schemeRegistry = SchemeRegistryFactory.createDefault();
		schemeRegistry.register(https);

		//Work with PoolingClientConnectionManager
		ClientConnectionManager connection = new PoolingClientConnectionManager(
				schemeRegistry);

		//Generate httpclient
		return new DefaultHttpClient(connection);
	}
	
}
