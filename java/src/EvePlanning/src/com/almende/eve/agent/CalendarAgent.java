/**
 * @file CalendarAgent.java
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
 * @date	  2012-05-09
 */

package com.almende.eve.agent;

import com.almende.eve.json.annotation.Name;
import com.almende.eve.json.annotation.Required;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

public interface CalendarAgent {
	abstract public String getUsername();
	abstract public String getEmail();
	
	public ArrayNode getCalendarList() throws Exception;
	
	public ArrayNode getEvents(
			@Required(false) @Name("start") String start, 
			@Required(false) @Name("end") String end, 
			@Required(false) @Name("calendarId") String calendarId) 
			throws Exception;
	
	public ObjectNode getEvent (
			@Name("eventId") String eventId,
			@Required(false) @Name("calendarId") String calendarId) 
			throws Exception;
	
	public ObjectNode createEvent (
			@Name("event") ObjectNode event,
			@Required(false) @Name("calendarId") String calendarId) 
			throws Exception;
	
	public ObjectNode updateEvent (@Name("event") ObjectNode event,
			@Required(false) @Name("calendarId") String calendarId) 
			throws Exception;
	
	public void deleteEvent (
			@Name("eventId") String eventId,
			@Required(false) @Name("calendarId") String calendarId) 
			throws Exception;
	
	/* TODO: implement methods getChangedEvents, getFree, getBusy
	abstract public List<Event> getChangedEvents(@Name("since") DateTime since) 
			throws Exception;

	abstract public List<TimePeriod> getFree (
			@Name("start") DateTime start, 
			@Name("end") DateTime end,
			@Name("calendar") @Required(false) String calendar) throws Exception;

	abstract public List<TimePeriod> getBusy (
			@Name("start") DateTime start, 
			@Name("end") DateTime end,
			@Name("calendar") @Required(false) String calendar) throws Exception;
	*/
}

