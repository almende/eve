/**
 * @file GoogleCalendarAgent.java
 * 
 * @brief 
 * The GoogleCalendarAgent can connect to a single Google Calendar, and 
 * get, create, update, and delete events. The agent uses Goolges RESTful API v3 
 * to access a Google Calendar, and does not use any specific Java libraries
 * for that. See:
 * https://developers.google.com/google-apps/calendar/v3/reference/
 * 
 * To setup authorization for a calendar agent, the method setAuthorization 
 * must be executed with valid authorization tokens. The agent will store
 * the tokens and refresh them automatically when needed.
 * To retrieve valid access tokens from google, the servlet GoogleAuth.java
 * can be used. This servlet is typically running at /auth/google.
 * Authorization needs to be setup only once for an agent.
 * 
 * The GoogleCalendarAgent contains the following core methods:
 *     - getEvents    Get all events in a given time window
 *     - getEvent     Get a specific event by its id
 *     - createEvent  Create a new event
 *     - updateEvent  Update an existing event
 *     - deleteEvent  Delete an existing event
 *     - getBusy      Get the busy intervals in given time window
 *     - clear        Delete all stored information 
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
 * @date	2012-07-03
 */


/**
 * 
 * DOCUMENTATION:
 *   https://developers.google.com/google-apps/calendar/v3/reference/
 *   https://developers.google.com/google-apps/calendar/v3/reference/events#resource
 */

package com.almende.eve.agent.google;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.Interval;

import com.almende.eve.agent.Agent;
import com.almende.eve.agent.CalendarAgent;
import com.almende.eve.config.Config;
import com.almende.eve.entity.calendar.Authorization;
import com.almende.eve.rpc.annotation.Access;
import com.almende.eve.rpc.annotation.AccessType;
import com.almende.eve.rpc.annotation.Name;
import com.almende.eve.rpc.annotation.Optional;
import com.almende.eve.rpc.jsonrpc.JSONRPCException;
import com.almende.eve.rpc.jsonrpc.JSONRPCException.CODE;
import com.almende.eve.rpc.jsonrpc.jackson.JOM;
import com.almende.eve.state.State;
import com.almende.util.HttpUtil;
import com.almende.util.IntervalsUtil;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

@Access(AccessType.PUBLIC)
public class GoogleCalendarAgent extends Agent implements CalendarAgent {
	// private Logger logger = Logger.getLogger(this.getClass().getName());

	// note: config parameters google.client_id and google.client_secret
	//       are loaded from the eve configuration
	private String OAUTH_URI = "https://accounts.google.com/o/oauth2";
	private String CALENDAR_URI = "https://www.googleapis.com/calendar/v3/calendars/";
	
	/**
	 * Set access token and refresh token, used to authorize the calendar agent. 
	 * These tokens must be retrieved via Oauth 2.0 authorization.
	 * @param access_token
	 * @param token_type
	 * @param expires_in
	 * @param refresh_token
	 * @throws IOException 
	 */
	public void setAuthorization (
			@Name("access_token") String access_token,
			@Name("token_type") String token_type,
			@Name("expires_in") Integer expires_in,
			@Name("refresh_token") String refresh_token) throws IOException {
		State state = getState();
		
		// retrieve user information
		String url = "https://www.googleapis.com/oauth2/v1/userinfo";
		Map<String, String> headers = new HashMap<String, String>();
		headers.put("Authorization", token_type + " " + access_token);
		String resp = HttpUtil.get(url, headers);
		
		ObjectNode info = JOM.getInstance().readValue(resp, ObjectNode.class);
		String email = info.has("email") ? info.get("email").asText() : null;
		String name = info.has("name") ? info.get("name").asText() : null;
		
		DateTime expires_at = calculateExpiresAt(expires_in);
		Authorization auth = new Authorization(access_token, token_type, 
				expires_at, refresh_token);
		
		// store the tokens in the state
		state.put("auth", auth);
		state.put("email", email);
		state.put("name", name);
	}
	
