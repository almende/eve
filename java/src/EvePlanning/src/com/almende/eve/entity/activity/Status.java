package com.almende.eve.entity.activity;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

@SuppressWarnings("serial")
public class Status implements Serializable, Cloneable {
	public Status() {}

	public List<Attendee> getAttendees() {
		return attendees;
	}

	public List<Attendee> withAttendees() {
		if (attendees == null) {
			attendees = new ArrayList<Attendee>();			
		}
		return attendees;
	}
	
	public void setAttendees(List<Attendee> attendees) {
		this.attendees = attendees;
	}
	
	public Location withLocation() {
		if (location == null) {
			location = new Location();
		}
		return location;
	}

	public Location getLocation() {
		return location;
	}

	public void setLocation(Location location) {
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

	public void setActivityStatus(ACTIVITY_STATUS activityStatus) {
		this.activityStatus = activityStatus;
	}

	public ACTIVITY_STATUS getActivityStatus() {
		return activityStatus;
	}

	public String getUpdated() {
		return updated;
	}
	public void setUpdated(String updated) {
		this.updated = updated;
	}
	
	public void merge(Status other) {
		if (other.attendees != null) {
			attendees = new ArrayList<Attendee>();
			for (Attendee attendee : other.attendees) {
				attendees.add(attendee != null ? attendee.clone() : null);
			}
		}
		
		if (other.location != null) {
			if (location != null) {
				location.merge(other.location);
			}
			else {
				location = other.location.clone();
			}
		}
		if (other.start != null) {
			start = other.start;
		}
		if (other.end != null) {
			end = other.end;
		}
		if (other.activityStatus != null) {
			activityStatus = other.activityStatus;
		}
		if (other.updated != null) {
			updated = other.updated;
		}
	}
	
	public Status clone() {
		Status clone = new Status();
		
		if (attendees != null) {
			clone.attendees = new ArrayList<Attendee>();
			for (Attendee attendee : attendees) {
				clone.attendees.add(attendee != null ? attendee.clone() : null);
			}
		}
		if (location != null) {
			clone.location = location.clone();
		}
		clone.start = start;
		clone.end = end;
		clone.activityStatus = activityStatus;
		clone.updated = updated;
		
		return clone;
	}

	public static enum ACTIVITY_STATUS {progress, planned, executed, error};

	private List<Attendee> attendees = null;
	private Location location = null;
	private String start = null;
	private String end = null;
	private ACTIVITY_STATUS activityStatus = null;
	private String updated = null;
}
