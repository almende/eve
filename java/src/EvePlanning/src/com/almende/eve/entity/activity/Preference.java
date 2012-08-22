package com.almende.eve.entity.activity;

import java.io.Serializable;

@SuppressWarnings("serial")
public class Preference implements Serializable, Cloneable {
	public Preference () {}
	
	public void setStart(String start) {
		this.start = start;
	}
	public String getStart() {
		return start;
	}

	public void setEnd(String end) {
		this.end = end;
	}

	public String getEnd() {
		return end;
	}

	public void setWeight(Double weight) {
		this.weight = weight;
	}

	public Double getWeight() {
		return weight;
	}

	public void merge(Preference other) {
		if (other.start != null) {
			start = other.start;
		}
		if (other.end != null) {
			end = other.end;
		}
		if (other.weight != null) {
			weight = other.weight;
		}
	}
	
	public Preference clone() {
		Preference clone = new Preference();
		clone.start = start;
		clone.end = end;
		clone.weight = weight;
		return clone;
	}
	
	private String start = null;
	private String end = null;
	private Double weight = null; // positive for preferred intervals, negative for undesirable intervals
}
