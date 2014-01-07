package com.almende.eve.monitor;

import org.joda.time.DateTime;

import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * 
 * Cache structure for state monitoring
 * 
 * @author ludo
 * 
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
	 * @return
	 */
	public boolean filter(ObjectNode params) {
		if (!params.has("maxAge") || !params.get("maxAge").isInt()
				|| stored == null) {
			return false;
		}
		return stored.plusMillis(params.get("maxAge").intValue()).isAfterNow();
	}
	
	/**
	 * Store value and set storage time to now();
	 * 
	 * @param value
	 */
	public void store(Object value) {
		this.stored = DateTime.now();
		this.value = value;
	}
	
	/**
	 * Get storage timestamp.
	 * 
	 * @return
	 */
	public DateTime getStored() {
		return stored;
	}
	
	/**
	 * Set storage timestamp for filtering the stored value.
	 * 
	 * @param stored
	 */
	public void setStored(DateTime stored) {
		this.stored = stored;
	}
	
	/**
	 * Basic getter
	 * 
	 * @return
	 */
	
	public Object getValue() {
		return value;
	}
	
	/**
	 * Basic setter
	 * 
	 * @param value
	 */
	public void setValue(Object value) {
		this.value = value;
	}
	
}
