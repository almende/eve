package com.almende.eve.scheduler.clock;

import org.joda.time.DateTime;

public interface Clock {
	public void requestTrigger(String agentId, DateTime due, Runnable callback);
	public void runInPool(Runnable method);

}
