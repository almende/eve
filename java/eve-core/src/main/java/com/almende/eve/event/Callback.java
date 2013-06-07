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
	
	public String id = null;
	public String url = null;
	public String method = null;
	public String params = null;
}