	/**
	 * Calculate the expiration time from a life time
	 * @param expires_in      Expiration time in seconds
	 * @return
	 */
	private DateTime calculateExpiresAt(Integer expires_in) {
		DateTime expires_at = null;
		if (expires_in != null && expires_in != 0) {
			// calculate expiration time, and subtract 5 minutes for safety
			expires_at = DateTime.now().plusSeconds(expires_in).minusMinutes(5);
		}
		return expires_at;
	}
		
	/**
	 * Refresh the access token using the refresh token
	 * the tokens in provided authorization object will be updated
	 * @param auth
	 * @throws Exception 
	 */
	private void refreshAuthorization (Authorization auth) throws Exception {
		String refresh_token = (auth != null) ? auth.getRefreshToken() : null;
		if (refresh_token == null) {
			throw new Exception("No refresh token available");
		}
		
		Config config = getAgentHost().getConfig();
		String client_id = config.get("google", "client_id");
		String client_secret = config.get("google", "client_secret");
		
		// retrieve new access_token using the refresh_token
		Map<String, String> params = new HashMap<String, String>();
		params.put("client_id", client_id);
		params.put("client_secret", client_secret);
		params.put("refresh_token", refresh_token);
		params.put("grant_type", "refresh_token");
		String resp = HttpUtil.postForm(OAUTH_URI + "/token", params);
		ObjectNode json = JOM.getInstance().readValue(resp, ObjectNode.class);
		if (!json.has("access_token")) {
			// TODO: give more specific error message
			throw new Exception("Retrieving new access token failed");
		}
		
		// update authorization
		if (json.has("access_token")) {
			auth.setAccessToken(json.get("access_token").asText());
		}
		if (json.has("expires_in")) {
			Integer expires_in = json.get("expires_in").asInt();
			DateTime expires_at = calculateExpiresAt(expires_in);
			auth.setExpiresAt(expires_at);
		}
	}
	
	/**
	 * Remove all stored data from this agent
	 */
	@Override
	public void sigDelete() {
		State state = getState();
		state.remove("auth");
		state.remove("email");
		state.remove("name");

		super.sigDelete();
	}
	
	/**
	 * Get the username associated with the calendar
	 * @return name
	 */
	@Override
	public String getUsername() {
		return getState().get("name",String.class);
	}
	
	/**
	 * Get the email associated with the calendar
	 * @return email
	 */
	@Override
	public String getEmail() {
		return getState().get("email",String.class);
	}
	
	/**
	 * Get ready-made HTTP request headers containing the authorization token
	 * Example usage: HttpUtil.get(url, getAuthorizationHeaders());
	 * @return
	 * @throws Exception 
	 */
	private Map<String, String> getAuthorizationHeaders () throws Exception {
		Authorization auth = getAuthorization();
		
		String access_token = (auth != null) ? auth.getAccessToken() : null;
		if (access_token == null) {
			throw new Exception("No authorization token available");
		}
		String token_type = (auth != null) ? auth.getTokenType() : null;
		if (token_type == null) {
			throw new Exception("No token type available");
		}
		
		Map<String, String> headers = new HashMap<String, String>();
		headers.put("Authorization", token_type + " " + access_token);
		return headers;
	}
	
	/**
	 * Retrieve authorization tokens
	 * @return
	 * @throws Exception
	 */
	private Authorization getAuthorization() throws Exception {
		Authorization auth = getState().get("auth",Authorization.class);

		// check if access_token is expired
		DateTime expires_at = (auth != null) ? auth.getExpiresAt() : null;
		if (expires_at != null && expires_at.isBeforeNow()) {
			refreshAuthorization(auth);
			getState().put("auth", auth);
		}
		
		return auth;
	}
	
	/**
	 * Get the calendar agents version
	 */
	@Override
	public String getVersion() {
		return "0.4";
	}
	
	/**
	 * Get the calendar agents description
	 */
	@Override
	public String getDescription() {
		return "This agent gives access to a Google Calendar. " +
				"It allows to search events, find free timeslots, " +
				"and add, edit, or remove events.";
	}

