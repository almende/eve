/**
 * Helper class to store a callback url and method
 */
package com.almende.eve.entity;

import java.io.Serializable;

@SuppressWarnings("serial")
public class Callback implements Serializable {
	public Callback(String id, String url, String method) {
		this.id = id;
		this.url = url;
		this.method = method;
	}
	
	public String id = null;
	public String url = null;
	public String method = null;
}
