package com.almende.eve.entity.activity;

import java.io.Serializable;

import org.joda.time.DateTime;

@SuppressWarnings("serial")
public class Activity implements Serializable, Cloneable {
	public Activity() {
		setSummary(null);
		setConstraints(null);
		setStatus(null);
	}

	public void setSummary(String summary) {
		this.summary = summary;
	}
	
	public String getSummary() {
		return summary;
	}

	public void setConstraints(Constraints constraints) {
		this.constraints = constraints != null ? constraints : new Constraints();
	}

	public Constraints getConstraints() {
		return constraints;
	}

	public void setStatus(Status status) {
		this.status = status != null ? status : new Status();
	}

	public Status getStatus() {
		return status;
	}

	public void merge(Activity other) {
		if (other.summary != null) {
			summary = other.summary;
		}
		constraints.merge(other.constraints);
		status.merge(other.status);
	}
	
	public Activity clone() {
		Activity clone = new Activity();
		clone.summary = summary;
		clone.constraints = constraints.clone();
		clone.status = status.clone();
		
		return clone;
	}
	
	public static Activity sync (Activity a, Activity b) {
		DateTime updatedA = null;
		if (a.getStatus().getUpdated() != null) {
			updatedA = new DateTime(a.getStatus().getUpdated());
		}
		DateTime updatedB = null;
		if (b.getStatus().getUpdated() != null) {
			updatedB = new DateTime(b.getStatus().getUpdated());
		}
		
		// TODO: simplify this
		Activity clone;
		if (updatedB == null) {
			// take a as newest
			clone = b.clone(); 
			clone.merge(a);
		}
		else if (updatedA == null) {
			// take b as newest
			clone = a.clone(); 
			clone.merge(b);
		}
		else if (updatedA.isAfter(updatedB)) {
			// take a as newest
			clone = b.clone(); 
			clone.merge(a);			
		}
		else {
			// take b as newest
			clone = a.clone();
			clone.merge(b);
		}
				
		return clone;
	}
	
	private String summary = "";
	private Constraints constraints = new Constraints();
	private Status status = new Status();
}
