package com.almende.eve.entity.activity;

import java.io.Serializable;

@SuppressWarnings("serial")
public class Time implements Serializable, Cloneable {
	public Time() {}
	
	public Long getDuration() {
		return duration;
	}
	public void setDuration(Long duration) {
		this.duration = duration;
	}
	
	public Long getDurationMin() {
		return durationMin;
	}
	public void setDurationMin(Long durationMin) {
		this.durationMin = durationMin;
	}
	
	public Long getDurationMax() {
		return durationMax;
	}
	public void setDurationMax(Long durationMax) {
		this.durationMax = durationMax;
	}
	
	public String getPeriodStart() {
		return periodStart;
	}
	public void setPeriodStart(String periodStart) {
		this.periodStart = periodStart;
	}
	
	public String getPeriodEnd() {
		return periodEnd;
	}
	public void setPeriodEnd(String periodEnd) {
		this.periodEnd = periodEnd;
	}
	
	public void merge(Time other) {
		if (other.duration != null) {
			duration = other.duration;
		}
		if (other.durationMin != null) {
			durationMin = other.durationMin;
		}
		if (other.durationMax != null) {
			durationMax = other.durationMax;
		}
		if (other.periodStart != null) {
			periodStart = other.periodStart;
		}
		if (other.periodEnd != null) {
			periodEnd = other.periodEnd;
		}
	}
	
	public Time clone() {
		Time clone = new Time();
		clone.duration = duration;
		clone.durationMin = durationMin;
		clone.durationMax = durationMax;
		clone.periodStart = periodStart;
		clone.periodEnd = periodEnd;
		return clone;
	}
	
	private Long duration = null;        // milliseconds
	private Long durationMin = null;     // milliseconds
	private Long durationMax = null;     // milliseconds
	private String periodStart = null;
	private String periodEnd = null;
}
