package com.almende.eve.entity.calendar;

import java.util.List;

import org.joda.time.DateTime;

public class Event {
	public String getKind() {
		return kind;
	}
	public void setKind(String kind) {
		this.kind = kind;
	}
	public String getEtag() {
		return etag;
	}
	public void setEtag(String etag) {
		this.etag = etag;
	}
	public String getId() {
		return id;
	}
	public void setId(String id) {
		this.id = id;
	}
	public String getStatus() {
		return status;
	}
	public void setStatus(String status) {
		this.status = status;
	}
	public String getHtmlLink() {
		return htmlLink;
	}
	public void setHtmlLink(String htmlLink) {
		this.htmlLink = htmlLink;
	}
	public DateTime getCreated() {
		return created;
	}
	public void setCreated(DateTime created) {
		this.created = created;
	}
	public DateTime getUpdated() {
		return updated;
	}
	public void setUpdated(DateTime updated) {
		this.updated = updated;
	}
	public String getSummary() {
		return summary;
	}
	public void setSummary(String summary) {
		this.summary = summary;
	}
	public String getDescription() {
		return description;
	}
	public void setDescription(String description) {
		this.description = description;
	}
	public String getLocation() {
		return location;
	}
	public void setLocation(String location) {
		this.location = location;
	}
	public String getColorId() {
		return colorId;
	}
	public void setColorId(String colorId) {
		this.colorId = colorId;
	}
	public Attendee getCreator() {
		return creator;
	}
	public void setCreator(Attendee creator) {
		this.creator = creator;
	}
	public Attendee getOrganizer() {
		return organizer;
	}
	public void setOrganizer(Attendee organizer) {
		this.organizer = organizer;
	}
	public Time getStart() {
		return start;
	}
	public void setStart(Time start) {
		this.start = start;
	}
	public Time getEnd() {
		return end;
	}
	public void setEnd(Time end) {
		this.end = end;
	}
	public List<String> getRecurrence() {
		return recurrence;
	}
	public void setRecurrence(List<String> recurrence) {
		this.recurrence = recurrence;
	}
	public String getRecurringEventId() {
		return recurringEventId;
	}
	public void setRecurringEventId(String recurringEventId) {
		this.recurringEventId = recurringEventId;
	}
	public Time getOriginalStartTime() {
		return originalStartTime;
	}
	public void setOriginalStartTime(Time originalStartTime) {
		this.originalStartTime = originalStartTime;
	}
	public String getTransparency() {
		return transparency;
	}
	public void setTransparency(String transparency) {
		this.transparency = transparency;
	}
	public String getVisibility() {
		return visibility;
	}
	public void setVisibility(String visibility) {
		this.visibility = visibility;
	}
	public String getiCalUID() {
		return iCalUID;
	}
	public void setiCalUID(String iCalUID) {
		this.iCalUID = iCalUID;
	}
	public Integer getSequence() {
		return sequence;
	}
	public void setSequence(Integer sequence) {
		this.sequence = sequence;
	}
	public List<Attendee> getAttendees() {
		return attendees;
	}
	public void setAttendees(List<Attendee> attendees) {
		this.attendees = attendees;
	}
	public Boolean getAttendeesOmitted() {
		return attendeesOmitted;
	}
	public void setAttendeesOmitted(Boolean attendeesOmitted) {
		this.attendeesOmitted = attendeesOmitted;
	}
	public Boolean getAnyoneCanAddSelf() {
		return anyoneCanAddSelf;
	}
	public void setAnyoneCanAddSelf(Boolean anyoneCanAddSelf) {
		this.anyoneCanAddSelf = anyoneCanAddSelf;
	}
	public Boolean getGuestsCanInviteOthers() {
		return guestsCanInviteOthers;
	}
	public void setGuestsCanInviteOthers(Boolean guestsCanInviteOthers) {
		this.guestsCanInviteOthers = guestsCanInviteOthers;
	}
	public Boolean getGuestsCanModify() {
		return guestsCanModify;
	}
	public void setGuestsCanModify(Boolean guestsCanModify) {
		this.guestsCanModify = guestsCanModify;
	}
	public Boolean getGuestsCanSeeOtherGuests() {
		return guestsCanSeeOtherGuests;
	}
	public void setGuestsCanSeeOtherGuests(Boolean guestsCanSeeOtherGuests) {
		this.guestsCanSeeOtherGuests = guestsCanSeeOtherGuests;
	}
	public Boolean getPrivateCopy() {
		return privateCopy;
	}
	public void setPrivateCopy(Boolean privateCopy) {
		this.privateCopy = privateCopy;
	}
	public Reminders getReminders() {
		return reminders;
	}
	public void setReminders(Reminders reminders) {
		this.reminders = reminders;
	}
	
	private String kind = "calendar#event";
	private String etag = null;
	private String id = null;
	private String status = null;
	private String htmlLink = null;
	private DateTime created = null;
	private DateTime updated = null;
	private String summary = null;
	private String description = null;
	private String location = null;
	private String colorId = null;
	private Attendee creator = null;
	private Attendee organizer = null;
	private Time start = null;
	private Time end = null;
	private List<String> recurrence = null;
	private String recurringEventId = null;
	private Time originalStartTime = null;
	private String transparency = null;
	private String visibility = null;
	private String iCalUID = null;
	private Integer sequence = null;
	List<Attendee> attendees = null;
	private Boolean attendeesOmitted = null;

	/*
	 * TODO 
	 * "extendedProperties": { 
	 *   "private": { 
	 *     (key): string 
	 *   }, 
	 *   "shared": {
	 *     (key): string 
	 *   } 
	 * }, 
	 * "gadget": { 
	 *   "type": string, 
	 *   "title": string, 
	 *   "link": string, 
	 *   "iconLink": string, 
	 *   "width": integer, 
	 *   "height": integer,
	 *  "display": string, 
	 *  "preferences": { 
	 *    (key): string } 
	 *   },
	 */

	private Boolean anyoneCanAddSelf = null;
	private Boolean guestsCanInviteOthers = null;
	private Boolean guestsCanModify = null;
	private Boolean guestsCanSeeOtherGuests = null;
	private Boolean privateCopy = null;
	private Reminders reminders = null;

}
