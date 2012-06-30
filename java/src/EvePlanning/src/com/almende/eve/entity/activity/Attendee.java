package com.almende.eve.entity.activity;

import java.io.Serializable;

@SuppressWarnings("serial")
public class Attendee implements Serializable, Cloneable {
	public Attendee() {}
	
	public String getDisplayName() {
		return displayName;
	}
	public void setDisplayName(String displayName) {
		this.displayName = displayName;
	}
	
	public String getEmail() {
		return email;
	}
	public void setEmail(String email) {
		this.email = email;
	}
	
	public String getAgent() {
		return agent;
	}
	public void setAgent(String agent) {
		this.agent = agent;
	}
	
	public Boolean getOptional() {
		return optional;
	}
	public void setOptional(Boolean optional) {
		this.optional = optional;
	}
	
	public void setResponseStatus(String responseStatus) {
		this.responseStatus = responseStatus;
	}

	public String getResponseStatus() {
		return responseStatus;
	}
	
	public Attendee clone () {
		Attendee clone = new Attendee();
		clone.displayName = displayName;
		clone.email = email;
		clone.agent = agent;
		clone.optional = optional;
		clone.responseStatus = responseStatus;
		
		return clone;
	}

	private String displayName = null;
	private String email = null;
	private String agent = null;          // eve agent url
	private Boolean optional = null;      // if false, attendee must attend
	private String responseStatus = null; // can be "needsAction", "declined", "tentative", "accepted" 
}
