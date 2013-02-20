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
 * @date   2012-08-09
 */

package com.almende.eve.agent;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.logging.Logger;

import org.joda.time.DateTime;
import org.joda.time.Duration;
import org.joda.time.Interval;
import org.joda.time.MutableDateTime;

import com.almende.eve.agent.annotation.Access;
import com.almende.eve.agent.annotation.AccessType;
import com.almende.eve.agent.annotation.Name;
import com.almende.eve.agent.annotation.Required;
import com.almende.eve.entity.Issue;
import com.almende.eve.entity.Issue.TYPE;
import com.almende.eve.entity.Weight;
import com.almende.eve.entity.activity.Activity;
import com.almende.eve.entity.activity.Attendee;
import com.almende.eve.entity.activity.Attendee.RESPONSE_STATUS;
import com.almende.eve.entity.activity.Preference;
import com.almende.eve.entity.activity.Status;
import com.almende.eve.entity.calendar.AgentData;
import com.almende.eve.rpc.jsonrpc.JSONRPCException;
import com.almende.eve.rpc.jsonrpc.JSONRequest;
import com.almende.eve.rpc.jsonrpc.jackson.JOM;
import com.almende.eve.state.State;
import com.almende.util.IntervalsUtil;
import com.almende.util.WeightsUtil;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

public class MeetingAgent extends Agent {
	private Logger logger = Logger.getLogger(this.getClass().getName());
	private final int LOOK_AHEAD_DAYS = 7; // number of days to look ahead when
										     // planning a meeting
	private final Double WEIGHT_BUSY_OPTIONAL_ATTENDEE = -1.0;
	private final Double WEIGHT_OFFICE_HOURS = 10.0;
	private final Double WEIGHT_PREFERRED_INTERVAL = 0.1;
	// private final Double WEIGHT_UNDESIRED_INTERVAL = -0.1;
	private final Double WEIGHT_DELAY_PER_DAY = -0.1;

