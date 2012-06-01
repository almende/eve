package com.almende.eve.entity.activity;

import java.io.Serializable;

@SuppressWarnings("serial")
public class Time implements Serializable, Cloneable {
	public Time() {}
	
	public Integer getDuration() {
		return duration;
	}
	public void setDuration(Integer duration) {
		this.duration = duration;
	}
	
	public Integer getDurationMin() {
		return durationMin;
	}
	public void setDurationMin(Integer durationMin) {
		this.durationMin = durationMin;
	}
	
	public Integer getDurationMax() {
		return durationMax;
	}
	public void setDurationMax(Integer durationMax) {
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
	
	private Integer duration = null;        // seconds
	private Integer durationMin = null;     // seconds
	private Integer durationMax = null;     // seconds
	private String periodStart = null;
	private String periodEnd = null;
}
