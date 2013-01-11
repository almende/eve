package com.almende.eve.session;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class Session {
	private HttpServletRequest request = null;
	private HttpServletResponse response = null;
	
	public Session(HttpServletRequest request, HttpServletResponse response) {
		this.request = request;
		this.response = response;
	}
	
	public HttpServletRequest getRequest() {
		return request;
	}
	
	public HttpServletResponse getResponse() {
		return response;
	}
}
