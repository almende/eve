package com.almende.eve.agent;

import java.util.List;

import com.almende.eve.json.annotation.Name;
import com.almende.eve.json.annotation.Required;

public interface AgentInterface {
	public String getId();
	public String getType();
	public String getVersion();
	public String getDescription();
	public List<String> getUrls();

	public List<Object> getMethods(
			@Name("asJSON") @Required(false) Boolean asJSON);
	
	public void onSubscribe (
			@Name("event") String event, 
			@Name("callbackUrl") String callbackUrl, 
			@Name("callbackMethod") String callbackMethod);
	public void onUnsubscribe (
			@Name("event") String event, 
			@Name("callbackUrl") String callbackUrl, 
			@Name("callbackMethod") String callbackMethod);
}