	/**
	 * Convert the event from a Eve event to a Google event 
	 * @param event
	 */
	private void toGoogleEvent(ObjectNode event) {
		if (event.has("agent") && event.get("agent").isTextual()) {
			// move agent url from event.agent to extendedProperties
			String agent = event.get("agent").asText();
			event.with("extendedProperties").with("shared").put("agent", agent);
			
			// TODO: change location into a string
		}
	}

	/**
	 * Convert the event from a Google event to a Eve event 
	 * @param event
	 */
	private void toEveEvent(ObjectNode event) {
		ObjectNode extendedProperties = (ObjectNode) event.get("extendedProperties");
		if (extendedProperties != null) {
			ObjectNode shared = (ObjectNode) extendedProperties.get("shared");
			if (shared != null && shared.has("agent") && shared.get("agent").isTextual()) {
				// move agent url from extended properties to event.agent
				String agent = shared.get("agent").asText();
				event.put("agent", agent);
				
				/* TODO: remove agent from extended properties
				shared.remove("agent");
				if (shared.size() == 0) {
					extendedProperties.remove("shared");
					if (extendedProperties.size() == 0) {
						event.remove("extendedProperties");
					}
				}
				*/
				
				// TODO: replace string location with Location object
			}
		}
	}
	
	/**
	 * Retrieve a list with all calendars in this google calendar
	 */
	@Override
	public ArrayNode getCalendarList() throws Exception {
		String url = CALENDAR_URI + "users/me/calendarList";
		String resp = HttpUtil.get(url, getAuthorizationHeaders());
		ObjectNode calendars = JOM.getInstance().readValue(resp, ObjectNode.class);

		// check for errors
		if (calendars.has("error")) {
			ObjectNode error = (ObjectNode)calendars.get("error");
			throw new JSONRPCException(error);
		}

		// get items from response
		ArrayNode items = null;
		if (calendars.has("items")) {
			items = (ArrayNode)calendars.get("items");
		}
		else {
			items = JOM.createArrayNode();
		}
		
		return items;
	}

	/**
	 * Get todays events. A convenience method for easy testing
	 * @param calendarId
	 * @return
	 * @throws Exception
	 */
	public ArrayNode getEventsToday(
			@Optional @Name("calendarId") String calendarId) throws Exception {
		DateTime now = DateTime.now();
		DateTime timeMin = now.minusMillis(now.getMillisOfDay());
		DateTime timeMax = timeMin.plusDays(1);

		return getEvents(timeMin.toString(), timeMax.toString(), calendarId);
	}
	
	/**
	 * Get all events in given interval
	 * @param timeMin 		start of the interval
	 * @param timeMax 		end of the interval
	 * @param calendarId   	optional calendar id. If not provided, the default
	 *                      calendar is used
	 */
	@Override
	public ArrayNode getEvents(
			@Optional @Name("timeMin") String timeMin, 
			@Optional @Name("timeMax") String timeMax, 
			@Optional @Name("calendarId") String calendarId) 
			throws Exception {
		// initialize optional parameters
		if (calendarId == null) {
			calendarId = getState().get("email",String.class);
		}
		
		// built url with query parameters
		String url = CALENDAR_URI + calendarId + "/events";
		Map<String, String> params = new HashMap<String, String>();
		if (timeMin != null) {
			params.put("timeMin", new DateTime(timeMin).toString());
		}
		if (timeMax != null) {
			params.put("timeMax", new DateTime(timeMax).toString());
		}
		// Set singleEvents=true to expand recurring events into instances
		params.put("singleEvents", "true");
		url = HttpUtil.appendQueryParams(url, params);
		
		// perform GET request
		Map<String, String> headers = getAuthorizationHeaders();
		String resp = HttpUtil.get(url, headers);
		ObjectMapper mapper = JOM.getInstance();
		ObjectNode json = mapper.readValue(resp, ObjectNode.class);
		
		// check for errors
		if (json.has("error")) {
			ObjectNode error = (ObjectNode)json.get("error");
			throw new JSONRPCException(error);
		}

		// get items from the response
		ArrayNode items = null;
		if (json.has("items")){
			items = (ArrayNode) json.get("items");
			
			// convert from Google to Eve event
			for (int i = 0; i < items.size(); i++) {
				ObjectNode item = (ObjectNode) items.get(i);
				toEveEvent(item);
			}
		}
		else {
			items = JOM.createArrayNode();
		}
		
		return items;
	}

