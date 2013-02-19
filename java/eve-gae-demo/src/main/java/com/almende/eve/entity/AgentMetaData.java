package com.almende.eve.entity;

import java.io.Serializable;

import com.google.code.twig.annotation.Id;

public class AgentMetaData implements Serializable {
	private static final long serialVersionUID = -4226057841340361619L;

	@Id private String id = null;
	private String type = null;
	
	public AgentMetaData() {}

	public AgentMetaData(String type, String id) {
		setType(type);
		setId(id);
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}
}
