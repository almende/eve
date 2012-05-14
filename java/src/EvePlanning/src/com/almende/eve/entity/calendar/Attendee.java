package com.almende.eve.entity.calendar;

public class Attendee {
	public String getEmail() {
		return email;
	}
	public void setEmail(String email) {
		this.email = email;
	}
	public String getDisplayName() {
		return displayName;
	}
	public void setDisplayName(String displayName) {
		this.displayName = displayName;
	}
	public Boolean getOrganizer() {
		return organizer;
	}
	public void setOrganizer(Boolean organizer) {
		this.organizer = organizer;
	}
	public Boolean getSelf() {
		return self;
	}
	public void setSelf(Boolean self) {
		this.self = self;
	}
	public Boolean getResource() {
		return resource;
	}
	public void setResource(Boolean resource) {
		this.resource = resource;
	}
	public Boolean getOptional() {
		return optional;
	}
	public void setOptional(Boolean optional) {
		this.optional = optional;
	}
	public String getResponseStatus() {
		return responseStatus;
	}
	public void setResponseStatus(String responseStatus) {
		this.responseStatus = responseStatus;
	}
	public String getComment() {
		return comment;
	}
	public void setComment(String comment) {
		this.comment = comment;
	}
	public Integer getAdditionalGuests() {
		return additionalGuests;
	}
	public void setAdditionalGuests(Integer additionalGuests) {
		this.additionalGuests = additionalGuests;
	}
	
	private String email = null;
    private String displayName = null;
    private Boolean organizer = null;
    private Boolean self = null;
    private Boolean resource = null;
    private Boolean optional = null;
    private String responseStatus = null;
    private String comment = null;
    private Integer additionalGuests = null;
}
