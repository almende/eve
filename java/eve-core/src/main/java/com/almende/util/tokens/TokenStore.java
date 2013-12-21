package com.almende.util.tokens;

import java.util.logging.Level;
import java.util.logging.Logger;

import org.joda.time.DateTime;

import com.almende.eve.agent.AgentHost;
import com.almende.eve.state.State;
import com.almende.eve.state.StateFactory;
import com.almende.util.uuid.UUID;

/**
 * Simple token system: Each outbound call gets a token, which is newly
 * generated each hour. Last 5 tokens are
 * kept in memory. If remote peer wants to check if this host has actually send
 * the call, it can request a resend of the
 * token at time X.
 * 
 * 
 * @author ludo
 * 
 */
public final class TokenStore {
	private static final Logger	LOG		= Logger.getLogger(TokenStore.class
												.getCanonicalName());
	private static final int	SIZE	= 5;
	private static State		tokens;
	private static DateTime		last	= DateTime.now();
	
	static {
		AgentHost host = AgentHost.getInstance();
		
		StateFactory factory = null;
		if (host.getConfig() != null) {
			factory = host
					.getStateFactoryFromConfig(host.getConfig(), "tokens");
		}
		if (factory == null) {
			factory = host.getStateFactory();
		}
		if (factory.exists("_TokenStore")) {
			tokens = factory.get("_TokenStore");
		} else {
			try {
				tokens = factory.create("_TokenStore");
				tokens.setAgentType(TokenStore.class);
			} catch (Exception e) {
				LOG.log(Level.WARNING, "", e);
			}
		}
	}
	
	private TokenStore() {
	};
	
	public static String get(String time) {
		try {
			String token =tokens.get(time, String.class);
			return token;
		} catch (Exception e) {
			LOG.log(Level.WARNING, "Exception during TokenStore get:", e);
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
				result = new TokenRet(token, now);
				tokens.put(now.toString(), token);
				last = now;
				
				if (tokens.size() > SIZE + 2) {
					DateTime oldest = last;
					for (String time : tokens.keySet()) {
						try {
							if (time.equals(State.KEY_AGENT_TYPE)) {
								continue;
							}
							if (DateTime.parse(time).isBefore(oldest)) {
								oldest = DateTime.parse(time);
							}
						} catch (Exception e) {
							LOG.log(Level.WARNING,
									"Failed in eviction of tokens:", e);
						}
					}
					tokens.remove(oldest.toString());
				}
			} else {
				result = new TokenRet(
						tokens.get(last.toString(), String.class), last);
			}
			return result;
		}
	}
}