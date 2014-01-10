/*
 * Copyright: Almende B.V. (2014), Rotterdam, The Netherlands
 * License: The Apache Software License, Version 2.0
 */
package com.almende.util.tokens;

import java.util.logging.Level;
import java.util.logging.Logger;

import org.joda.time.DateTime;

import com.almende.eve.rpc.jsonrpc.jackson.JOM;

/**
 * The Class TokenRet.
 */
public class TokenRet {
	private static final Logger	LOG		= Logger.getLogger(TokenRet.class
												.getCanonicalName());
	private String				token	= null;
	private String				time	= null;
	
	/**
	 * Instantiates a new token ret.
	 */
	public TokenRet() {
	}
	
	/**
	 * Instantiates a new token ret.
	 *
	 * @param token the token
	 * @param time the time
	 */
	public TokenRet(final String token, final DateTime time) {
		this.token = token;
		this.time = time.toString();
	}
	
	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		try {
			return JOM.getInstance().writeValueAsString(this);
		} catch (final Exception e) {
			LOG.log(Level.WARNING, "", e);
			return "{\"token\":\"" + token + "\",\"time\":\"" + time + "\"}";
		}
	}
	
	/**
	 * Gets the token.
	 *
	 * @return the token
	 */
	public String getToken() {
		return token;
	}
	
	/**
	 * Sets the token.
	 *
	 * @param token the new token
	 */
	public void setToken(final String token) {
		this.token = token;
	}
	
	/**
	 * Gets the time.
	 *
	 * @return the time
	 */
	public String getTime() {
		return time;
	}
	
	/**
	 * Sets the time.
	 *
	 * @param time the new time
	 */
	public void setTime(final String time) {
		this.time = time;
	}
}
