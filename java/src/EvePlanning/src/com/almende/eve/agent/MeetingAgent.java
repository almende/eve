/**
 * @brief 
 * The MeetingAgent can dynamically schedule a meeting with multiple attendees.
 *
 * The MeetingAgent synchronizes a meeting for one or multiple attendees, 
 * and dynamically schedules the meeting on a free time slot in all calendars, 
 * reckoning with office hours (Mon-Fri, 9:00-17:00, CET). The duration, 
 * summary, location, and one or multiple attendees can be specified. 
 * After having created a meeting, the meeting can be updated (add/remove
 * attendees, change summary, duration, start time, etc.). The meetings can be
 * changed in both your Google Calendar and via the MeetingAgent itself.
 * 
 * The MeetingAgent regularly checks for updates its meeting and reschedules it
 * when needed. The update frequency depends on the time the meeting was last 
 * changed. When just changed, the MeetingAgent checks every 10 seconds, and 
 * this interval is linearly decreased towards once an hour. 
 * 
 * The MeetingAgent uses Activity as data structure, and uses this structure
 * to describe a meeting. To setup a MeetingAgent call the method setActivity
 * or updateActivity. The MeetingAgent will automatically start scheduling and
 * monitoring the meeting, and stops with monitoring once the event is past.
 * 
 * Core methods are:
 *     setActivity     Clear current meeting and setup a new meeting
 *     updateActivity  Update current meeting
 *     update          Force an update: synchronize and reschedule the meeting
 *     clear           Remove the meetings from the attendees calendars, and
 *                     delete all stored information.
 * 
 * A minimal, valid Activity structure looks like:
 *     {
 *         "summary": "Test C",
 *         "constraints": {
 *             "attendees": [
 *                 {
 *                     "agent": "http://myserver.com/agents/googlecalendaragent/123/",
 *                 },
 *                 {
 *                     "agent": "http://myserver.com/agents/googlecalendaragent/456/",
 *                 }
 *             ]
 *         }
 *     }
 * 
 * 
 * @license
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy 
 * of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 *
 * Copyright © 2012 Almende B.V.
 *
 * @author 	Jos de Jong, <jos@almende.org>
 * @date   2012-07-03
 */

package com.almende.eve.agent;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;

import org.joda.time.DateTime;
import org.joda.time.Duration;
import org.joda.time.Interval;

import com.almende.eve.context.Context;
import com.almende.eve.entity.activity.Activity;
import com.almende.eve.entity.activity.Attendee;
import com.almende.eve.entity.calendar.CalendarAgentData;
import com.almende.eve.json.JSONRPCException;
import com.almende.eve.json.JSONRequest;
import com.almende.eve.json.annotation.Name;
import com.almende.eve.json.annotation.Required;
import com.almende.eve.json.jackson.JOM;
import com.almende.util.IntervalsUtil;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

// TODO: rename to AppointmentAgent?
public class MeetingAgent extends Agent {
	private Logger logger = Logger.getLogger(this.getClass().getName());
	private int LOOK_AHEAD_DAYS = 7; // number of days to look ahead when
										// planning a meeting

	/**
	 * Set the event for this meeting agent
	 * 
	 * @param summary
	 *            Description for the meeting
	 * @param location
	 * @param duration
	 *            Duration in minutes
	 * @param agents
	 *            List with calendar agent urls of the attendees
	 */
	public void setEvent(@Name("summary") String summary,
			@Required(false) @Name("location") String location,
			@Name("duration") Integer duration,
			@Name("agents") List<String> agents) {
		Activity activity = new Activity();
		activity.setSummary(summary);
		activity.getConstraints().getLocation().setSummary(location);
		for (String agent : agents) {
			Attendee attendee = new Attendee();
			attendee.setAgent(agent);
			activity.getConstraints().getAttendees().add(attendee);
		}

		update();
	}

	/**
	 * Set a new activity. Currently stored activity will be removed.
	 * @param activity
	 * @return
	 * @throws Exception
	 */
	public Activity setActivity(@Name("activity") Activity activity)
			throws Exception {
		clear();

		return updateActivity(activity);
	}

