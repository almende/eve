package com.almende.util.tokens;

import java.util.logging.Level;
import java.util.logging.Logger;

import org.joda.time.DateTime;

import com.almende.eve.rpc.jsonrpc.jackson.JOM;

public class TokenRet {
	private static final Logger		LOG		= Logger.getLogger(TokenRet.class
			.getCanonicalName());
	private String	token	= null;
	private String	time	= null;
	
	public TokenRet(){}
	
	public TokenRet(String token, DateTime time) {
		this.token = token;
		this.time = time.toString();
	}
	
	public String toString() {
		try {
			return JOM.getInstance().writeValueAsString(this);
		} catch (Exception e) {
			LOG.log(Level.WARNING, "", e);
			return "{\"token\":\"" + token + "\",\"time\":\"" + time
					+ "\"}";
		}
	}
	
	public String getToken() {
		return token;
	}
	public void setToken(String token) {
		this.token = token;
	}	
	public String getTime() {
		return time;
	}
	public void setTime(String time) {
		this.time = time;
	}
}
