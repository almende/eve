package com.almende.eve.entity.calendar;

public class Reminder {
	public String getMethod() {
		return method;
	}
	public void setMethod(String method) {
		this.method = method;
	}
	public Integer getMinutes() {
		return minutes;
	}
	public void setMinutes(Integer minutes) {
		this.minutes = minutes;
	}
	
	private String method = null;   // "email", "sms", "popup"
    private Integer minutes = null;
}
