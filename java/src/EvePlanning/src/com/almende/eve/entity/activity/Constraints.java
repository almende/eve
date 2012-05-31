package com.almende.eve.entity.activity;

import java.io.Serializable;
import java.util.List;

@SuppressWarnings("serial")
public class Constraints implements Serializable {
	public List<Attendee> attendees = null;
	public List<String> locations = null;
	public Time time = null;
}
