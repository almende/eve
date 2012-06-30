package com.almende.eve.entity.activity;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

@SuppressWarnings("serial")
public class Constraints implements Serializable, Cloneable {
	public Constraints () {
		setAttendees(null);
		setLocation(null);
		setTime(null);
	}
	
	public List<Attendee> getAttendees() {
		return attendees;
	}
	public void setAttendees(List<Attendee> attendees) {
		this.attendees = attendees != null ? attendees : new ArrayList<Attendee>();
	}
	public Attendee getAttendee(String agent) {
		for (Attendee attendee : attendees) {
			if (attendee.getAgent().equals(agent)) {
				return attendee;
			}
		}
		return null;
	}
	
	public Location getLocation() {
		return location;
	}
	public void setLocation(Location location) {
		this.location = location != null ? location : new Location();
	}
	
	public Time getTime() {
		return time;
	}
	public void setTime(Time time) {
		this.time = time != null ? time : new Time();
	}
	
	public void merge(Constraints other) {
		// replace attendees
		attendees.clear();
		for (Attendee attendee : other.attendees) {
			attendees.add(attendee.clone());
		}
		
		location.merge(other.location);
		time.merge(other.time);
	}
	
	public Constraints clone() {
		Constraints clone = new Constraints ();
		
		clone.attendees = new ArrayList<Attendee>();
		for (Attendee attendee : attendees) {
			clone.attendees.add(attendee != null ? attendee.clone() : null);
		}
		clone.location = location.clone();
		clone.time = time.clone();
		
		return clone;
	}
	
	private List<Attendee> attendees = null;
	private Location location = null;
	private Time time = null;
}
