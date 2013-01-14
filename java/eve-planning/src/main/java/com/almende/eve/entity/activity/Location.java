package com.almende.eve.entity.activity;

import java.io.Serializable;

@SuppressWarnings("serial")
public class Location implements Serializable, Cloneable {
	public Location() {}
	
	public String getSummary() {
		return summary;
	}

	public void setSummary(String summary) {
		this.summary = summary;
	}

	public Double getLat() {
		return lat;
	}

	public void setLat(Double lat) {
		this.lat = lat;
	}

	public Double getLng() {
		return lng;
	}

	public void setLng(Double lng) {
		this.lng = lng;
	}

	public void merge(Location other) {
		if (other.summary != null) {
			summary = other.summary;
		}
		if (other.lat != null) {
			lat = other.lat;
		}
		if (other.lng != null) {
			lng = other.lng;
		}
	}
	
	public Location clone() {
		Location clone = new Location();
		clone.summary = summary;
		clone.lat = lat;
		clone.lng = lng;
		return clone;
	}

	private String summary = null;
	private Double lat = null;  // latitude
	private Double lng = null;  // longitude
}