	/**
	 * Get a set with attendee agent urls from an activity returns an empty list
	 * when no attendees
	 * 
	 * @param activity
	 * @return
	 */
	private Set<String> getAttendeesAgents(Activity activity) {
		Set<String> agents = new TreeSet<String>();
		for (Attendee attendee : activity.getConstraints().getAttendees()) {
			agents.add(attendee.getAgent());
		}

		return agents;
	}

	/**
	 * update the activity for meeting agent
	 * 
	 * @param activity
	 * @throws Exception
	 */
	public Activity updateActivity(@Name("activity") Activity updatedActivity)
			throws Exception {
		Activity activity = (Activity) getContext().get("activity");
		if (activity == null) {
			activity = new Activity();
		}

		Set<String> prevAttendees = getAttendeesAgents(activity);

		/*
		 * TODO: cleanup, this check just gives issues // check if the correct
		 * agent url is entered String agent = updatedActivity.getAgent();
		 * String myUrl = getUrl(); if (agent != null) { if
		 * (!agent.equals(myUrl)) { throw new
		 * Exception("Activity contains an agent url which does not match my url"
		 * ); // TODO: more detailed error description (output urls) } }
		 */

		// if no updated timestamp is provided, set the timestamp to now
		if (updatedActivity.getStatus().getUpdated() == null) {
			updatedActivity.getStatus().setUpdated(DateTime.now().toString());
		}

		// synchronize with the stored activity
		activity = Activity.sync(activity, updatedActivity);

		// ensure the url of the meeting agent is filled in
		String myUrl = getUrl();
		activity.setAgent(myUrl);

		// create duration when missing
		Long duration = activity.getConstraints().getTime().getDuration();
		if (duration == null) {
			duration = Duration.standardHours(1).getMillis(); // 1 hour in ms
			activity.getConstraints().getTime().setDuration(duration);
		}

		// remove calendar events from removed attendees
		Set<String> currentAttendees = getAttendeesAgents(activity);
		Set<String> removedAttendees = new TreeSet<String>(prevAttendees);
		removedAttendees.removeAll(currentAttendees);
		for (String attendee : removedAttendees) {
			clearAttendee(attendee);
		}

		getContext().put("activity", activity);

		// update all attendees, start timer to regularly check
		update();

		return (Activity) getContext().get("activity");
	}

	/**
	 * Get meeting summary
	 * 
	 * @return
	 */
	public String getSummary() {
		Activity activity = (Activity) getContext().get("activity");
		return (activity != null) ? activity.getSummary() : null;
	}

	/**
	 * get meeting activity returns null if no activity has been initialized.
	 * 
	 * @return
	 */
	public Activity getActivity() {
		return (Activity) getContext().get("activity");
	}

	/**
	 * Apply the constraints of the the activity (for example duration)
	 * 
	 * @param activity
	 */
	private void applyConstraints() {
		Activity activity = (Activity) getContext().get("activity");
		if (activity == null) {
			return;
		}

		// reset the error
		if (activity.getStatus().getError() != null) {
			activity.getStatus().setError(null);
			activity.getStatus().setUpdated(DateTime.now().toString());
			
			// store the changed activity in the context
			getContext().put("activity", activity);
		}
		
		// check time constraints
		Long duration = activity.getConstraints().getTime().getDuration();
		if (duration != null) {
			String start = activity.getStatus().getStart();
			String end = activity.getStatus().getEnd();
			if (start != null && end != null) {
				DateTime startTime = new DateTime(start);
				DateTime endTime = new DateTime(end);
				Interval interval = new Interval(startTime, endTime);
				if (interval.toDurationMillis() != duration) {
					logger.info("status did not match constraints. "
							+ "Changed end time to match the duration of "
							+ duration + " ms");

					// duration does not match. adjust the end time
					endTime = startTime.plus(duration);
					activity.getStatus().setEnd(endTime.toString());
					activity.getStatus().setUpdated(DateTime.now().toString());

					// store the changed activity in the context
					getContext().put("activity", activity);
				}
			}
		}
	}

	/**
	 * synchronize the meeting in all attendees calendars
	 */
	private boolean syncEvents() {
		logger.info("update started");
		Activity activity = (Activity) getContext().get("activity");

		boolean changed = false;
		if (activity != null) {
			String updatedBefore = activity.getStatus().getUpdated();

			for (Attendee attendee : activity.getConstraints().getAttendees()) {
				String agent = attendee.getAgent();
				syncEvent(agent);
			}

			activity = (Activity) getContext().get("activity");
			String updatedAfter = activity.getStatus().getUpdated();

			changed = !updatedBefore.equals(updatedAfter);
		}

		return changed;
	}