	/**
	 * Convenience method to quickly set a new activity. 
	 * Currently stored activity will be removed.
	 * 
	 * @param summary
	 *            Description for the meeting
	 * @param location
	 * @param duration
	 *            Duration in minutes
	 * @param agents
	 *            List with calendar agent urls of the attendees
	 */
	public void setActivityQuick(@Name("summary") String summary,
			@Required(false) @Name("location") String location,
			@Name("duration") Integer duration,
			@Name("agents") List<String> agents) {
		Activity activity = new Activity();
		activity.setSummary(summary);
		activity.withConstraints().withLocation().setSummary(location);
		for (String agent : agents) {
			Attendee attendee = new Attendee();
			attendee.setAgent(agent);
			activity.withConstraints().withAttendees().add(attendee);
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
	private Set<String> getAgents(Activity activity) {
		Set<String> agents = new TreeSet<String>();
		for (Attendee attendee : activity.withConstraints().withAttendees()) {
			String agent = attendee.getAgent();
			if (agent != null) {
				agents.add(agent);
			}
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
		Activity activity = (Activity) getState().get("activity");
		if (activity == null) {
			activity = new Activity();
		}

		Set<String> prevAttendees = getAgents(activity);

		// if no updated timestamp is provided, set the timestamp to now
		if (updatedActivity.withStatus().getUpdated() == null) {
			updatedActivity.withStatus().setUpdated(DateTime.now().toString());
		}

		// synchronize with the stored activity
		activity = Activity.sync(activity, updatedActivity);

		// ensure the url of the meeting agent is filled in
		String myUrl = getFirstUrl();
		activity.setAgent(myUrl);

		// create duration when missing
		Long duration = activity.withConstraints().withTime().getDuration();
		if (duration == null) {
			duration = Duration.standardHours(1).getMillis(); // 1 hour in ms
			activity.withConstraints().withTime().setDuration(duration);
		}

		// remove calendar events from removed attendees
		Set<String> currentAttendees = getAgents(activity);
		Set<String> removedAttendees = new TreeSet<String>(prevAttendees);
		removedAttendees.removeAll(currentAttendees);
		for (String attendee : removedAttendees) {
			clearAttendee(attendee);
		}

		getState().put("activity", activity);

		// update all attendees, start timer to regularly check
		update();

		return (Activity) getState().get("activity");
	}

	/**
	 * Get meeting summary
	 * 
	 * @return
	 */
	public String getSummary() {
		Activity activity = (Activity) getState().get("activity");
		return (activity != null) ? activity.getSummary() : null;
	}

	/**
	 * get meeting activity returns null if no activity has been initialized.
	 * 
	 * @return
	 */
	public Activity getActivity() {
		return (Activity) getState().get("activity");
	}

	/**
	 * Apply the constraints of the the activity (for example duration)
	 * 
	 * @param activity
	 * @return changed    Returns true if the activity is changed
	 */
	private boolean applyConstraints() {
		Activity activity = (Activity) getState().get("activity");
		boolean changed = false;
		if (activity == null) {
			return false;
		}

		// constraints on attendees/resources
		/* TODO: copy actual attendees to status.attendees
		List<Attendee> constraintsAttendees = activity.withConstraints().withAttendees();
		List<Attendee> attendees = new ArrayList<Attendee>();
		for (Attendee attendee : constraintsAttendees) {
			attendees.add(attendee.clone());
		}
		activity.withStatus().setAttendees(attendees);
		// TODO: is it needed to check if the attendees are changed?
		*/
		
		// check time constraints
		Long duration = activity.withConstraints().withTime().getDuration();
		if (duration != null) {
			String start = activity.withStatus().getStart();
			String end = activity.withStatus().getEnd();
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
					activity.withStatus().setEnd(endTime.toString());
					activity.withStatus().setUpdated(DateTime.now().toString());

					changed = true;
				}
			}
		}
		
		// location constraints
		String newLocation = activity.withConstraints().withLocation().getSummary();
		String oldLocation = activity.withStatus().withLocation().getSummary();
		if (newLocation != null && !newLocation.equals(oldLocation)) {
			activity.withStatus().withLocation().setSummary(newLocation);
			changed = true;
		}
		
		if (changed) {
			// store the updated activity
			getState().put("activity", activity);
		}
		return changed;
	}

	/**
	 * synchronize the meeting in all attendees calendars
	 */
	private boolean syncEvents() {
		logger.info("syncEvents started");
		Activity activity = (Activity) getState().get("activity");

		boolean changed = false;
		if (activity != null) {
			String updatedBefore = activity.withStatus().getUpdated();

			for (Attendee attendee : activity.withConstraints().withAttendees()) {
				String agent = attendee.getAgent();
				if (agent != null) {
					if (attendee.getResponseStatus() != RESPONSE_STATUS.declined) {
						syncEvent(agent);
					}
					else {
						clearAttendee(agent);
					}
				}
			}

			activity = (Activity) getState().get("activity");
			String updatedAfter = activity.withStatus().getUpdated();

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
		logger.info("update started");

		// stop running tasks
		stopAutoUpdate();
		
		clearIssues();
		
		// synchronize the events
		boolean changedEvent = syncEvents();
		if (changedEvent) {
			syncEvents();
		}

		// Check if the activity is finished
		// If not, schedule a new update task. Else we are done
		Activity activity = getActivity();
		String start = 
				(activity != null) ? activity.withStatus().getStart() : null;
		String updated = 
				(activity != null) ? activity.withStatus().getUpdated() : null;
		boolean isFinished = false;
		if (start != null && (new DateTime(start)).isBefore(DateTime.now())) {
			// start of the event is in the past
			isFinished = true;
			if (updated != null && (new DateTime(updated)).isAfter(new DateTime(start))) {
				// if changed after the last planned start time, then it is 
				// updated afterwards, so do not mark as finished
				isFinished = false;
			}
		}
		if (activity != null && !isFinished) {
			// not yet finished. Reschedule the activity
			updateBusyIntervals();

			boolean changedConstraints = applyConstraints();
			boolean rescheduled = scheduleActivity();
			if (changedConstraints || rescheduled) {
				changedEvent = syncEvents();
				if (changedEvent) {
					syncEvents();
				}
			}
			
			// TODO: not so nice adjusting the activityStatus here this way
			if (activity.withStatus().getActivityStatus() != Status.ACTIVITY_STATUS.error) {
				// store status of a activity as "planned"
				activity.withStatus().setActivityStatus(Status.ACTIVITY_STATUS.planned);
				getState().put("activity", activity);
			}

			startAutoUpdate();
		} else {
			// store status of a activity as "executed"
			activity.withStatus().setActivityStatus(Status.ACTIVITY_STATUS.executed);
			getState().put("activity", activity);

			logger.info("The activity is over, my work is done. Goodbye world.");
		}
	}

	/**
	 * Get the timestamp rounded to the next half hour
	 * @return
	 */
	private DateTime getNextHalfHour() {
		DateTime next = DateTime.now();
		next = next.minusMillis(next.getMillisOfSecond());
		next = next.minusSeconds(next.getSecondOfMinute());

		if (next.getMinuteOfHour() > 30) {
			next = next.minusMinutes(next.getMinuteOfHour());
			next = next.plusMinutes(60);
		} else {
			next = next.minusMinutes(next.getMinuteOfHour());
			next = next.plusMinutes(30);
		}

		return next;
	}

	/**
	 * Schedule the meeting based on currently known event status, infeasible
	 * intervals, and preferences
	 * 
	 * @return rescheduled Returns true if the activity has been rescheduled
	 *         When rescheduled, events must be synchronized again with
	 *         syncEvents.
	 */
	private boolean scheduleActivity() {
		logger.info("scheduleActivity started"); // TODO: cleanup
		State state = getState();
		Activity activity = (Activity) state.get("activity");
		if (activity == null) {
			return false;
		}
		
		// read planned start and end from the activity
		DateTime activityStart = null;
		if (activity.withStatus().getStart() != null) {
			activityStart = new DateTime(activity.withStatus().getStart());
		}
		DateTime activityEnd = null;
		if (activity.withStatus().getEnd() != null) {
			activityEnd = new DateTime(activity.withStatus().getEnd());
		}
		Interval activityInterval = null;
		if (activityStart != null && activityEnd != null) {
			activityInterval = new Interval(activityStart, activityEnd);
		}

		// calculate solutions
		List<Weight> solutions = calculateSolutions();
		if (solutions.size() > 0) {
			// there are solutions. yippie!
			Weight solution = solutions.get(0);
			if (activityInterval == null || 
					!solution.getInterval().equals(activityInterval)) {
				// interval is changed, save new interval 
				Status status = activity.withStatus();
				status.setStart(solution.getStart().toString());
				status.setEnd(solution.getEnd().toString());
				status.setActivityStatus(Status.ACTIVITY_STATUS.planned);
				status.setUpdated(DateTime.now().toString());
				state.put("activity", activity);
				logger.info("Activity replanned at " + solution.toString()); // TODO: cleanup logging
				return true;
			}
			else {
				// planning did not change. nothing to do.
			}
		}
		else {
			if (activityStart != null || activityEnd != null) {
				// no solution
				Issue issue = new Issue();
				issue.setCode(Issue.NO_PLANNING);
				issue.setType(Issue.TYPE.error);
				issue.setMessage("No free interval found for the meeting");
				issue.setTimestamp(DateTime.now().toString());
				// TODO: generate hints
				addIssue(issue);
	
				Status status = activity.withStatus();
				status.setStart(null);
				status.setEnd(null);
				status.setActivityStatus(Status.ACTIVITY_STATUS.error);
				status.setUpdated(DateTime.now().toString());
				state.put("activity", activity);
				logger.info(issue.getMessage()); // TODO: cleanup logging
				return true;
			}
			else {
				// planning did not change (no solution was already the case)
			}
		}
		
		return false;
	}

	/**
	 * Calculate all feasible intervals with their preference weight, based on
	 * the event status, stored infeasible intervals, and preferred intervals.
	 * If there are no solutions, an empty array is returned.
	 * @return solutions
	 */
	@SuppressWarnings("unchecked")
	private List<Weight> calculateSolutions() {
		logger.info("calculateSolutions started"); // TODO: cleanup
		
		State state = getState();
		List<Weight> solutions = new ArrayList<Weight>();
		
		// get the activity
		Activity activity = (Activity) state.get("activity");
		if (activity == null) {
			return solutions;
		}
		
		// get infeasible intervals
		List<Interval> infeasible = (List<Interval>) state.get("infeasible");
		if (infeasible == null) {
			infeasible = new ArrayList<Interval>();
		}
		
		// get preferred intervals
		List<Weight> preferred = (List<Weight>) state.get("preferred");
		if (preferred == null) {
			preferred = new ArrayList<Weight>();
		}
		
		// get the duration of the activity
		Long durationLong = activity.withConstraints().withTime().getDuration();
		Duration duration = null;
		if (durationLong != null) {
			duration = new Duration(durationLong);
		} else {
			// TODO: give error when duration is not defined?
			duration = Duration.standardHours(1);
		}
		
		// check interval at next half hour
		DateTime firstTimeslot = getNextHalfHour();
		Interval test = new Interval(firstTimeslot, firstTimeslot.plus(duration));
		testInterval(infeasible, preferred, test, solutions);
		
		// loop over all infeasible intervals
		for (Interval i : infeasible) {
			// test timeslot left from the infeasible interval
			test = new Interval(i.getStart().minus(duration), i.getStart());
			testInterval(infeasible, preferred, test, solutions);
			
			// test timeslot right from the infeasible interval
			test = new Interval(i.getEnd(), i.getEnd().plus(duration));
			testInterval(infeasible, preferred, test, solutions);
		}

		// loop over all preferred intervals
		for (Weight w : preferred) {
			// test timeslot left from the start of the preferred interval
			test = new Interval(w.getStart().minus(duration), w.getStart());
			testInterval(infeasible, preferred, test, solutions);

			// test timeslot right from the start of the preferred interval
			test = new Interval(w.getStart(), w.getStart().plus(duration));
			testInterval(infeasible, preferred, test, solutions);
			
			// test timeslot left from the end of the preferred interval
			test = new Interval(w.getEnd().minus(duration), w.getEnd());
			testInterval(infeasible, preferred, test, solutions);
			
			// test timeslot right from the end of the preferred interval
			test = new Interval(w.getEnd(), w.getEnd().plus(duration));
			testInterval(infeasible, preferred, test, solutions);
		}
		
		// order the calculated feasible timeslots by weight, from highest to
		// lowest. In case of equals weights, the timeslots are ordered by 
		// start date
		class WeightComparator implements Comparator<Weight> {
			@Override
			public int compare(Weight a, Weight b) {
				if (a.getWeight() != null && b.getWeight() != null) {
					int cmp = Double.compare(a.getWeight(), b.getWeight());
					if (cmp == 0) {
						return a.getStart().compareTo(b.getStart());
					}
					else {
						return -cmp;
					}
				}
				return 0;
			}
		}
		WeightComparator comparator = new WeightComparator();
		Collections.sort(solutions, comparator);

		// remove duplicates
		int i = 1;
		while (i < solutions.size()) {
			if (solutions.get(i).equals(solutions.get(i - 1))) {
				solutions.remove(i);
			}
			else {
				i++;
			}
		}
		
		return solutions;
	}
	
	/**
	 * Test if given interval is feasible. If so, calculate the preference 
	 * weight and add it to the provided array with solutions
	 * @param infeasible
	 * @param preferred
	 * @param test
	 * @param solutions
	 */
	private void testInterval(final List<Interval> infeasible, 
			final List<Weight> preferred, final Interval test, 
			List<Weight> solutions) {
		boolean feasible = calculateFeasible(infeasible, test);
		if (feasible) {
			double weight = calculatePreference(preferred, test);
			solutions.add(new Weight(test, weight));
		}
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
		State state = getState();
		Activity activity = getActivity();
		
		// determine the interval (1 hour by default)
		long TEN_SECONDS = 10 * 1000;
		long ONE_HOUR = 60 * 60 * 1000;
		long interval = ONE_HOUR; // default is 1 hour
		if (activity != null) {
			String updated = activity.withStatus().getUpdated();
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
		String task = getScheduler().createTask(request, interval);
		state.put("updateTask", task);

		logger.info("Auto update started. Interval = " + interval
				+ " milliseconds");
	}

	/**
	 * Stop automatic updating
	 */
	public void stopAutoUpdate() {
		State state = getState();

		String task = (String) state.get("updateTask");
		if (task != null) {
			getScheduler().cancelTask(task);
			state.remove("updateTask");
		}

		logger.info("Auto update stopped");
	}

	/**
	 * Convert a calendar event into an activity
	 * @param event
	 * @return activity
	 */
	private Activity convertEventToActivity(ObjectNode event) {
		Activity activity = new Activity();
		
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

		// description
		String description = null;
		if (event.has("description")) {
			description = event.get("description").asText();
		}
		activity.setDescription(description);
		
		// updated
		String updated = null;
		if (event.has("updated")) {
			updated = event.get("updated").asText();
		}
		activity.withStatus().setUpdated(updated);

		// start
		String start = null;
		if (event.with("start").has("dateTime")) {
			start = event.with("start").get("dateTime").asText();
		}
		activity.withStatus().setStart(start);

		// end
		String end = null;
		if (event.with("end").has("dateTime")) {
			end = event.with("end").get("dateTime").asText();
		}
		activity.withStatus().setEnd(end);

		// duration
		if (start != null && end != null) {
			Interval interval = new Interval(new DateTime(start), new DateTime(
					end));
			Long duration = interval.toDurationMillis();
			activity.withConstraints().withTime().setDuration(duration);
		}

		// location
		String location = null;
		if (event.has("location")) {
			location = event.get("location").asText();
		}
		activity.withConstraints().withLocation().setSummary(location);
		
		return activity;
	}

	/**
	 * Merge an activity into an event
	 * All fields that are in the event will be left as they are
	 * @param event
	 * @param activity
	 */
	private void mergeActivityIntoEvent(ObjectNode event, Activity activity) {
		// merge static information
		event.put("agent", activity.getAgent());
		event.put("summary", activity.getSummary());
		event.put("description", activity.getDescription());
		
		/// merge status information
		Status status = activity.withStatus();
		event.put("updated", status.getUpdated());
		event.with("start").put("dateTime", status.getStart());
		event.with("end").put("dateTime", status.getEnd());
		event.put("location", status.withLocation().getSummary());
	}

	/**
	 * Retrieve all current issues. If there are no issues, an empty array
	 * is returned
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public List<Issue> getIssues() {
		List<Issue> issues = (List<Issue>) getState().get("issues");
		if (issues == null) {
			issues = new ArrayList<Issue>();
		}
		return issues;
	}

	/**
	 * Remove all issues
	 */
	private void clearIssues() {
		getState().remove("issues");
	}
	
	/**
	 * Add an issue to the issue list
	 * The issue will trigger an event
	 * @param issue
	 */
	private void addIssue(Issue issue) {
		List<Issue> issues = getIssues();
		issues.add(issue);
		getState().put("issues", issues);
		
		// trigger an error event
		try {
			String event = issue.getType().toString();
			ObjectNode data = JOM.createObjectNode();
			data.put("issue", JOM.getInstance().convertValue(issue, 
					ObjectNode.class));
			ObjectNode params = JOM.createObjectNode();
			params.put("description", issue.getMessage());
			params.put("data", data);
			trigger(event, params);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * Create an issue with type, code, and message
	 * timestamp will be set to NOW
	 * @param type
	 * @param code
	 * @param message
	 */
	private void addIssue (TYPE type, Integer code, String message) {
		Issue issue = new Issue();
		issue.setType(type);
		issue.setCode(code);
		issue.setMessage(message);
		issue.setTimestamp(DateTime.now().toString());
		addIssue(issue);
	}
	
	/**
	 * Retrieve the data of a single calendar agent from the state
	 * 
	 * @param agentUrl
	 * @return data returns the calendar data. If not available, a new, empty
	 *         CalendarAgentData is returned.
	 */
	// TODO: create some separate AgentData handling class, instead of methods in MeetingAgent
	@SuppressWarnings("unchecked")
	private AgentData getAgentData(String agentUrl) {
		Map<String, AgentData> calendarAgents = 
				(Map<String, AgentData>) getState().get("calendarAgents");

		if (calendarAgents != null && calendarAgents.containsKey(agentUrl)) {
			return calendarAgents.get(agentUrl);
		}
		return new AgentData();
	}

	/**
	 * Put data for a calendar agent into the state
	 * 
	 * @param agentUrl
	 * @param data
	 */
	@SuppressWarnings("unchecked")
	private void putAgentData(String agentUrl, AgentData data) {
		State state = getState();
		Map<String, AgentData> calendarAgents = 
				(Map<String, AgentData>) state.get("calendarAgents");
		if (calendarAgents == null) {
			calendarAgents = new HashMap<String, AgentData>();
		}

		calendarAgents.put(agentUrl, data);
		state.put("calendarAgents", calendarAgents);
	}

	/**
	 * Remove a calendar agent data from the state
	 * @param agent
	 * @param data
	 */
	@SuppressWarnings("unchecked")
	private void removeAgentData(String agent) {
		State state = getState();
		Map<String, AgentData> calendarAgents = (Map<String, AgentData>) state
				.get("calendarAgents");
		if (calendarAgents != null && calendarAgents.containsKey(agent)) {
			calendarAgents.remove(agent);
			state.put("calendarAgents", calendarAgents);
		}
	}

	/**
	 * Retrieve the busy intervals of a calendar agent from the state
	 * @param agent
	 * @return busy returns busy intervals, or null if not available
	 */
	private List<Interval> getAgentBusy(String agent) {
		AgentData data = getAgentData(agent);
		return data.busy;
	}

	/**
	 * Put the busy intervals for a calendar agent into the state
	 * @param agent
	 * @param busy
	 */
	private void putAgentBusy(String agent, List<Interval> busy) {
		AgentData data = getAgentData(agent);
		data.busy = busy;
		putAgentData(agent, data);
	}

	/**
	 * Retrieve calendar event from calendaragent
	 * @param agent
	 * @return event   Calendar event, or null if not found
	 */
	private ObjectNode getEvent (String agent) {
		ObjectNode event = null;
		String eventId = getAgentData(agent).eventId;
		if (eventId != null) {
			ObjectNode params = JOM.createObjectNode();
			params.put("eventId", eventId);
			try {
				event = send(agent, "getEvent", params, ObjectNode.class);
			} catch (JSONRPCException e) {
				if (e.getCode() == 404) {
					// event was deleted by the user.

					//e.printStackTrace();
					Activity activity = (Activity) getState().get("activity");
					Attendee attendee = activity.withConstraints().withAttendee(agent);
					attendee.setResponseStatus(RESPONSE_STATUS.declined);
					getState().put("activity", activity);
					
					clearAttendee(agent); // TODO: seems not to work
				} else {
					e.printStackTrace(); // TODO: remove print stacktrace
				}
			} catch (Exception e) {
				addIssue(TYPE.warning, Issue.EXCEPTION, e.getMessage());
				e.printStackTrace(); // TODO: remove print stacktrace
			}
		}
		return event;
	}

	// TODO: comment
	private boolean equalsDateTime(String a, String b) {
		if (a != null && b != null) {
			return new DateTime(a).equals(	new DateTime(b));
		}
		if (a == null && b == null) {
			return false;
		}
		
		return true;
	}
	
	/**
	 * Synchronize the event with given calendar agent
	 * 
	 * @param agent
	 */
	// TODO: the method syncEvent has grown to large. split it up
	private void syncEvent(@Name("agent") String agent) {
		logger.info("syncEvent started for agent " + agent);
		State state = getState();

		// retrieve event from calendar agent
		ObjectNode event = getEvent(agent);
		if (event == null) {
			event = JOM.createObjectNode();
		}
		Activity eventActivity = convertEventToActivity(event);
		
		// verify all kind of stuff
		Activity activity = (Activity) state.get("activity");
		if (activity == null) {
			return; // oops no activity at all
		}
		if (activity.withStatus().getStart() == null || 
				activity.withStatus().getEnd() == null) {
			return; // activity is not yet planned. cancel synchronization
		}
		Attendee attendee = activity.withConstraints().getAttendee(agent); 
		if (attendee == null) {
			return; // unknown attendee
		}
		if (attendee.getResponseStatus() == Attendee.RESPONSE_STATUS.declined) {
			// attendee does not want to attend
			clearAttendee(agent);
			return; 
		}
		
		// check if the activity or the retrieved event is changed since the
		// last synchronization
		AgentData agentData = getAgentData(agent);
		boolean activityChanged = !equalsDateTime(agentData.activityUpdated, 
				activity.withStatus().getUpdated());
		boolean eventChanged = !equalsDateTime(agentData.eventUpdated, 
				eventActivity.withStatus().getUpdated());
		boolean changed = activityChanged || eventChanged;

		if (changed && activity.isNewerThan(eventActivity)) {
			// activity is updated (event is out-dated or not yet existing)

			// TODO: cleanup logging
			try {
				logger.info("activity is newer than event. Updating event. Activity="
						+ JOM.getInstance().writeValueAsString(activity));
			} catch (Exception e) {
				e.printStackTrace();
			}

			// merge the activity into the event
			mergeActivityIntoEvent(event, activity);
			
			// TODO: if attendee cannot attend (=optional or declined), show this somehow in the event
			
			// save the event
			ObjectNode params = JOM.createObjectNode();
			params.put("event", event);
			try {
				// TODO: only update/create the event when the attendee
				// is not optional or is available at the planned time
				String method = event.has("id") ? "updateEvent" : "createEvent";
				ObjectNode updatedEvent = send(agent, method, params,
						ObjectNode.class);

				// update the agent data
				agentData.eventId = updatedEvent.get("id").asText();
				agentData.eventUpdated = updatedEvent.get("updated").asText();
				agentData.activityUpdated = activity.withStatus().getUpdated();
				putAgentData(agent, agentData);
			} catch (JSONRPCException e) {
				addIssue(TYPE.warning, Issue.JSONRPCEXCEPTION, e.getMessage());
				e.printStackTrace(); // TODO remove printing stacktrace
			} catch (Exception e) {
				addIssue(TYPE.warning, Issue.EXCEPTION, e.getMessage());
				e.printStackTrace(); // TODO remove printing stacktrace
			}
		} else if (changed) {
			// event is updated (activity is out-dated or both have the same 
			// updated timestamp)
			
			// if start is changed, add this as preferences to the constraints
			if (!equalsDateTime(activity.withStatus().getStart(),
					eventActivity.withStatus().getStart())) {
				/* TODO: store the old interval as undesired?
				String oldStart = activity.withStatus().getStart();
				String oldEnd = activity.withStatus().getEnd();
				if (oldStart != null && oldEnd != null) {
					Preference undesired = new Preference ();
					undesired.setStart(oldStart);
					undesired.setEnd(oldEnd);
					undesired.setWeight(WEIGHT_UNDESIRED_INTERVAL);
					activity.getConstraints().getTime().addPreference(undesired);	
				}
				*/
				
				// store the new interval as preferred
				String newStart = eventActivity.withStatus().getStart();
				String newEnd = eventActivity.withStatus().getEnd();
				if (newStart != null && newEnd != null) {
					Preference preferred = new Preference ();
					preferred.setStart(newStart);
					preferred.setEnd(newEnd);
					preferred.setWeight(WEIGHT_PREFERRED_INTERVAL);

					// overwrite other preferences with this new preference
					// TODO: all preferences are overwritten for now. Behavior should be changed.
					List<Preference> preferences = new ArrayList<Preference>();
					preferences.add(preferred);
					activity.getConstraints().getTime().setPreferences(preferences);				
					
					//activity.getConstraints().getTime().addPreference(preferred);
				}
			}
			else {
				// events are in sync, nothing to do
			}
			
			// update the activity
			activity.merge(eventActivity);
			state.put("activity", activity);

			// update the agent data
			agentData.eventId = event.get("id").asText();
			agentData.eventUpdated = event.get("updated").asText();
			agentData.activityUpdated = activity.withStatus().getUpdated();
			putAgentData(agent, agentData);

			// TODO: cleanup
			try {
				logger.info("event is newer than activity. Updating activity. Activity="
						+ JOM.getInstance().writeValueAsString(activity));
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		else {
			// activity and eventActivity have the same updated timestamp
			// nothing to do.
			logger.info("event and activity are in sync"); // TODO: cleanup
		}
	}

	/**
	 * Update the busy intervals of all attendees, and merge the results
	 */
	private void updateBusyIntervals() {
		Activity activity = getActivity();
		if (activity != null) {
			List<Attendee> attendees = activity.withConstraints().withAttendees();
			for (Attendee attendee : attendees) {
				String agent = attendee.getAgent();
				if (attendee.getResponseStatus() != RESPONSE_STATUS.declined) {
					updateBusyInterval(agent);
				}
			}
		}
		
		mergeTimeConstraints();
	}

	/**
	 * Merge the busy intervals of all attendees, and the preferred intervals
	 */
	private void mergeTimeConstraints() {
		List<Interval> infeasibleIntervals = new ArrayList<Interval>();
		List<Weight> preferredIntervals = new ArrayList<Weight>();

		Activity activity = getActivity();
		if (activity != null) {
			// read and merge the stored busy intervals of all attendees
			for (Attendee attendee : activity.withConstraints().withAttendees()) {
				String agent = attendee.getAgent();
				if (attendee.getResponseStatus() == RESPONSE_STATUS.declined) {
					// This attendee declined. 
					// Ignore this attendees busy interval
				}
				else if (new Boolean(true).equals(attendee.getOptional())) {
					// This attendee is optional. 
					// Add its busy intervals to the soft constraints
					List<Interval> attendeeBusy = getAgentBusy(agent);
					if (attendeeBusy != null) {
						for (Interval i : attendeeBusy) {
							Weight wi = new Weight(
									i.getStart(), i.getEnd(), 
									WEIGHT_BUSY_OPTIONAL_ATTENDEE);

							preferredIntervals.add(wi);
						}
					}					
				}
				else {
					// this attendee is required.
					// Add its busy intervals to the hard constraints
					List<Interval> attendeeBusy = getAgentBusy(agent);
					if (attendeeBusy != null) {
						infeasibleIntervals.addAll(attendeeBusy);
					}
				}				
			}

			// read the time preferences and add them to the soft constraints
			List<Preference> preferences = activity.withConstraints()
				.withTime().withPreferences();
			for (Preference p : preferences) {
				if (p != null) {
					Weight wi = new Weight(
							new DateTime(p.getStart()), 
							new DateTime(p.getEnd()), 
							p.getWeight());

					preferredIntervals.add(wi);
				}
			}
		}
		
		// add office hours profile to the soft constraints
		// TODO: don't include (hardcoded) office hours here, should be handled
		// by a PersonalAgent
		DateTime timeMin = DateTime.now();
		DateTime timeMax = timeMin.plusDays(LOOK_AHEAD_DAYS);
		List<Interval> officeHours = IntervalsUtil.getOfficeHours(timeMin,
				timeMax);
		for (Interval i : officeHours) {
			Weight wi = new Weight(i, WEIGHT_OFFICE_HOURS);
			preferredIntervals.add(wi);
		}
		
		// add delay penalties to the soft constraints
		DateTime now = DateTime.now();
		MutableDateTime d = new MutableDateTime(now.getYear(), 
				now.getMonthOfYear(), now.getDayOfMonth(), 0, 0, 0, 0);
		for (int i = 0; i <= LOOK_AHEAD_DAYS; i++) {
			DateTime start = d.toDateTime();
			DateTime end = start.plusDays(1);
			Weight wi = new Weight(start, end, 
					WEIGHT_DELAY_PER_DAY * i);
			preferredIntervals.add(wi);
			d.addDays(1);
		}

		// order and store the aggregated lists with intervals
		IntervalsUtil.order(infeasibleIntervals);
		getState().put("infeasible", infeasibleIntervals);
		WeightsUtil.order(preferredIntervals);
		getState().put("preferred", preferredIntervals);
	}
	
	/**
	 * Calculate the average preference for given interval.
	 * The method aggregates over all stored preferences
	 * Default preference is 0.
	 * @param preferredIntervals   list with intervals ordered by start
	 * @param test                 test interval
	 * @return preference
	 */
	private double calculatePreference(
			List<Weight> preferredIntervals, Interval test) {
		double preference = 0;

		for (Weight interval : preferredIntervals) {
			Interval overlap = test.overlap(interval.getInterval());
			if (overlap != null) {
				Double weight = interval.getWeight();
				if (weight != null) {
					double durationCheck = test.toDurationMillis();
					double durationOverlap = overlap.toDurationMillis();
					double avgWeight = (durationOverlap / durationCheck) * weight;
					preference += avgWeight;
				}
			}

			if (interval.getStart().isAfter(test.getEnd())) {
				// as the list is ordered, we can exit as soon as we have an
				// interval which starts after the wanted interval.
				break;
			}
		}
		
		return preference;
	}
			
	/**
	 * Calculate whether given interval is feasible (i.e. does not overlap with
	 * any of the infeasible intervals, and is not in the past)
	 * @param infeasibleIntervals   list with intervals ordered by start
	 * @param timeMin
	 * @param timeMax
	 * @return feasible
	 */
	private boolean calculateFeasible(List<Interval> infeasibleIntervals,
			Interval test) {
		if (test.getStart().isBeforeNow()) {
			// interval starts in the past
			return false;
		}
		
		for (Interval interval : infeasibleIntervals) {
			if (test.overlaps(interval)) {
				return false;
			}
			if (interval.getStart().isAfter(test.getEnd())) {
				// as the list is ordered, we can exit as soon as we have an
				// interval which starts after the wanted interval.
				break;
			}
		}
		
		return true; 
	}
	
	/**
	 * Retrieve the feasible and preferred intervals
	 * @return
	 */
	// TODO: remove this temporary method
	@SuppressWarnings("unchecked")
	public ObjectNode getIntervals() {
		ObjectNode intervals = JOM.createObjectNode();

		List<Interval> infeasible = (List<Interval>) getState().get("infeasible");
		List<Weight> preferred = (List<Weight>) getState().get("preferred");
		List<Weight> solutions = calculateSolutions(); 

		// merge the intervals
		List<Interval> mergedInfeasible = null;
		List<Weight> mergedPreferred = null;
		if (infeasible != null) {
			mergedInfeasible = IntervalsUtil.merge(infeasible);
		}
		if (preferred != null) {
			mergedPreferred = WeightsUtil.merge(preferred);
		}

		if (infeasible != null) {
			ArrayNode arr = JOM.createArrayNode();
			for (Interval interval : infeasible) {
				ObjectNode o = JOM.createObjectNode();
				o.put("start", interval.getStart().toString());
				o.put("end", interval.getEnd().toString());
				arr.add(o);
			}
			intervals.put("infeasible", arr);
		}

		if (preferred != null) {
			ArrayNode arr = JOM.createArrayNode();
			for (Weight weight : preferred) {
				ObjectNode o = JOM.createObjectNode();
				o.put("start", weight.getStart().toString());
				o.put("end", weight.getEnd().toString());
				o.put("weight", weight.getWeight());
				arr.add(o);
			}
			intervals.put("preferred", arr);
		}

		if (solutions != null) {
			ArrayNode arr = JOM.createArrayNode();
			for (Weight weight : solutions) {
				ObjectNode o = JOM.createObjectNode();
				o.put("start", weight.getStart().toString());
				o.put("end", weight.getEnd().toString());
				o.put("weight", weight.getWeight());
				arr.add(o);
			}
			intervals.put("solutions", arr);
		}

		if (mergedInfeasible != null) {
			ArrayNode arr = JOM.createArrayNode();
			for (Interval i : mergedInfeasible) {
				ObjectNode o = JOM.createObjectNode();
				o.put("start", i.getStart().toString());
				o.put("end", i.getEnd().toString());
				arr.add(o);
			}
			intervals.put("mergedInfeasible", arr);
		}

		if (mergedPreferred != null) {
			ArrayNode arr = JOM.createArrayNode();
			for (Weight wi : mergedPreferred) {
				ObjectNode o = JOM.createObjectNode();
				o.put("start", wi.getStart().toString());
				o.put("end", wi.getEnd().toString());
				o.put("weight", wi.getWeight());
				arr.add(o);
			}
			intervals.put("mergedPreferred", arr);
		}		

		return intervals;		
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
			String eventId = getAgentData(agent).eventId;
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

			// store the interval in the state
			putAgentBusy(agent, busy);

		} catch (JSONRPCException e) {
			addIssue(TYPE.warning, Issue.JSONRPCEXCEPTION, e.getMessage());
			e.printStackTrace(); // TODO remove printing stacktrace
		} catch (Exception e) {
			addIssue(TYPE.warning, Issue.EXCEPTION, e.getMessage());
			e.printStackTrace(); // TODO remove printing stacktrace
		}
	}

	/**
	 * Delete everything of the agent
	 */
	@Override
	public void delete() {
		clear(); 
		
		// super class will delete the state
		super.delete();
	}

	/**
	 * Clear the stored activity, and remove events from attendees.
	 */
	@Access(AccessType.UNAVAILABLE)
	public void clear () {
		Activity activity = getActivity();

		if (activity != null) {
			List<Attendee> attendees = activity.withConstraints().withAttendees();
			for (Attendee attendee : attendees) {
				String agent = attendee.getAgent();
				if (agent != null) {
					clearAttendee(agent);
				}
			}
		}

		// stop auto update timer (if any)
		stopAutoUpdate();
	}
	
	/**
	 * Clear an event from given agent
	 * 
	 * @param agent
	 */
	private void clearAttendee(@Name("agent") String agent) {
		AgentData data = getAgentData(agent);
		if (data != null) {
			try {
				if (data.eventId != null) {
					ObjectNode params = JOM.createObjectNode();
					params.put("eventId", data.eventId);
					send(agent, "deleteEvent", params);
					data.eventId = null;
				}
			} catch (JSONRPCException e) {
				if (e.getCode() == 404) {
					// event was already deleted. fine!
					data.eventId = null;
				}
				else {
					e.printStackTrace();
				}
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

			if (data.eventId == null) {
				removeAgentData(agent);
				logger.info("clearAttendee " + agent + " cleared");
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

	/**
	 * Get the first url of the agents urls. Returns null if the agent does not
	 * have any urls.
	 * @return firstUrl
	 */
	private String getFirstUrl() {
		List<String> urls = getUrls();
		if (urls.size() > 0) {
			return urls.get(0);
		}
		return null;
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
