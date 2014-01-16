/*
 * Copyright: Almende B.V. (2014), Rotterdam, The Netherlands
 * License: The Apache Software License, Version 2.0
 */
package com.almende.eve.scheduler.clock;

import java.util.NavigableMap;
import java.util.TreeMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.joda.time.DateTime;
import org.joda.time.Interval;

import com.almende.eve.config.Config;

/**
 * The Class RunnableClock.
 */
public class RunnableClock implements Runnable, Clock {
	private static final NavigableMap<ClockEntry, ClockEntry>	TIMELINE	= new TreeMap<ClockEntry, ClockEntry>();
	private static final ScheduledExecutorService				POOL		= Executors
																					.newScheduledThreadPool(
																							4,
																							Config.getThreadFactory());
	private static ScheduledFuture<?>							future		= null;
	
	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Runnable#run()
	 */
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
	
	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * com.almende.eve.scheduler.clock.Clock#requestTrigger(java.lang.String,
	 * org.joda.time.DateTime, java.lang.Runnable)
	 */
	@Override
	public void requestTrigger(final String agentId, final DateTime due,
			final Runnable callback) {
		synchronized (TIMELINE) {
			final ClockEntry ce = new ClockEntry(agentId, due, callback);
			final ClockEntry oldVal = TIMELINE.get(ce);
			if (oldVal == null || oldVal.getDue().isAfter(due)) {
				TIMELINE.put(ce, ce);
				run();
			}
		}
	}
}

/**
 * @author Almende
 * 
 */
class ClockEntry implements Comparable<ClockEntry> {
	private String		agentId;
	private DateTime	due;
	private Runnable	callback;
	
	/**
	 * @param agentId
	 * @param due
	 * @param callback
	 */
	public ClockEntry(final String agentId, final DateTime due,
			final Runnable callback) {
		this.agentId = agentId;
		this.due = due;
		this.callback = callback;
	}
	
	/**
	 * @return
	 */
	public String getAgentId() {
		return agentId;
	}
	
	/**
	 * @param agentId
	 */
	public void setAgentId(final String agentId) {
		this.agentId = agentId;
	}
	
	/**
	 * @return
	 */
	public DateTime getDue() {
		return due;
	}
	
	/**
	 * @param due
	 */
	public void setDue(final DateTime due) {
		this.due = due;
	}
	
	/**
	 * @return
	 */
	public Runnable getCallback() {
		return callback;
	}
	
	/**
	 * @param callback
	 */
	public void setCallback(final Runnable callback) {
		this.callback = callback;
	}
	
	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
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
	
	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
		return agentId.hashCode();
	}
	
	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Comparable#compareTo(java.lang.Object)
	 */
	@Override
	public int compareTo(final ClockEntry o) {
		if (due.equals(o.due)) {
			return 0;
		}
		return due.compareTo(o.due);
	}
}