	/**
	 * Get busy intervals of today. A convenience method for easy testing
	 * @param calendarId
	 * @return
	 * @throws Exception
	 */
	public ArrayNode getBusyToday(
			@Optional @Name("calendarId") String calendarId,
			@Optional @Name("excludeEventIds") Set<String> excludeEventIds) 
			throws Exception {
		DateTime now = DateTime.now();
		DateTime timeMin = now.minusMillis(now.getMillisOfDay());
		DateTime timeMax = timeMin.plusDays(1);
		String dateTimeZone = null;

		return getBusy(timeMin.toString(), timeMax.toString(), calendarId, 
				excludeEventIds, dateTimeZone);
	}
	
	/**
	 * Get the start time from a google event (including all-day-events)
	 * Returns null if not found
	 * @param event
	 * @param timeZone   Timezone, needed for all-day-events
	 * @return start
	 */
	private static DateTime getStart(ObjectNode event, DateTimeZone timeZone) {
		if (!event.has("start")) {
			return null;
		}
		JsonNode startObj = event.get("start");
		
		DateTime start = null;
		if (startObj.has("dateTime") && !startObj.get("dateTime").isNull()) {
			String dateTimeStr = startObj.get("dateTime").asText();
			start = new DateTime(dateTimeStr);
		}
		else if (startObj.has("date") && !startObj.get("date").isNull()) {
			String dateStr = startObj.get("date").asText();
			
			if (startObj.has("timeZone") && !startObj.get("timeZone").isNull()) {
				String timeZoneStr = startObj.get("timeZone").asText();
				timeZone = DateTimeZone.forID(timeZoneStr);
			}
			if (timeZone != null) {
				start = new DateTime(dateStr, timeZone);
			}
			else {
				start = new DateTime(dateStr);
			}
		}
		else {
			start = null;
		}
		
		return start;
	}
	
	/**
	 * Get the end time from a google event (including all-day-events)
	 * Returns null if not found
	 * @param event
	 * @param timeZone   Timezone, needed for all-day-events
	 * @return end
	 */
	private static DateTime getEnd(ObjectNode event, DateTimeZone timeZone) {
		if (!event.has("end")) {
			return null;
		}
		JsonNode endObj = event.get("end");
		
		DateTime end = null;
		if (endObj.has("dateTime") && !endObj.get("dateTime").isNull()) {
			String dateTimeStr = endObj.get("dateTime").asText();
			end = new DateTime(dateTimeStr);
		}
		else if (endObj.has("date") && !endObj.get("date").isNull()) {
			String dateStr = endObj.get("date").asText();
			
			if (endObj.has("timeZone") && !endObj.get("timeZone").isNull()) {
				String timeZoneStr = endObj.get("timeZone").asText();
				timeZone = DateTimeZone.forID(timeZoneStr);
			}
			if (timeZone != null) {
				end = new DateTime(dateStr, timeZone);
			}
			else {
				end = new DateTime(dateStr);
			}
		}
		else {
			end = null;
		}
		
		return end;
	}

