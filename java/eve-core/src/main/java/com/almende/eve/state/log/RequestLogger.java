/*
 * Copyright: Almende B.V. (2014), Rotterdam, The Netherlands
 * License: The Apache Software License, Version 2.0
 */
package com.almende.eve.state.log;

/**
 * The Interface RequestLogger.
 */
public interface RequestLogger {
	
	/**
	 * Log.
	 *
	 * @param record the record
	 */
	void log(AgentDetailRecord record);
}
