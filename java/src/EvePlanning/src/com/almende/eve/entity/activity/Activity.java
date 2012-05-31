package com.almende.eve.entity.activity;

import java.io.Serializable;

@SuppressWarnings("serial")
public class Activity implements Serializable {
	public String summary = null;
	public Constraints constraints = new Constraints();
	public Status status = new Status();
}
