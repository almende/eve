package com.almende.eve.transport.http;

import java.util.logging.Level;
import java.util.logging.Logger;

import org.joda.time.DateTime;

import com.almende.eve.rpc.jsonrpc.jackson.JOM;
import com.almende.eve.state.FileStateFactory;
import com.almende.eve.state.State;
import com.almende.util.uuid.UUID;

/**
 * Simple token system: Each outbound call gets a token, which is newly
 * generated each hour. Last 5 tokens are
 * kept in memory. If remote peer wants to check if this host has actually send
 * the call, it can request a resend of the
 * token at time X.
 * 
 * @author ludo
 * 
 */
public final class TokenStore {
	private static final Logger		LOG		= Logger.getLogger(TokenStore.class
													.getCanonicalName());
	private static final TokenStore	ME		= new TokenStore();
	private static final int		SIZE	= 5;
	private static State			tokens;
	private static DateTime			last	= DateTime.now();
	
	private TokenStore() {
		FileStateFactory factory = new FileStateFactory(".evecookies");
		if (factory.exists("_TokenStore")) {
			tokens = factory.get("_TokenStore");
		} else {
			try {
				tokens = factory.create("_TokenStore");
			} catch (Exception e) {
				LOG.log(Level.WARNING, "", e);
			}
		}
	}
	
	public static String get(String time) {
		try {
			return tokens.get(time, String.class);
		} catch (Exception e) {
			return null;
		}
	}
	
	public static TokenRet create() {
		synchronized (tokens) {
			TokenRet result;
			if (tokens.size() == 0
					|| tokens.get(last.toString(), String.class) == null
					|| last.plus(3600000).isBeforeNow()) {
				DateTime now = DateTime.now();
				String token = new UUID().toString();
				result = ME.new TokenRet(token, now);
				tokens.put(now.toString(), token);
				last = now;
				
				if (tokens.size() > SIZE + 2) {
					DateTime oldest = last;
					for (String time : tokens.keySet()) {
						try {
							if (DateTime.parse(time).isBefore(oldest)) {
								oldest = DateTime.parse(time);
							}
						} catch (Exception e) {
						}
					}
					tokens.remove(oldest.toString());
				}
			} else {
				result = ME.new TokenRet(tokens.get(last.toString(),
						String.class), last);
			}
			return result;
		}
	}
	
	class TokenRet {
		private String	token	= null;
		private String	time	= null;
		
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
		
		public String getTime() {
			return time;
		}
	}
}