	/**
	 * Retrieve the busy intervals in the calendar
	 * @param timeMin         Start time
	 * @param timeMax         End time
	 * @param calendarId      Optional calendar id. the primary calendar is 
	 *                         used by default
	 * @param excludeEventIds Optional list with ids of events to be excluded
	 *                         from the busy intervals.
	 * @param timeZone        Optional time zone. UTC is used by default. 
	 *                         Needed to correctly process all-day-events.
	 * @throws Exception 
	 */
	@Override
	public ArrayNode getBusy(
			@Name("timeMin") String timeMin, 
			@Name("timeMax") String timeMax,
			@Optional @Name("calendarId") String calendarId,
			@Optional @Name("excludeEventIds") Set<String> excludeEventIds,
			@Optional @Name("timeZone") String timeZone ) 
			throws Exception {
		DateTimeZone dtz = DateTimeZone.UTC;
		if (timeZone != null) {
			dtz = DateTimeZone.forID(timeZone);
		}		
		
		ArrayNode events = getEvents(timeMin, timeMax, calendarId);
		
		List<Interval> busy = new ArrayList<Interval>();
        for (int i = 0; i < events.size(); i++) {
        	ObjectNode event = (ObjectNode) events.get(i);
        	
        	// filter excludes
        	String eventId = event.has("id") ? event.get("id").asText() : null;
        	boolean exclude = (eventId != null && excludeEventIds != null &&
        			excludeEventIds.contains(eventId));
        	if (!exclude) {
        		DateTime start = getStart(event, dtz);
        		DateTime end = getEnd(event, dtz);
        		if (start != null && end != null) {
		        	Interval interval = new Interval(start, end);
		        	busy.add(interval);
        		}
        	}
        }

        // merge the intervals
        List<Interval> merged = IntervalsUtil.merge(busy);
        
        // convert to JSON array
        ArrayNode array = JOM.createArrayNode();
        for (Interval interval : merged) {
        	ObjectNode obj = JOM.createObjectNode();
        	obj.put("start", interval.getStart().toString());
        	obj.put("end", interval.getEnd().toString());
        	array.add(obj);
        }
        return array;
	}

	/**
	 * Get a single event by id
	 * @param eventId         Id of the event
	 * @param calendarId      Optional calendar id. the primary calendar is 
	 *                         used by default
	 */
	@Override
	public ObjectNode getEvent (
			@Name("eventId") String eventId,
			@Optional @Name("calendarId") String calendarId) 
			throws Exception {
		// initialize optional parameters
		if (calendarId == null) {
			calendarId = getState().get("email",String.class);
		}

		// built url
		String url = CALENDAR_URI + calendarId + "/events/" + eventId;
		
		// perform GET request
		Map<String, String> headers = getAuthorizationHeaders();
		String resp = HttpUtil.get(url, headers);
		ObjectMapper mapper = JOM.getInstance();
		ObjectNode event = mapper.readValue(resp, ObjectNode.class);
		
		// convert from Google to Eve event
		toEveEvent(event);
		
		// check for errors
		if (event.has("error")) {
			ObjectNode error = (ObjectNode)event.get("error");
			Integer code = error.has("code") ? error.get("code").asInt() : null;
			if (code != null && (code.equals(404) || code.equals(410))) {
				throw new JSONRPCException(CODE.NOT_FOUND);				
			}
			
			throw new JSONRPCException(error);
		}
		
		// check if canceled. If so, return null
		// TODO: be able to retrieve canceled events?
		if (event.has("status") && event.get("status").asText().equals("cancelled")) {
			throw new JSONRPCException(CODE.NOT_FOUND);
		}
		
		return event;
	}

	/**
	 * Create an event
	 * @param event           JSON structure containing the calendar event
	 * @param calendarId      Optional calendar id. the primary calendar is 
	 *                         used by default
	 * @return createdEvent   JSON structure with the created event
	 */
	@Override
	public ObjectNode createEvent (@Name("event") ObjectNode event,
			@Optional @Name("calendarId") String calendarId) 
			throws Exception {
		// initialize optional parameters
		if (calendarId == null) {
			calendarId = getState().get("email",String.class);
		}

		// built url
		String url = CALENDAR_URI + calendarId + "/events";

		// convert from Google to Eve event
		toGoogleEvent(event);
		
		// perform POST request
		ObjectMapper mapper = JOM.getInstance();
		String body = mapper.writeValueAsString(event);
		Map<String, String> headers = getAuthorizationHeaders();
		headers.put("Content-Type", "application/json");
		String resp = HttpUtil.post(url, body, headers);
		ObjectNode createdEvent = mapper.readValue(resp, ObjectNode.class);
		
		// convert from Google to Eve event
		toEveEvent(event);
		
		// check for errors
		if (createdEvent.has("error")) {
			ObjectNode error = (ObjectNode)createdEvent.get("error");
			throw new JSONRPCException(error);
		}
		
		return createdEvent;
	}

