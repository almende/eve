package com.almende.eve.entity.calendar;

import java.util.Date;

import org.joda.time.DateTime;

public class Time {
    public Date getDate() {
		return date;
	}
	public void setDate(Date date) {
		this.date = date;
	}
	public DateTime getDateTime() {
		return dateTime;
	}
	public void setDateTime(DateTime dateTime) {
		this.dateTime = dateTime;
	}
	public String getTimeZone() {
		return timeZone;
	}
	public void setTimeZone(String timeZone) {
		this.timeZone = timeZone;
	}

	private Date date = null;
    private DateTime dateTime = null;
    private String timeZone = null;
}
