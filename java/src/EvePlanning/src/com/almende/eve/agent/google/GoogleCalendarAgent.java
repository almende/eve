/**
 * @file GoogleCalendarAgent.java
 * 
 * @brief 
 * TODO: brief
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
 * Copyright © 2010-2011 Almende B.V.
 *
 * @author 	Jos de Jong, <jos@almende.org>
 * @date	  2011-07-21
 */


/**
 * 
 * DOCUMENTATION:
 *   http://code.google.com/apis/calendar/data/2.0/developers_guide_java.html
 *   http://www.evolvingsolutions.ca/devwing/category/coding/
 */

package com.almende.eve.agent.google;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.joda.time.DateTime;

import com.almende.eve.agent.Agent;
import com.almende.eve.context.AgentContext;
import com.almende.eve.entity.calendar.Authorization;

import com.almende.eve.json.JSONRPCException;
import com.almende.eve.json.annotation.Name;
import com.almende.eve.json.annotation.Required;
import com.almende.eve.json.jackson.JOM;
import com.almende.util.HttpUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;


public class GoogleCalendarAgent extends Agent /* TODO implements CalendarAgent */ {
	//private Logger logger = Logger.getLogger(this.getClass().getName());

	// TODO: put these constants in a configuration file instead of having them hard coded
	private String CLIENT_ID = "231599786845-p4r6ka1emoj8enivejds6vma41ni2s26.apps.googleusercontent.com";
	private String CLIENT_SECRET = "tUtHesFJEAfiyVjbJd4q0Hvq";
	private String OAUTH_URI = "https://accounts.google.com/o/oauth2";

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
		AgentContext context = getContext();

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
		
		// store the tokens in the context
		context.put("auth", auth);
		context.put("email", email);
		context.put("name", name);		
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
		
		// retrieve new access_token using the refresh_token
		Map<String, String> params = new HashMap<String, String>();
		params.put("client_id", CLIENT_ID);
		params.put("client_secret", CLIENT_SECRET);
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
	 * Remove authorization tokens
	 */
	public void clearAuthorization() {
		AgentContext context = getContext();
		context.remove("auth");
		context.remove("email");
		context.remove("name");			
	}
	
	/**
	 * Get the username associated with the calendar
	 * @return name
	 */
	public String getUsername() {
		return getContext().get("name");
	}
	
	/**
	 * Get the email associated with the calendar
	 * @return email
	 */
	public String getEmail() {
		return getContext().get("email");
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
		Authorization auth = getContext().get("auth");
		
		// check if access_token is expired
		DateTime expires_at = (auth != null) ? auth.getExpiresAt() : null;
		if (expires_at != null && expires_at.isAfterNow()) {
			// TODO: remove this logging
			System.out.println("access token is expired. refreshing now...");
			refreshAuthorization(auth);
			getContext().put("auth", auth);
		}
		
		return auth;
	}
	
	@Override
	public String getVersion() {
		return "0.3";
	}
	
	@Override
	public String getDescription() {
		return "This agent gives access to a Google Calendar. " +
				"It allows to search events, find free timeslots, " +
				"and add, edit, or remove events.";
	}

	public ArrayNode getCalendarList() throws Exception {
		String url = "https://www.googleapis.com/calendar/v3/users/me/calendarList";
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

	public ArrayNode getEvents(
			@Required(false) @Name("start") String start, 
			@Required(false) @Name("end") String end, 
			@Required(false) @Name("calendar") String calendar) throws Exception {
		// get calendar id
		String calendarId = null;
		if (calendar != null) {
			calendarId = calendar;
		}
		else {
			calendarId = getContext().get("email");
		}
		
		// built url with query parameters
		String url = "https://www.googleapis.com/calendar/v3/calendars/" + 
			calendarId + "/events";
		Map<String, String> params = new HashMap<String, String>();
		if (start != null) {
			params.put("timeMin", start);
		}
		if (end != null) {
			params.put("timeMax", end);
		}
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
		}
		else {
			items = JOM.createArrayNode();
		}
		
		return items;
	}
	
	public ArrayNode getEventsToday() throws Exception {
		DateTime start = DateTime.now();
		start = start.minusHours(start.getHourOfDay());
		start = start.minusMinutes(start.getMinuteOfDay());
		start = start.minusSeconds(start.getSecondOfDay());
		start = start.minusMillis(start.getMillisOfDay());
		
		DateTime end = start.plusDays(1);
		
		return getEvents(start.toString(), end.toString(), null);
	}
}


