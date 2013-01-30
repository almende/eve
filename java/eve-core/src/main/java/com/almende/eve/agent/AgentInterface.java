package com.almende.eve.agent;

import java.util.List;

import com.almende.eve.agent.annotation.Name;
import com.almende.eve.agent.annotation.Required;

public interface AgentInterface {
	public String getId();
	public String getType();
	public String getVersion();
	public String getDescription();
	public List<String> getUrls();

	public List<Object> getMethods(
			@Name("asJSON") @Required(false) Boolean asJSON);
	
	public String onSubscribe (
			@Name("event") String event, 
			@Name("callbackUrl") String callbackUrl, 
			@Name("callbackMethod") String callbackMethod);
	public void onUnsubscribe (
			@Required(false) @Name("subscriptionId") String subscriptionId, 
			@Required(false) @Name("event") String event, 
			@Required(false) @Name("callbackUrl") String callbackUrl, 
			@Required(false) @Name("callbackMethod") String callbackMethod);
}
