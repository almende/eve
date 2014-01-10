/**
 * Helper class to store a callback url and method
 */
package com.almende.eve.event;

import java.io.Serializable;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.almende.eve.rpc.jsonrpc.jackson.JOM;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * The Class Callback.
 */
@SuppressWarnings("serial")
public class Callback implements Serializable {
	private static final transient Logger	LOG		= Logger.getLogger(Callback.class
															.getCanonicalName());
	private String							id		= null;
	private String							url		= null;
	private String							method	= null;
	private String							params	= null;
	
	/**
	 * Instantiates a new callback.
	 */
	public Callback() {
	};
	
	/**
	 * Instantiates a new callback.
	 * 
	 * @param id
	 *            the id
	 * @param url
	 *            the url
	 * @param method
	 *            the method
	 * @param params
	 *            the params
	 */
	public Callback(final String id, final String url, final String method,
			final ObjectNode params) {
		this.id = id;
		this.url = url;
		this.method = method;
		try {
			this.params = JOM.getInstance().writeValueAsString(params);
		} catch (final Exception e) {
			LOG.log(Level.WARNING, "", e);
		}
	}
	
	/**
	 * Gets the id.
	 * 
	 * @return the id
	 */
	public String getId() {
		return id;
	}
	
	/**
	 * Sets the id.
	 * 
	 * @param id
	 *            the new id
	 */
	public void setId(final String id) {
		this.id = id;
	}
	
	/**
	 * Gets the url.
	 * 
	 * @return the url
	 */
	public String getUrl() {
		return url;
	}
	
	/**
	 * Sets the url.
	 * 
	 * @param url
	 *            the new url
	 */
	public void setUrl(final String url) {
		this.url = url;
	}
	
	/**
	 * Gets the method.
	 * 
	 * @return the method
	 */
	public String getMethod() {
		return method;
	}
	
	/**
	 * Sets the method.
	 * 
	 * @param method
	 *            the new method
	 */
	public void setMethod(final String method) {
		this.method = method;
	}
	
	/**
	 * Gets the params.
	 * 
	 * @return the params
	 */
	public String getParams() {
		return params;
	}
	
	/**
	 * Sets the params.
	 * 
	 * @param params
	 *            the new params
	 */
	public void setParams(final String params) {
		this.params = params;
	}
}
