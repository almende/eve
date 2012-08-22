package com.almende.eve.entity.calendar;

import java.io.Serializable;
import java.util.List;

import org.joda.time.Interval;

/**
 * Structure for storing data of a calendar agent, eventId and busy profile
 * this structure is used by the MeetingAgent
 */
@SuppressWarnings("serial")
public class AgentData implements Serializable {
	public String eventId = null;
	public String activityUpdated = null;
	public String eventUpdated = null;
	public List<Interval> busy = null;
}
