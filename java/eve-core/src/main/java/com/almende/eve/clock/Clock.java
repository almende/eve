package com.almende.eve.clock;

import org.joda.time.DateTime;

public interface Clock {
	public void requestTrigger(String agentId, DateTime due, Runnable callback);
}
