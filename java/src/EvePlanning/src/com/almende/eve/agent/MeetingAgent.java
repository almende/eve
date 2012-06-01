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
import com.almende.eve.entity.activity.Status;
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
	 * Synchronize the event with given calendar agent
	 * @param agent
	 */
	public void syncEvent (@Name("agent") String agent) {
		logger.info("updateEvent started for agent " + agent);

		Context context = getContext();
		Activity activity = context.get("activity");
		String summary = activity.getSummary();
		Status status = activity.getStatus();
		DateTime start = (status != null) ? new DateTime(status.getStart()) : null;
		DateTime end = (status != null) ? new DateTime(status.getEnd()) : null;
		DateTime updated = (status != null && status.getUpdated() != null) ? 
				new DateTime(status.getUpdated()) : null;

		// retrieve event
		ObjectNode event = null;
		DateTime eventUpdated = null;
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

		// get the datetime when the event is updated
		if (event != null && event.has("updated")) {
			eventUpdated = new DateTime(event.get("updated").asText());
		}

		// update the meeting agents data when event is newer
		if (eventUpdated != null && (updated == null || eventUpdated.isAfter(updated))) {
			// read new data
			updated = new DateTime(eventUpdated);
			if (event.has("summary")) {
				summary = event.get("summary").asText();
			}
			if (event.with("start").has("dateTime")) {
				start = new DateTime(event.with("start").get("dateTime").asText());
			}
			if (event.with("end").has("dateTime")) {
				end = new DateTime(event.with("end").get("dateTime").asText());
			}
			
			// update activity
			if (activity.getStatus() == null) {
				activity.setStatus(new Status());
			}
			activity.setSummary(summary);
			activity.getStatus().setUpdated(updated.toString());
			activity.getStatus().setStart(start.toString());
			activity.getStatus().setEnd(end.toString());
			context.put("activity", activity);
			
			// TODO: check the constraints again? duration?
			
			logger.info("MeetingAgent is outdated. new data:" +
					"\nSummary = " + summary + 
					"\nUpdated = " + updated.toString() +
					"\nStart   = " + start.toString() +
					"\nEnd     = " + end.toString());
		}
				
		// check if the event is out-dated
		boolean eventOutdated = (eventUpdated != null && 
				updated != null && 
				updated.isAfter(eventUpdated));
		
		// update/create event when not-existing or outdated
		if (event == null || eventOutdated) {
			String method;
			if (event == null) {
				logger.info("Create event");
				method = "createEvent";
				event = JOM.createObjectNode();
			}
			else {
				logger.info("Update event");
				method = "updateEvent";
			}
			event.put("summary", summary);
			event.with("start").put("dateTime", start.toString());
			event.with("end").put("dateTime", end.toString());
			
			ObjectNode params = JOM.createObjectNode();
			params.put("event", event);
			try {
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
