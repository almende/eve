/**
 * Helper class to store a callback url and method
 */
package com.almende.eve.entity;

import java.io.Serializable;

@SuppressWarnings("serial")
public class Callback implements Serializable {
	public Callback(String url, String method) {
		this.url = url;
		this.method = method;
	}
	public String url = null;
	public String method = null;
}
