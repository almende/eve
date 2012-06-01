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

	public Integer getLat() {
		return lat;
	}

	public void setLat(Integer lat) {
		this.lat = lat;
	}

	public Integer getLng() {
		return lng;
	}

	public void setLng(Integer lng) {
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
	private Integer lat = null;  // latitude
	private Integer lng = null;  // longitude
}
