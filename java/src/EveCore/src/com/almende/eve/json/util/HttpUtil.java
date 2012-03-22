package com.almende.eve.json.util;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

public class HttpUtil {
	/**
	 * Send a request to an url and receive the response
	 * @param url        The url
	 * @throws IOException 
	 */
	static public String get(String url) throws IOException {
		return fetch("GET", url, null);
	}

	/**
	 * Send a request to an url and receive the response
	 * @param url        The url
	 * @param body       Request body
	 * @throws IOException 
	 */
	static public String post(String url, String body) throws IOException {
		return fetch("POST", url, body);
	}

	/**
	 * Send a request to an url and receive the response
	 * @param method 
	 * @param url
	 * @param body
	 * @return response
	 * @throws IOException 
	 */
	static public String fetch(String method, String url, String body) 
			throws IOException {
		URL serverAddress = new URL(url); 
	    HttpURLConnection conn = 
	    	(HttpURLConnection)serverAddress.openConnection();
	    conn.setConnectTimeout(10000);
	    conn.setReadTimeout(10000);
	    
	    /* TODO: authorization
	    // put auth token in the header when available
	    if (authTokensReceived != null && authTokensReceived.containsKey(url)) {
	    	String authToken = authTokensReceived.get(url); 
	    	conn.addRequestProperty("Authorization", "AgentLogin " + authToken);
	    }
	    */

	    if (method != null) {
	    	conn.setRequestMethod(method);
	    }
	    
	    if (body != null) {
		    conn.setDoOutput(true);

		    OutputStream os = conn.getOutputStream();
			os.write(body.getBytes());
			os.flush();
			os.close();
	    }
	    
	    InputStream is = conn.getInputStream();
	    String response = streamToString(is);
	    is.close();

	    return response;
	}
	
	static private String streamToString(InputStream in) throws IOException {
		StringBuffer out = new StringBuffer();
		byte[] b = new byte[4096];
		for (int n; (n = in.read(b)) != -1;) {
			out.append(new String(b, 0, n));
		}
		return out.toString();
	}
}
