package com.almende.eve.entity.activity;

import java.io.Serializable;

import org.joda.time.DateTime;

@SuppressWarnings("serial")
public class Time implements Serializable {
	public Integer duration = null;        // seconds
	public Integer durationMin = null;     // seconds
	public Integer durationMax = null;     // seconds
	public DateTime periodStart = null;
	public DateTime periodEnd = null;
}
