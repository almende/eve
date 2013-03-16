package com.almende.eve.transport.http;

import java.util.Map;
import java.util.UUID;

import org.joda.time.DateTime;

import com.almende.eve.rpc.jsonrpc.jackson.JOM;
import com.almende.eve.state.FileStateFactory;

/**
 * Simple token system: Each outbound call gets a token, which is newly generated each hour. Last 5 tokens are
 * kept in memory. If remote peer wants to check if this host has actually send the call, it can request a resend of the 
 * token at time X. 
 *  
 * @author ludo
 *
 */
public class TokenStore {
	static final TokenStore me = new TokenStore();
	static final int SIZE = 5;
	static Map<String, Object> TOKENS;
	static DateTime last = DateTime.now();
	
	private TokenStore(){
		FileStateFactory factory = new FileStateFactory(".evecookies");
		if (factory.exists("_TokenStore")){
			TOKENS=factory.get("_TokenStore");
		} else {
			try {
				TOKENS = factory.create("_TokenStore");
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}
	
	public static String get(String time){
		try {
			return (String) TOKENS.get(time);
		} catch (Exception e){
//			e.printStackTrace();
			return null;
		}
	}
	public static TokenRet create(){
		synchronized(TOKENS){
			TokenRet result;
			if (TOKENS.size()==0 || TOKENS.get(last.toString()) == null || last.plus(3600000).isBeforeNow()){
				DateTime now = DateTime.now();
				String token = UUID.randomUUID().toString();
				result = me.new TokenRet(token,now);
				TOKENS.put(now.toString(), token);
				last = now;
//				System.err.println("TOKENS now:"+TOKENS.values());

				if (TOKENS.size()> SIZE+2){
					DateTime oldest = last;
					for (String time: TOKENS.keySet()){
						try {
						if (DateTime.parse(time).isBefore(oldest)) oldest = DateTime.parse(time);
						} catch (Exception e){};
					}
//					System.err.println("Removing token:"+oldest);
					TOKENS.remove(oldest);
				}
			} else {
//				System.err.println("TOKENS were:"+TOKENS.values());
				result = me.new TokenRet((String)TOKENS.get(last.toString()),last);
			}
			return result;
		}
	}
	class TokenRet {
		String token=null;
		String time=null;
		
		public TokenRet(String token, DateTime time){
			this.token=token;
			this.time=time.toString();
		}
		public String toString(){
			try {
				return JOM.getInstance().writeValueAsString(this);
			} catch (Exception e){
				e.printStackTrace();
				return "{\"token\":\""+token+"\",\"time\":\""+time+"\"}";
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