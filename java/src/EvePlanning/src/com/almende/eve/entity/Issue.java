package com.almende.eve.entity;

import java.io.Serializable;
import java.util.List;

@SuppressWarnings("serial")
public class Issue implements Serializable {
	public Issue() {}
	
	public void setCode(Integer code) {
		this.code = code;
	}
	public Integer getCode() {
		return code;
	}
	
	public void setType(TYPE type) {
		this.type = type;
	}

	public TYPE getType() {
		return type;
	}

	public void setMessage(String message) {
		this.message = message;
	}

	public String getMessage() {
		return message;
	}

	public void setTimestamp(String timestamp) {
		this.timestamp = timestamp;
	}

	public String getTimestamp() {
		return timestamp;
	}

	public void setHints(List<Hint> hints) {
		this.hints = hints;
	}
	
	public List<Hint> getHints() {
		return hints;
	}
	
	public boolean hasHints() {
		return (hints != null && hints.size() > 0);
	}

	public static enum TYPE {error, warning, weakWarning, info};
	
	// error codes
	// TODO: better implement error codes
	public static Integer NO_PLANNING = 1000;
	public static Integer IOEXCEPTION = 2000;
	public static Integer JSONRPCEXCEPTION = 2001;
	
	
	private Integer code = null;
	private TYPE type = null;
	private String message = null;
	private String timestamp = null;
	private List<Hint> hints = null;
}
