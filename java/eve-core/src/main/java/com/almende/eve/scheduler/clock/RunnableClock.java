package com.almende.eve.scheduler.clock;

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
	private static final Logger								LOG			= Logger.getLogger("RunnableClock");
	private static final TreeMap<ClockEntry, ClockEntry>	TIMELINE	= new TreeMap<ClockEntry, ClockEntry>();
	private static ScheduledExecutorService					pool		= Executors
																				.newScheduledThreadPool(8);
	private static ScheduledFuture<?>						future		= null;
	
	public void run() {
		synchronized (TIMELINE) {
			while (!TIMELINE.isEmpty()) {
				ClockEntry ce = TIMELINE.firstEntry().getValue();
				DateTime now = DateTime.now();
				if (ce.getDue().isBefore(now)) {
					TIMELINE.remove(ce);
					pool.execute(ce.getCallback());
					continue;
				}
				if (future != null) {
					future.cancel(false);
					future = null;
				}
				long interval = new Interval(now, ce.getDue()).toDurationMillis();
				future = pool.schedule(this, interval, TimeUnit.MILLISECONDS);
				break;
			}
		}
	}
	
	@Override
	public void requestTrigger(String agentId, DateTime due, Runnable callback) {
		synchronized (TIMELINE) {
			ClockEntry ce = new ClockEntry(agentId, due, callback);
			ClockEntry oldVal = TIMELINE.get(ce);
			if (oldVal == null || oldVal.getDue().isAfter(due)) {
				TIMELINE.put(ce, ce);
				run();
			}
		}
	}
	
	@Override
	public void runInPool(Runnable method) {
		pool.execute(method);
	}
}

class ClockEntry implements Comparable<ClockEntry> {
	private String		agentId;
	private DateTime	due;
	private Runnable	callback;
	
	public ClockEntry(String agentId, DateTime due, Runnable callback) {
		this.agentId = agentId;
		this.due = due;
		this.callback = callback;
	}
	
	public String getAgentId() {
		return agentId;
	}

	public void setAgentId(String agentId) {
		this.agentId = agentId;
	}

	public DateTime getDue() {
		return due;
	}

	public void setDue(DateTime due) {
		this.due = due;
	}

	public Runnable getCallback() {
		return callback;
	}

	public void setCallback(Runnable callback) {
		this.callback = callback;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o){
			return true;
		}
		if (!(o instanceof ClockEntry)){
			return false;
		}
		ClockEntry other = (ClockEntry) o;
		return agentId.equals(other.agentId);
	}
	
	@Override
	public int hashCode() {
		return agentId.hashCode();
	}
	
	@Override
	public int compareTo(ClockEntry o) {
		if (due.equals(o.due)){
			return 0;
		}
		return due.compareTo(o.due);
	}
}
