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

import java.util.List;

import org.joda.time.DateTime;

import com.almende.eve.entity.calendar.Event;
import com.almende.eve.entity.calendar.TimePeriod;
import com.almende.eve.json.annotation.Name;
import com.almende.eve.json.annotation.Required;
import com.fasterxml.jackson.databind.node.ArrayNode;

public interface CalendarAgent {
	abstract public Event addEvent(@Name("event") Event event) throws Exception;
	abstract public Event updateEvent(@Name("event") Event event) throws Exception;
	abstract public void deleteEvent(@Name("event") Event event) throws Exception ;

	// TODO: correctly define retrieving an calendarlist
	abstract public ArrayNode getCalendarList() throws Exception;

	abstract public Event getEvent(@Name("id") String id) throws Exception;
	abstract public List<Event> getEvents(
			@Name("start") DateTime start, 
			@Name("end") DateTime end, 
			@Name("calendar") @Required(false) String calendar) throws Exception;
	
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
}

