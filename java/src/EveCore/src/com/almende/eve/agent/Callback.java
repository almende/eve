package com.almende.eve.agent;

import java.io.Serializable;


/**
 * Helper class to store a callback url and method
 */
@SuppressWarnings("serial")
public class Callback implements Serializable {
	public Callback(String callbackUrl, String callbackMethod) {
		this.callbackUrl = callbackUrl;
		this.callbackMethod = callbackMethod;
	}
	public String callbackUrl = null;
	public String callbackMethod = null;
}
