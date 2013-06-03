package com.almende.eve.entity.activity;

import java.io.Serializable;
import java.net.URI;

import org.joda.time.DateTime;

@SuppressWarnings("serial")
public class Activity implements Serializable, Cloneable {
	
	private String summary = null;
	private String description= null;
	private URI agent = null;   // The agent managing the activity
	private Constraints constraints = null;
	private Status status = null;
	
	public Activity() {	}

	public void setSummary(String summary) {
		this.summary = summary;
	}
	
	public String getSummary() {
		return summary;
	}

	public void setDescription(String description) {
		this.description = description;
	}
	
	public String getDescription() {
		return description;
	}

	public void setAgent(URI agent) {
		this.agent = agent;
	}
	
	public URI getAgent() {
		return agent;
	}

	public void setConstraints(Constraints constraints) {
		this.constraints = constraints != null ? constraints : new Constraints();
	}

	public Constraints getConstraints() {
		return constraints;
	}

	public Constraints withConstraints() {
		if (constraints == null) {
			constraints = new Constraints();
		}
		return constraints;
	}

	public void setStatus(Status status) {
		this.status = status != null ? status : new Status();
	}

	public Status withStatus() {
		if (status == null) {
			status = new Status();
		}
		return status;
	}
	
	public Status getStatus() {
		return status;
	}

	public void merge(Activity other) {
		if (other.summary != null) {
			summary = other.summary;
		}
		if (other.description != null) {
			description = other.description;
		}
		if (other.agent != null) {
			agent = other.agent;
		}
		if (other.constraints != null) {
			if (constraints != null) {
				constraints.merge(other.constraints);
			}
			else {
				constraints = other.constraints.clone();
			}
		}
		if (other.status != null) {
			if (status != null) {
				status.merge(other.status);
			}
			else {
				status = other.status.clone();
			}
		}
		status.merge(other.status);
	}
	
	public Activity clone() {
		Activity clone = new Activity();
		
		clone.summary = summary;
		clone.description = description;
		clone.agent = agent;
		if (constraints != null) {
			clone.constraints = constraints.clone();
		}
		if (status != null) {
			clone.status = status.clone();
		}
		
		return clone;
	}
	
	/**
	 * Check if this Activity is updated more recently than an other Activity
	 * @param other
	 * @return
	 */
	public boolean isNewerThan (Activity other) {
		DateTime updatedThis = null;
		if (this.getStatus() != null && this.getStatus().getUpdated() != null) {
			updatedThis = new DateTime(this.getStatus().getUpdated());
		}
		DateTime updatedOther = null;
		if (other.getStatus() != null && other.getStatus().getUpdated() != null) {
			updatedOther = new DateTime(other.getStatus().getUpdated());
		}

		if (updatedOther == null) {
			// take this as newest
			return true;
		}
		else if (updatedThis == null) {
			// take other as newest
			return false;
		}
		else if (updatedThis.isAfter(updatedOther)) {
			// take this as newest
			return true;	
		}
		else {
			// take other as newest
			return false;
		}
	}

	/**
	 * Synchronize two activities. 
	 * The newest activity will be merged into a clone of the oldest activity.
	 * @param a
	 * @param b
	 * @return
	 */
	public static Activity sync (Activity a, Activity b) {
		Activity clone;
		if (a.isNewerThan(b)) {
			clone = b.clone(); 
			clone.merge(a);
		}
		else {
			clone = a.clone();
			clone.merge(b);
		}

		return clone;
	}

}
