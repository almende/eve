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
	
	public boolean isMustAttend() {
		return mustAttend;
	}
	public void setMustAttend(boolean mustAttend) {
		this.mustAttend = mustAttend;
	}
	
	public Attendee clone () {
		Attendee clone = new Attendee();
		clone.displayName = displayName;
		clone.email = email;
		clone.agent = agent;
		clone.mustAttend = mustAttend;
		
		return clone;
	}
	
	private String displayName = null;
	private String email = null;
	private String agent = null;       // eve agent url
	private boolean mustAttend = true;
}
