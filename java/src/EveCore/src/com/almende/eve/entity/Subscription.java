package com.almende.eve.entity;

import java.io.Serializable;

@SuppressWarnings("serial")
public class Subscription implements Serializable {
	public String event;          // event to be subscribed to
	public String callbackUrl;    // url of an agent to be notified
	public String callbackMethod; // callback method for the agent 

	public Subscription (String event, String callbackUrl, 
			String callbackMethod) {
		this.event = event;
		this.callbackUrl = callbackUrl;
		this.callbackMethod = callbackMethod;
	}
}
