package com.almende.eve.entity.activity;

import java.io.Serializable;
import java.util.List;

@SuppressWarnings("serial")
public class Status implements Serializable {
	public String description = null;
	public List<Attendee> attendees = null;
	public String location = null;
	public String start = null;
	public String end = null;
	public String updated = null;
}
