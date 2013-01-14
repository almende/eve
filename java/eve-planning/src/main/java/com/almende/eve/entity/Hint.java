package com.almende.eve.entity;

import java.io.Serializable;

import com.almende.eve.entity.activity.Constraints;

@SuppressWarnings("serial")
public class Hint implements Serializable {
	public Hint() {}
	
	public void setCode(Integer code) {
		this.code = code;
	}
	public Integer getCode() {
		return code;
	}

	public void setMessage(String message) {
		this.message = message;
	}

	public String getMessage() {
		return message;
	}

	public void setConstraints(Constraints constraints) {
		this.constraints = constraints;
	}

	public Constraints getConstraints() {
		return constraints;
	}

	private Integer code = null;
	private String message = null;
	private Constraints constraints = null;
}