	/**
	 * Schedule or re-schedule the meeting. Synchronize the events, retrieve
	 * busy profiles, re-schedule the event.
	 */
	public void update() {
		// TODO: optimize the update method
		logger.info("updating...");

		// stop running tasks
		stopAutoUpdate();
		
		boolean changed = syncEvents();
		if (changed) {
			syncEvents();
		}

		applyConstraints();

		updateBusyIntervals();

		boolean rescheduled = schedule();
		if (rescheduled) {
			changed = syncEvents();
			if (changed) {
				syncEvents();
			}
		}
		
		// Check if the activity is finished
		// If not, schedule a new update task. Else we are done
		Activity activity = getActivity();
		String start = (activity != null) ? 
				activity.getStatus().getStart() : null;
		boolean isFinished = false;
		if (start != null && (new DateTime(start)).isBefore(DateTime.now())) {
			isFinished = true;
		}
		if (activity != null && !isFinished) {
			startAutoUpdate();
		} else {
			logger.info("The activity is over, my work is done. Goodbye world.");
		}		
		
	}

	/**
	 * Get the timestamp of the next rounded hour
	 * 
	 * @return
	 */
	/*
	 * TODO: cleanup private DateTime getNextHour() { DateTime nextHour =
	 * DateTime.now(); nextHour =
	 * nextHour.minusMillis(nextHour.getMillisOfSecond()); nextHour =
	 * nextHour.minusSeconds(nextHour.getSecondOfMinute()); nextHour =
	 * nextHour.minusMinutes(nextHour.getMinuteOfHour()); nextHour =
	 * nextHour.plusHours(1); return nextHour; }
	 */

	/**
	 * Get the timestamp rounded to the next half hour
	 * 
	 * @return
	 */
	private DateTime getNextHalfHour() {
		DateTime nextHalfHour = DateTime.now();
		nextHalfHour = nextHalfHour.minusMillis(nextHalfHour
				.getMillisOfSecond());
		nextHalfHour = nextHalfHour.minusSeconds(nextHalfHour
				.getSecondOfMinute());

		if (nextHalfHour.getMinuteOfHour() > 30) {
			nextHalfHour = nextHalfHour.minusMinutes(nextHalfHour
					.getMinuteOfHour());
			nextHalfHour = nextHalfHour.plusMinutes(60);
		} else {
			nextHalfHour = nextHalfHour.minusMinutes(nextHalfHour
					.getMinuteOfHour());
			nextHalfHour = nextHalfHour.plusMinutes(30);
		}

		return nextHalfHour;
	}

