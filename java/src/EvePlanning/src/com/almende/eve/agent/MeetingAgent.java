/**
 * @file MeetingAgent.java
 * 
 * @brief 
 * The MeetingAgent can dynamically manage a meeting with multiple attendees.
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
 * @date	  2012-05-31
 */

package com.almende.eve.agent;

import java.io.IOException;
import java.util.List;
import java.util.logging.Logger;

import org.joda.time.DateTime;

import com.almende.eve.context.Context;
import com.almende.eve.entity.activity.Activity;
import com.almende.eve.entity.activity.Attendee;
import com.almende.eve.json.JSONRPCException;
import com.almende.eve.json.annotation.Name;
import com.almende.eve.json.annotation.Required;
import com.almende.eve.json.jackson.JOM;
import com.fasterxml.jackson.databind.node.ObjectNode;

public class MeetingAgent extends Agent {
	private Logger logger = Logger.getLogger(this.getClass().getName());
	
	/**
	 * Set the event for this meeting agent
	 * @param summary   Description for the meeting
	 * @param location
	 * @param duration  Duration in minutes
	 * @param agents    List with calendar agent urls of the attendees
	 */
	public void setEvent(
			@Name("summary") String summary,
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
	
	// TODO: create a method clear
	
	/**
	 * update the activity for meeting agent
	 * @param activity
	 */
	public Activity updateActivity(@Name("activity") Activity updatedActivity) {
		Activity activity = getContext().get("activity");
		if (activity == null) {
			activity = new Activity();
		}

		activity = Activity.sync(activity, updatedActivity);

		// create duration when missing
		Integer duration = activity.getConstraints().getTime().getDuration();
		if (duration == null) {
			duration = 60; // minutes
			activity.getConstraints().getTime().setDuration(duration);
		}
		
		// create start when missing
		DateTime start;
		if (activity.getStatus().getStart() != null) {
			start = new DateTime(activity.getStatus().getStart());
		}
		else {
			start = DateTime.now();
			start = start.plusHours(1);
			start = start.minusMinutes(start.getMinuteOfHour());
			start = start.minusSeconds(start.getSecondOfMinute());
			start = start.minusMillis(start.getMillisOfSecond());

			activity.getStatus().setStart(start.toString());
		}
		
		// create end when missing
		DateTime end;
		if (activity.getStatus().getEnd() != null) {
			end = new DateTime(activity.getStatus().getEnd());
			
			// TODO: change end to match fixed duration?
		}
		else {
			end = start.plusMinutes(duration);
			activity.getStatus().setEnd(end.toString());
		}
		
		getContext().put("activity", activity);
		
		// update all attendees
		update();
		
		return getContext().get("activity");
	}

	/**
	 * Get meeting summary
	 * @return
	 */
	public String getSummary() {
		Activity activity = getContext().get("activity");
		return (activity != null) ? activity.getSummary() : null;
	}

	/**
	 * get meeting activity
	 * @return
	 */
	public Activity getActivity() {
		return getContext().get("activity");
	}
	
	/**
	 * update and synchronize the meeting
	 */
	public void update() {
		logger.info("update started");
		Activity activity = getContext().get("activity");
		
		for (Attendee attendee : activity.getConstraints().getAttendees()) {
			String agent = attendee.getAgent();
			syncEvent(agent);
		}
	}
	
	/**
	 * Merge an event into an Activity
	 * @param activity
	 * @param event
	 */
	private void merge(Activity activity, ObjectNode event) {
		String summary = null;
		if (event.has("summary")) {
			summary = event.get("summary").asText();
		}
		activity.setSummary(summary);

		String updated = null;
		if (event.has("updated")) {
			updated = event.get("updated").asText();
		}		
		activity.getStatus().setUpdated(updated);

		String start = null;
		if (event.with("start").has("dateTime")) {
			start = event.with("start").get("dateTime").asText();
		}
		activity.getStatus().setStart(start);

		String end = null;
		if (event.with("end").has("dateTime")) {
			end = event.with("end").get("dateTime").asText();
		}
		activity.getStatus().setEnd(end);

		String location = null;
		if (event.has("location")) {
			location = event.get("location").asText();
		}
		activity.getConstraints().getLocation().setSummary(location);
	}
	
	/**
	 * Merge an activity into an event
	 * @param event
	 * @param activity
	 */
	private void merge(ObjectNode event, Activity activity) {
		event.put("summary", activity.getSummary());
		event.put("updated", activity.getStatus().getUpdated());
		event.put("location", activity.getConstraints().getLocation().getSummary());

		event.with("start").put("dateTime", activity.getStatus().getStart());
		event.with("end").put("dateTime", activity.getStatus().getEnd());
	}

	/**
	 * Synchronize the event with given calendar agent
	 * @param agent
	 */
	public void syncEvent (@Name("agent") String agent) {
		logger.info("updateEvent started for agent " + agent);

		Context context = getContext();
		Activity activity = context.get("activity");
		
		// retrieve event
		ObjectNode event = null;
		String eventId = context.get(agent);
		if (eventId != null) {
				ObjectNode params = JOM.createObjectNode();
				params.put("eventId", eventId);
				try {
					event = send(agent, "getEvent", params, ObjectNode.class);
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (JSONRPCException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
					
					// TODO: distinguish between deleted event and other errors
					//       in case of error other than deleted event
					//       this sync action must be cancelled
				}
		}

		if (event == null) {
			event = JOM.createObjectNode();
		}
		Activity eventActivity = new Activity();
		merge(eventActivity, event);

		if (activity.isAfter(eventActivity)) {
			// activity is updated (event is outdated or not yet existing)
			
			// merge the activity into the event
			merge(event, activity);

			// save the event
			ObjectNode params = JOM.createObjectNode();
			params.put("event", event);
			try {
				String method = event.has("id") ? "updateEvent": "createEvent";
				ObjectNode updatedEvent = send(agent, method, params, ObjectNode.class);
				
				// store new eventId
				// TODO: only needed in case of creation?
				if (updatedEvent != null && updatedEvent.has("id")) {
					eventId = updatedEvent.get("id").asText();
				}
				else {
					eventId = null;
				}
				context.put(agent, eventId);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (JSONRPCException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}					
		}
		else {
			// event is updated
			Activity syncActivity = activity.clone();
			merge(syncActivity, event);
			context.put("activity", syncActivity);
		}
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
