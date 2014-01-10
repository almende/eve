/*
 * Copyright: Almende B.V. (2014), Rotterdam, The Netherlands
 * License: The Apache Software License, Version 2.0
 */
package com.almende.eve.monitor;

import org.joda.time.DateTime;

import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * Cache structure for state monitoring.
 * 
 * @author ludo
 */
public class Cache implements ResultMonitorConfigType {
	private static final long	serialVersionUID	= 2159298023743341010L;
	private DateTime			stored				= null;
	private Object				value				= null;
	
	/**
	 * Returns if cached result passes filter data. Default implementation
	 * supports filtering on value age:
	 * 
	 * { "maxAge":1000 } - example: Max age is one second.
	 * 
	 * @param params
	 *            the params
	 * @return true, if successful
	 */
	public boolean filter(final ObjectNode params) {
		if (!params.has("maxAge") || !params.get("maxAge").isInt()
				|| stored == null) {
			return false;
		}
		return stored.plusMillis(params.get("maxAge").intValue()).isAfterNow();
	}
	
	/**
	 * Store value and set storage time to now();.
	 * 
	 * @param value
	 *            the value
	 */
	public void store(final Object value) {
		stored = DateTime.now();
		this.value = value;
	}
	
	/**
	 * Get storage timestamp.
	 * 
	 * @return the stored
	 */
	public DateTime getStored() {
		return stored;
	}
	
	/**
	 * Set storage timestamp for filtering the stored value.
	 * 
	 * @param stored
	 *            the new stored
	 */
	public void setStored(final DateTime stored) {
		this.stored = stored;
	}
	
	/**
	 * Basic getter.
	 * 
	 * @return the value
	 */
	
	public Object getValue() {
		return value;
	}
	
	/**
	 * Basic setter.
	 * 
	 * @param value
	 *            the new value
	 */
	public void setValue(final Object value) {
		this.value = value;
	}
	
}