	/**
	 * Schedule the meeting based on currently known event status and busy
	 * intervals
	 * 
	 * @return rescheduled Returns true if the activity has been rescheduled
	 *         When rescheduled, events must be synchronized again with
	 *         syncEvents.
	 */
	@SuppressWarnings("unchecked")
	private boolean schedule() {
		// read activity and busy intervals from the context
		Context context = getContext();
		Activity activity = (Activity) context.get("activity");
		if (activity == null) {
			return false;
		}
		List<Interval> busy = (List<Interval>) context.get("busy");
		if (busy == null) {
			busy = new ArrayList<Interval>();
		}

		// read planned start and end from the activity
		DateTime activityStart = null;
		if (activity.getStatus().getStart() != null) {
			activityStart = new DateTime(activity.getStatus().getStart());
		}
		DateTime activityEnd = null;
		if (activity.getStatus().getEnd() != null) {
			activityEnd = new DateTime(activity.getStatus().getEnd());
		}
		boolean planned = (activityStart != null && activityEnd != null);

		// check if there is a double booking
		boolean overlaps = false;
		if (planned) {
			Interval activityInterval = new Interval(activityStart, activityEnd);
			overlaps = IntervalsUtil.overlaps(activityInterval, busy);
		}

		if (!planned) {
			logger.info("Activity is not yet planned");
		}
		if (overlaps) {
			logger.info("Activity overlaps with another activity");
		}

		if (!planned || overlaps) {
			// the activity is not planned yet, or there is a double booking
			// replan the activity

			// get the duration of the activity
			Long durationLong = activity.getConstraints().getTime()
					.getDuration();
			Duration activityDuration;
			if (durationLong != null) {
				activityDuration = new Duration(durationLong);
			} else {
				// TODO: give error when duration is not defined?
				activityDuration = Duration.standardHours(1);
			}

			// find the first available interval long enough to contain the
			// activity
			Interval theChosenOne = null;
			DateTime timeMin = getNextHalfHour();
			DateTime timeMax = timeMin.plusDays(LOOK_AHEAD_DAYS);
			List<Interval> available = IntervalsUtil.inverse(busy, timeMin,
					timeMax);
			for (Interval interval : available) {
				Duration duration = interval.toDuration();
				if (duration.isLongerThan(activityDuration)
						|| duration.isEqual(activityDuration)) {
					theChosenOne = interval;
					break;
				}
			}

			// if there is an interval found, adjust the activity
			if (theChosenOne != null) {
				activityStart = new DateTime(theChosenOne.getStart());
				activityEnd = activityStart.plus(activityDuration);
				activity.getStatus().setStart(activityStart.toString());
				activity.getStatus().setEnd(activityEnd.toString());
			} else {
				// TODO: escape: no planning possible
				String message = "No free interval found for the meeting";
				activity.getStatus().setError(message);

				// trigger an error event
				try {
					String event = "error";
					ObjectNode params = JOM.createObjectNode();
					params.put("description", message);
					trigger(event, params);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
			activity.getStatus().setUpdated(DateTime.now().toString());

			logger.info("Activity rescheduled start=" + activityStart
					+ ", end=" + activityEnd);

			// TODO: if changed, store the activity again
			context.put("activity", activity);
			return true;
		}

		return false;
	}

	/**
	 * Start automatic updating
	 * The interval of the update task depends on the timestamp the activity
	 * is last updated. When recently updated, the interval is smaller.
	 * interval is  minimum 10 sec and maximum 1 hour.
	 * @throws IOException
	 * @throws JSONRPCException
	 * @throws JsonMappingException
	 * @throws JsonParseException
	 */
	public void startAutoUpdate() {
		Context context = getContext();
		Activity activity = getActivity();
		
		// determine the interval (1 hour by default)
		long TEN_SECONDS = 10 * 1000;
		long ONE_HOUR = 60 * 60 * 1000;
		long interval = ONE_HOUR; // default is 1 hour
		if (activity != null) {
			String updated = activity.getStatus().getUpdated();
			if (updated != null) {
				DateTime dateUpdated = new DateTime(updated);
				DateTime now = DateTime.now();
				interval = new Interval(dateUpdated, now).toDurationMillis();
			}
		}
		if (interval < TEN_SECONDS) {
			interval = TEN_SECONDS;
		}
		if (interval > ONE_HOUR) {
			interval = ONE_HOUR;
		}
		
		// stop any running task
		stopAutoUpdate();

		// schedule an update task and store the task id
		JSONRequest request = new JSONRequest("update", null);
		String task = context.getScheduler().createTask(request, interval);
		context.put("updateTask", task);

		logger.info("Auto update started. Interval = " + interval
				+ " milliseconds");
	}

	/**
	 * Stop automatic updating
	 */
	public void stopAutoUpdate() {
		Context context = getContext();

		String task = (String) context.get("updateTask");
		if (task != null) {
			context.getScheduler().cancelTask(task);
			context.remove("updateTask");
		}

		logger.info("Auto update stopped");
	}

	/**
	 * Merge an event into an Activity
	 * 
	 * @param activity
	 * @param event
	 */
	private void merge(Activity activity, ObjectNode event) {
		// agent
		String agent = null;
		if (event.has("agent")) {
			agent = event.get("agent").asText();
		}
		activity.setAgent(agent);

		// summary
		String summary = null;
		if (event.has("summary")) {
			summary = event.get("summary").asText();
		}
		activity.setSummary(summary);

		// updated
		String updated = null;
		if (event.has("updated")) {
			updated = event.get("updated").asText();
		}
		activity.getStatus().setUpdated(updated);

		// start
		String start = null;
		if (event.with("start").has("dateTime")) {
			start = event.with("start").get("dateTime").asText();
		}
		activity.getStatus().setStart(start);

		// end
		String end = null;
		if (event.with("end").has("dateTime")) {
			end = event.with("end").get("dateTime").asText();
		}
		activity.getStatus().setEnd(end);

		// duration
		if (start != null && end != null) {
			Interval interval = new Interval(new DateTime(start), new DateTime(
					end));
			Long duration = interval.toDurationMillis();
			activity.getConstraints().getTime().setDuration(duration);
		}

		// location
		String location = null;
		if (event.has("location")) {
			location = event.get("location").asText();
		}
		activity.getConstraints().getLocation().setSummary(location);
	}

	/**
	 * Merge an activity into an event
	 * 
	 * @param event
	 * @param activity
	 */
	private void merge(ObjectNode event, Activity activity) {
		event.put("agent", activity.getAgent());
		event.put("summary", activity.getSummary());
		event.put("updated", activity.getStatus().getUpdated());
		event.put("location", activity.getConstraints().getLocation()
				.getSummary());

		event.with("start").put("dateTime", activity.getStatus().getStart());
		event.with("end").put("dateTime", activity.getStatus().getEnd());
	}

	/**
	 * Retrieve the data of a single calendar agent from the context
	 * 
	 * @param agentUrl
	 * @return data returns the calendar data. If not available, a new, empty
	 *         CalendarAgentData is returned.
	 */
	@SuppressWarnings("unchecked")
	private CalendarAgentData getAttendeeData(String agentUrl) {
		Map<String, CalendarAgentData> calendarAgents = (Map<String, CalendarAgentData>) getContext()
				.get("calendarAgents");

		return calendarAgents != null ? calendarAgents.get(agentUrl)
				: new CalendarAgentData();
	}

	/**
	 * Put data for a calendar agent into the context
	 * 
	 * @param agentUrl
	 * @param data
	 */
	@SuppressWarnings("unchecked")
	private void putAttendeeData(String agentUrl, CalendarAgentData data) {
		Context context = getContext();
		Map<String, CalendarAgentData> calendarAgents = (Map<String, CalendarAgentData>) context
				.get("calendarAgents");
		if (calendarAgents == null) {
			calendarAgents = new HashMap<String, CalendarAgentData>();
		}

		calendarAgents.put(agentUrl, data);
		context.put("calendarAgents", calendarAgents);
	}

	/**
	 * Remove a calendar agent data from the context
	 * 
	 * @param agentUrl
	 * @param data
	 */
	@SuppressWarnings("unchecked")
	private void removeAttendeeData(String agentUrl) {
		Context context = getContext();
		Map<String, CalendarAgentData> calendarAgents = (Map<String, CalendarAgentData>) context
				.get("calendarAgents");
		if (calendarAgents != null && calendarAgents.containsKey(agentUrl)) {
			calendarAgents.remove(agentUrl);
			context.put("calendarAgents", calendarAgents);
		}
	}

	/**
	 * Retrieve the stored eventId of a calendar agent from the context
	 * 
	 * @param agentUrl
	 * @return data returns the calendar data, or null if not available
	 */
	private String getAttendeeEventId(String agentUrl) {
		CalendarAgentData data = getAttendeeData(agentUrl);
		if (data != null) {
			return data.eventId;
		}
		return null;
	}

	/**
	 * Put the eventId for a calendar agent into the context
	 * 
	 * @param agentUrl
	 * @param data
	 */
	private void putAttendeeEventId(String agentUrl, String eventId) {
		CalendarAgentData data = getAttendeeData(agentUrl);
		if (data == null) {
			data = new CalendarAgentData();
		}
		data.eventId = eventId;

		putAttendeeData(agentUrl, data);
	}

	/**
	 * Retrieve the busy intervals of a calendar agent from the context
	 * 
	 * @param agentUrl
	 * @return busy returns busy intervals, or null if not available
	 */
	private List<Interval> getAttendeeBusy(String agentUrl) {
		CalendarAgentData data = getAttendeeData(agentUrl);
		if (data != null) {
			return data.busy;
		}
		return null;
	}

	/**
	 * Put the busy intervals for a calendar agent into the context
	 * 
	 * @param agentUrl
	 * @param busy
	 */
	private void putAttendeeBusy(String agentUrl, List<Interval> busy) {
		CalendarAgentData data = getAttendeeData(agentUrl);
		if (data == null) {
			data = new CalendarAgentData();
		}
		data.busy = busy;

		putAttendeeData(agentUrl, data);
	}

	/**
	 * Synchronize the event with given calendar agent
	 * 
	 * @param agent
	 */
	private void syncEvent(@Name("agent") String agent) {
		logger.info("updateEvent started for agent " + agent);
		Context context = getContext();

		// TODO: in this method, reckon with the attendee responseStatus

		// retrieve event
		ObjectNode event = null;
		String eventId = getAttendeeEventId(agent);
		if (eventId != null) {
			ObjectNode params = JOM.createObjectNode();
			params.put("eventId", eventId);
			try {
				event = send(agent, "getEvent", params, ObjectNode.class);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (JSONRPCException e) {
				if (e.getCode() == 404) {
					// event was canceled by the user.
					/*
					 * TODO: change the status of this attendee
					 * setAttendeeStatus(activity, agent, "declined");
					 */
					e.printStackTrace();
				} else {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}

		if (event == null) {
			event = JOM.createObjectNode();
		}
		Activity eventActivity = new Activity();
		merge(eventActivity, event);

		Activity activity = (Activity) context.get("activity");
		if (activity.isAfter(eventActivity)) {
			// activity is updated (event is out-dated or not yet existing)

			// TODO: cleanup
			try {
				logger.info("activity is newer than event. Updating event. Activity="
						+ JOM.getInstance().writeValueAsString(activity));
			} catch (Exception e) {
				e.printStackTrace();
			}

			// merge the activity into the event
			merge(event, activity);

			// save the event
			ObjectNode params = JOM.createObjectNode();
			params.put("event", event);
			try {
				String method = event.has("id") ? "updateEvent" : "createEvent";
				ObjectNode updatedEvent = send(agent, method, params,
						ObjectNode.class);

				// TODO: cleanup logging
				logger.info("method=" + method + ", params="
						+ JOM.getInstance().writeValueAsString(params));

				// store new eventId
				// TODO: only needed in case of creation?
				if (updatedEvent != null && updatedEvent.has("id")) {
					eventId = updatedEvent.get("id").asText();
				} else {
					eventId = null;
				}
				putAttendeeEventId(agent, eventId);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (JSONRPCException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		} else {
			// event is updated
			Activity syncActivity = activity.clone();
			merge(syncActivity, event);
			context.put("activity", syncActivity);

			// TODO: cleanup
			try {
				logger.info("event is newer than activity. Updating activity. Activity="
						+ JOM.getInstance().writeValueAsString(syncActivity));
			} catch (Exception e) {
				e.printStackTrace();
			}

		}
	}

	/**
	 * Update the busy intervals of all attendees, and merge the results
	 */
	private void updateBusyIntervals() {
		Activity activity = getActivity();
		if (activity != null) {
			Set<String> agents = getAttendeesAgents(activity);
			if (agents != null) {
				for (String agent : agents) {
					updateBusyInterval(agent);
				}
			}
		}
		mergeBusyIntervals();
	}

	/**
	 * Merge the busy intervals of all attendees
	 */
	private void mergeBusyIntervals() {
		// read the stored busy intervals of all attendees
		List<Interval> busy = new ArrayList<Interval>();
		Activity activity = getActivity();
		if (activity != null) {
			Set<String> agents = getAttendeesAgents(activity);
			if (agents != null) {
				for (String agent : agents) {
					List<Interval> attendeeBusy = getAttendeeBusy(agent);
					if (attendeeBusy != null) {
						busy.addAll(attendeeBusy);
					}
				}
			}
		}

		// add office hours profile
		// TODO: don't include (hardcoded) office hours here, should be handled
		// by a PersonalAgent
		DateTime timeMin = DateTime.now();
		DateTime timeMax = timeMin.plusDays(LOOK_AHEAD_DAYS);
		List<Interval> officeHours = IntervalsUtil.getOfficeHours(timeMin,
				timeMax);
		List<Interval> outSideOfficeHours = IntervalsUtil.inverse(officeHours,
				timeMin, timeMax);
		busy.addAll(outSideOfficeHours);

		// merge the busy intervals of all attendees in one list
		List<Interval> mergedBusy = IntervalsUtil.merge(busy);

		// store the list
		getContext().put("busy", mergedBusy);
	}

	/**
	 * get the stored busy interval of an agent TODO: remove method getBusy,
	 * temporary
	 */
	@SuppressWarnings("unchecked")
	public ArrayNode getBusy() {
		List<Interval> busy = (List<Interval>) getContext().get("busy");

		// convert to JSON array
		ArrayNode array = JOM.createArrayNode();
		for (Interval interval : busy) {
			ObjectNode obj = JOM.createObjectNode();
			obj.put("start", interval.getStart().toString());
			obj.put("end", interval.getEnd().toString());
			array.add(obj);
		}
		return array;
	}

	/**
	 * Retrieve the busy intervals of a calendar agent
	 * 
	 * @param agent
	 */
	private void updateBusyInterval(@Name("agent") String agent) {
		try {
			// create parameters with the boundaries of the interval to be
			// retrieved
			ObjectNode params = JOM.createObjectNode();
			DateTime timeMin = DateTime.now();
			DateTime timeMax = timeMin.plusDays(LOOK_AHEAD_DAYS);
			params.put("timeMin", timeMin.toString());
			params.put("timeMax", timeMax.toString());

			// exclude the event managed by this agent from the busy intervals
			String eventId = getAttendeeEventId(agent);
			if (eventId != null) {
				ArrayNode excludeEventIds = JOM.createArrayNode();
				excludeEventIds.add(eventId);
				params.put("excludeEventIds", excludeEventIds);
			}

			// get the busy intervals from the agent
			ArrayNode array = send(agent, "getBusy", params, ArrayNode.class);

			// convert from ArrayNode to List
			List<Interval> busy = new ArrayList<Interval>();
			for (int i = 0; i < array.size(); i++) {
				ObjectNode obj = (ObjectNode) array.get(i);
				String start = obj.has("start") ? obj.get("start").asText()
						: null;
				String end = obj.has("end") ? obj.get("end").asText() : null;
				busy.add(new Interval(new DateTime(start), new DateTime(end)));
			}

			// store the interval in the context
			putAttendeeBusy(agent, busy);

		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (JSONRPCException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	/**
	 * Clear the stored activity, and remove events from attendees.
	 */
	@Override
	public void clear() throws Exception {
		Activity activity = getActivity();

		if (activity != null) {
			List<Attendee> attendees = activity.getConstraints().getAttendees();
			for (Attendee attendee : attendees) {
				String agent = attendee.getAgent();
				if (agent != null) {
					clearAttendee(agent);
				}
			}
		}

		// stop auto update timer (if any)
		stopAutoUpdate();

		// super class will clear the context
		super.clear();
	}

	/**
	 * Clear an event from given agent
	 * 
	 * @param agent
	 */
	private void clearAttendee(@Name("agent") String agent) {
		Context context = getContext();
		CalendarAgentData data = getAttendeeData(agent);
		if (data != null) {
			try {
				if (data.eventId != null) {
					ObjectNode params = JOM.createObjectNode();
					params.put("eventId", data.eventId);
					send(agent, "deleteEvent", params);
				}

				removeAttendeeData(agent);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (JSONRPCException e) {
				if (e.getCode() == 404) {
					// event was already deleted. fine!
					context.remove(agent);
				}

				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}

	// TODO: cleanup this temporary method
	public ArrayNode getOfficeHours(@Name("timeMin") String timeMin,
			@Name("timeMax") String timeMax) {
		List<Interval> available = IntervalsUtil.getOfficeHours(new DateTime(
				timeMin), new DateTime(timeMax));

		// convert to JSON array
		ArrayNode array = JOM.createArrayNode();
		for (Interval interval : available) {
			ObjectNode obj = JOM.createObjectNode();
			obj.put("start", interval.getStart().toString());
			obj.put("end", interval.getEnd().toString());
			array.add(obj);
		}
		return array;
	}

	@Override
	public String getDescription() {
		return "A MeetingAgent can dynamically plan and manage a meeting.";
	}

	@Override
	public String getVersion() {
		return "0.1";
	}
}
