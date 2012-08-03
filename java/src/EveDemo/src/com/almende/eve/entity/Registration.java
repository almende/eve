package com.almende.eve.entity;

import java.io.Serializable;

@SuppressWarnings("serial")
public class Registration implements Serializable {
	public void setDirectoryAgent(String directoryAgent) {
		this.directoryAgent = directoryAgent;
	}
	public String getDirectoryAgent() {
		return directoryAgent;
	}
	
	public void setType(String type) {
		this.type = type;
	}
	public String getType() {
		return type;
	}

	public void setUsername(String username) {
		this.username = username;
	}
	public String getUsername() {
		return username;
	}

	public void setEmail(String email) {
		this.email = email;
	}
	public String getEmail() {
		return email;
	}

	public void setAgent(String agent) {
		this.agent = agent;
	}
	public String getAgent() {
		return agent;
	}

	private String directoryAgent = null;
	private String type = null;
	private String username = null;
	private String email = null;
	private String agent = null;

}