	/**
	 * Quick create an event
	 * @param start
	 * @param end
	 * @param summary
	 * @param location
	 * @param calendarId
	 * @return
	 * @throws Exception
	 */
	public ObjectNode createEventQuick (
			@Optional @Name("start") String start,
			@Optional @Name("end") String end,
			@Optional @Name("summary") String summary,
			@Optional @Name("location") String location,
			@Optional @Name("calendarId") String calendarId) throws Exception {
		ObjectNode event = JOM.createObjectNode();
		
		if (start == null) {
			// set start to current time, rounded to hours
			DateTime startDate = DateTime.now();
			startDate = startDate.plusHours(1);
			startDate = startDate.minusMinutes(startDate.getMinuteOfHour());
			startDate = startDate.minusSeconds(startDate.getSecondOfMinute());
			startDate = startDate.minusMillis(startDate.getMillisOfSecond());
			start = startDate.toString();
		}
		ObjectNode startObj = JOM.createObjectNode();
		startObj.put("dateTime", start);
		event.put("start", startObj);
		if (end == null) {
			// set end to start +1 hour
			DateTime startDate = new DateTime(start);
			DateTime endDate = startDate.plusHours(1);
			end = endDate.toString();
		}
		ObjectNode endObj = JOM.createObjectNode();
		endObj.put("dateTime", end);
		event.put("end", endObj);
		if (summary != null) {
			event.put("summary", summary);
		}
		if (location != null) {
			event.put("location", location);
		}
		
		return createEvent(event, calendarId);
	}
	
	/**
	 * Update an existing event
	 * @param event           JSON structure containing the calendar event
	 *                         (event must have an id)
	 * @param calendarId      Optional calendar id. the primary calendar is 
	 *                         used by default
	 * @return updatedEvent   JSON structure with the updated event
	 */
	@Override
	public ObjectNode updateEvent (@Name("event") ObjectNode event,
			@Optional @Name("calendarId") String calendarId) 
			throws Exception {
		// initialize optional parameters
		if (calendarId == null) {
			calendarId = getState().get("email",String.class);
		}

		// convert from Eve to Google event
		toGoogleEvent(event);

		// read id from event
		String id = event.get("id").asText();
		if (id == null) {
			throw new Exception("Parameter 'id' missing in event");
		}
		
		// built url
		String url = CALENDAR_URI + calendarId + "/events/" + id;
		
		// perform POST request
		ObjectMapper mapper = JOM.getInstance();
		String body = mapper.writeValueAsString(event);
		Map<String, String> headers = getAuthorizationHeaders();
		headers.put("Content-Type", "application/json");
		String resp = HttpUtil.put(url, body, headers);
		ObjectNode updatedEvent = mapper.readValue(resp, ObjectNode.class);
		
		// check for errors
		if (updatedEvent.has("error")) {
			ObjectNode error = (ObjectNode)updatedEvent.get("error");
			throw new JSONRPCException(error);
		}

		// convert from Google to Eve event
		toEveEvent(event);
		
		return updatedEvent;
	}
	
	/**
	 * Delete an existing event
	 * @param eventId         id of the event to be deleted
	 * @param calendarId      Optional calendar id. the primary calendar is 
	 *                         used by default
	 */
	@Override
	public void deleteEvent (@Name("eventId") String eventId,
			@Optional @Name("calendarId") String calendarId) 
			throws Exception {
		// initialize optional parameters
		if (calendarId == null) {
			calendarId = getState().get("email",String.class);
		}

		// built url
		String url = CALENDAR_URI + calendarId + "/events/" + eventId;
		
		// perform POST request
		Map<String, String> headers = getAuthorizationHeaders();
		String resp = HttpUtil.delete(url, headers);
		if (!resp.isEmpty()) {
			ObjectNode node = JOM.getInstance().readValue(resp, ObjectNode.class);
			
			// check error code
			if (node.has("error")) {
				ObjectNode error = (ObjectNode) node.get("error");
				Integer code = error.has("code") ? error.get("code").asInt() : null;
				if (code != null && (code.equals(404) || code.equals(410))) {
					throw new JSONRPCException(CODE.NOT_FOUND);				
				}
				
				throw new JSONRPCException(error);
			}
			else {
				throw new Exception(resp);
			}
		}
	}
}


