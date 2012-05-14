package com.almende.eve.entity.calendar;

import org.joda.time.DateTime;

public class TimePeriod {
	public DateTime getStart() {
		return start;
	}
	public void setStart(DateTime start) {
		this.start = start;
	}
	public DateTime getEnd() {
		return end;
	}
	public void setEnd(DateTime end) {
		this.end = end;
	}
	
	private DateTime start = null;
	private DateTime end = null;
}
