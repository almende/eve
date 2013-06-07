package com.almende.eve.scheduler.clock;

import org.joda.time.DateTime;

public interface Clock {
	void requestTrigger(String agentId, DateTime due, Runnable callback);
	void runInPool(Runnable method);

}
