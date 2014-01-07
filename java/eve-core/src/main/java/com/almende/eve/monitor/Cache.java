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
public interface Cache extends ResultMonitorConfigType {

	/**
	 * Basic setter
	 * 
	 * @param value
	 */
	void setValue(Object value);

	/**
	 * Basic getter, not found behavior is implementation specific. 
	 * 
	 * @return
	 */
	Object getValue();

	/**
	 * Set storage timestamp for filtering the stored value. 
	 * 
	 * @param stored
	 */
	void setStored(DateTime stored);

	/**
	 * Get storage timestamp.
	 * 
	 * @return
	 */
	DateTime getStored();

	/**
	 * Store value and set storage time to now();
	 * 
	 * @param value
	 */
	void store(Object value);

	/**
	 * returns if cached result passes filter data. Filter availability is implementation specific.
	 * 
	 * @param params
	 * @return
	 */
	boolean filter(ObjectNode params);
	
}
