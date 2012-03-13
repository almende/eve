package eve.entity;

import java.io.Serializable;

public class Subscription implements Serializable {
	private static final long serialVersionUID = 1L;
	
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

