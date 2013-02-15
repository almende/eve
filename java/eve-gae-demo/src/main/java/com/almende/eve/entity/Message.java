package com.almende.eve.entity;

import java.io.Serializable;
import java.util.Set;

@SuppressWarnings("serial")
public class Message implements Serializable {
	public Message () {}
	
	public Message(String from, Set<String> to, String description) {
		setFrom(from);
		setTo(to);
		setDescription(description);
	}
	
	public void setFrom(String from) {
		this.from = from;
	}
	public String getFrom() {
		return from;
	}
	public void setTo(Set<String> to) {
		this.to = to;
	}
	public Set<String> getTo() {
		return to;
	}
	public void setDescription(String description) {
		this.description = description;
	}
	public String getDescription() {
		return description;
	}

	public void setTimestamp(String timestamp) {
		this.timestamp = timestamp;
	}
	public String getTimestamp() {
		return timestamp;
	}

	public void setAgent(String agent) {
		this.agent = agent;
	}
	public String getAgent() {
		return agent;
	}

	public void setBox(String box) {
		this.box = box;
	}
	public String getBox() {
		return box;
	}

	public void setStatus(String status) {
		this.status = status;
	}
	public String getStatus() {
		return status;
	}

	// the actual message
	private String from = null;
	private Set<String> to = null;
	private String description = null;
	private String timestamp = null;
	
	// meta information for storing the message in the correct mailbox
	private String agent = null;  // the url of the agent
	private String box = null;    // "inbox" or "outbox"
	private String status = null; // "read", "unread", etc.
}
