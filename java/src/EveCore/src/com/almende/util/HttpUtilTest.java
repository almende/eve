package com.almende.util;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class HttpUtilTest {
	public static void main (String[] args) {
		try {
			String url1 = "http://www.google.com";
			url1 = "http://bit.ly/4hW294";
			String resp1 = HttpUtil.get(url1);
			System.out.println("GET " + url1);
			System.out.println(resp1);
		
			// append query parameters to url
			String url = "http://mydatabase.com/users";
			Map<String, String> params = new HashMap<String, String>();
			params.put("orderby", "name");
			params.put("limit", "10");
			params.put("query", "string with spaces & / chars");
			String fullUrl = HttpUtil.appendQueryParams(url, params);
			System.out.println("fullUrl=" + fullUrl);
			// fullUrl = "http://mydatabase.com/user?orderby=name&limit=10"
			
			Map<String, String> params2 = HttpUtil.getQueryParams(fullUrl);
			for (String param : params2.keySet()) {
				System.out.println(param + ":" + params2.get(param));
			}
			System.out.println(HttpUtil.removeQueryParams(fullUrl));
			
			
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
