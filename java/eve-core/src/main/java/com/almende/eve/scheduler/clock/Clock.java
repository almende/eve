/*
 * Copyright: Almende B.V. (2014), Rotterdam, The Netherlands
 * License: The Apache Software License, Version 2.0
 */
package com.almende.eve.scheduler.clock;

import org.joda.time.DateTime;
/**
 * The Interface Clock.
 */
public interface Clock {
	
	/**
	 * Request trigger.
	 *
	 * @param agentId the agent id
	 * @param due the due
	 * @param callback the callback
	 */
	void requestTrigger(String agentId, DateTime due, Runnable callback);
	
}
