package com.almende.eve.scheduler.clock;

import java.util.NavigableMap;
import java.util.TreeMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

import org.joda.time.DateTime;
import org.joda.time.Interval;

public class RunnableClock implements Runnable, Clock {
	@SuppressWarnings("unused")
	private static final Logger									LOG			= Logger.getLogger("RunnableClock");
	private static final NavigableMap<ClockEntry, ClockEntry>	TIMELINE	= new TreeMap<ClockEntry, ClockEntry>();
	private static final ScheduledExecutorService				POOL		= Executors
																					.newScheduledThreadPool(2);
	private static ScheduledFuture<?>							future		= null;
	
	@Override
	public void run() {
		synchronized (TIMELINE) {
			while (!TIMELINE.isEmpty()) {
				final ClockEntry ce = TIMELINE.firstEntry().getValue();
				final DateTime now = DateTime.now();
				if (ce.getDue().isBefore(now)) {
					TIMELINE.remove(ce);
					POOL.execute(ce.getCallback());
					continue;
				}
				if (future != null) {
					future.cancel(false);
					future = null;
				}
				final long interval = new Interval(now, ce.getDue())
						.toDurationMillis();
				future = POOL.schedule(this, interval, TimeUnit.MILLISECONDS);
				break;
			}
		}
	}
	
	@Override
	public void requestTrigger(final String agentId, final DateTime due, final Runnable callback) {
		synchronized (TIMELINE) {
			final ClockEntry ce = new ClockEntry(agentId, due, callback);
			final ClockEntry oldVal = TIMELINE.get(ce);
			if (oldVal == null || oldVal.getDue().isAfter(due)) {
				TIMELINE.put(ce, ce);
				run();
			}
		}
	}
	
	@Override
	public void runInPool(final Runnable method) {
		POOL.execute(method);
	}
}

class ClockEntry implements Comparable<ClockEntry> {
	private String		agentId;
	private DateTime	due;
	private Runnable	callback;
	
	public ClockEntry(final String agentId, final DateTime due, final Runnable callback) {
		this.agentId = agentId;
		this.due = due;
		this.callback = callback;
	}
	
	public String getAgentId() {
		return agentId;
	}
	
	public void setAgentId(final String agentId) {
		this.agentId = agentId;
	}
	
	public DateTime getDue() {
		return due;
	}
	
	public void setDue(final DateTime due) {
		this.due = due;
	}
	
	public Runnable getCallback() {
		return callback;
	}
	
	public void setCallback(final Runnable callback) {
		this.callback = callback;
	}
	
	@Override
	public boolean equals(final Object o) {
		if (this == o) {
			return true;
		}
		if (!(o instanceof ClockEntry)) {
			return false;
		}
		final ClockEntry other = (ClockEntry) o;
		return agentId.equals(other.agentId);
	}
	
	@Override
	public int hashCode() {
		return agentId.hashCode();
	}
	
	@Override
	public int compareTo(final ClockEntry o) {
		if (due.equals(o.due)) {
			return 0;
		}
		return due.compareTo(o.due);
	}
}
