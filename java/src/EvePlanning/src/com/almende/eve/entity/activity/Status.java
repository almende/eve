package com.almende.eve.entity.activity;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

@SuppressWarnings("serial")
public class Status implements Serializable, Cloneable {
	public Status() {
		setAttendees(null);
	}
	
	public String getDescription() {
		return description;
	}
	public void setDescription(String description) {
		this.description = description;
	}
	
	public List<Attendee> getAttendees() {
		return attendees;
	}
	public void setAttendees(List<Attendee> attendees) {
		this.attendees = attendees != null ? attendees : new ArrayList<Attendee>();
	}
	
	public String getLocation() {
		return location;
	}
	public void setLocation(String location) {
		this.location = location;
	}
	
	public String getStart() {
		return start;
	}
	public void setStart(String start) {
		this.start = start;
	}
	
	public String getEnd() {
		return end;
	}
	public void setEnd(String end) {
		this.end = end;
	}

	public void setError(String error) {
		this.error = error;
	}

	public String getError() {
		return error;
	}

	public String getUpdated() {
		return updated;
	}
	public void setUpdated(String updated) {
		this.updated = updated;
	}
	
	public void merge(Status other) {
		if (other.description != null) {
			description = other.description;
		}
		
		// replace attendees
		attendees.clear();
		for (Attendee attendee : attendees) {
			attendees.add(attendee.clone());
		}
		
		if (other.location != null) {
			location = other.location;
		}
		if (other.start != null) {
			start = other.start;
		}
		if (other.end != null) {
			end = other.end;
		}
		if (other.error != null) {
			error = other.error;
		}
		if (other.updated != null) {
			updated = other.updated;
		}
	}
	
	public Status clone() {
		Status clone = new Status();
		
		clone.description = description;
		clone.attendees = new ArrayList<Attendee>();
		for (Attendee attendee : attendees) {
			clone.attendees.add(attendee != null ? attendee.clone() : null);
		}
		clone.location = location;
		clone.start = start;
		clone.end = end;
		clone.error = error;
		clone.updated = updated;
		
		return clone;
	}

	private String description = null;
	private List<Attendee> attendees = null;
	private String location = null;
	private String start = null;
	private String end = null;
	private String error = null;
	private String updated = null;
}
