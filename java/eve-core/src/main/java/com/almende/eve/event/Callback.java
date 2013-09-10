/**
 * Helper class to store a callback url and method
 */
package com.almende.eve.event;

import java.io.Serializable;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.almende.eve.rpc.jsonrpc.jackson.JOM;
import com.fasterxml.jackson.databind.node.ObjectNode;

@SuppressWarnings("serial")
public class Callback implements Serializable {
	private static final transient Logger LOG = Logger.getLogger(Callback.class.getCanonicalName());
	
	public Callback(){};
	
	public Callback(String id, String url, String method, ObjectNode params) {
		this.id = id;
		this.url = url;
		this.method = method;
		try {
			this.params = JOM.getInstance().writeValueAsString(params);
		} catch (Exception e) {
			LOG.log(Level.WARNING,"",e);
		}
	}
	
	private String id = null;
	private String url = null;
	private String method = null;
	private String params = null;

	public String getId() {
		return id;
	}
	public void setId(String id) {
		this.id = id;
	}
	public String getUrl() {
		return url;
	}
	public void setUrl(String url) {
		this.url = url;
	}
	public String getMethod() {
		return method;
	}
	public void setMethod(String method) {
		this.method = method;
	}
	public String getParams() {
		return params;
	}
	public void setParams(String params) {
		this.params = params;
	}
}
