/*
 * Copyright: Almende B.V. (2014), Rotterdam, The Netherlands
 * License: The Apache Software License, Version 2.0
 */
package com.almende.eve.scheduler;

/**
 * The Class AbstractScheduler.
 *
 * @author Almende
 */
public abstract class AbstractScheduler implements Scheduler {
	
	/* (non-Javadoc)
	 * @see com.almende.eve.scheduler.Scheduler#cancelAllTasks()
	 */
	@Override
	public void cancelAllTasks() {
		for (final String id : getTasks()) {
			cancelTask(id);
		}
	}
	
}
