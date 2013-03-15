package com.almende.eve.transport.http;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.joda.time.DateTime;

import com.almende.eve.rpc.jsonrpc.jackson.JOM;

/**
 * Simple token system: Each outbound call gets a token, which is newly generated each hour. Last 5 tokens are
 * kept in memory. If remote peer wants to check if this host has actually send the call, it can request a resend of the 
 * token at time X. 
 *  
 * @author ludo
 *
 */
public class TokenStore {
	static final int SIZE = 5;
	static final Map<DateTime,String> TOKENS = new HashMap<DateTime,String>(SIZE+1);
	static DateTime last = DateTime.now();
	
	public static String get(String time){
		try {
			return TOKENS.get(new DateTime(time));
		} catch (Exception e){
			return null;
		}
	}
	public static TokenRet create(){
		synchronized(TOKENS){
			TokenRet result;
			if (TOKENS.size()==0 || last.plus(3600000).isBeforeNow()){
				DateTime now = DateTime.now();
				String token = UUID.randomUUID().toString();
				result = new TokenStore().new TokenRet(token,now);
				TOKENS.put(now, token);
				if (TOKENS.size()> SIZE){
					DateTime oldest = last;
					for (DateTime time: TOKENS.keySet()){
						if (time.isBefore(oldest)) oldest = time;
					}
					TOKENS.remove(oldest);
				}
			} else {
				result = new TokenStore().new TokenRet(TOKENS.get(last),last);
			}
			return result;
		}
	}
	class TokenRet {
		String token;
		String time;
		
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