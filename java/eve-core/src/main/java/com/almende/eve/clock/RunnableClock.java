package com.almende.eve.clock;

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
	private static final Logger log = Logger.getLogger("RunnableClock");
	static TreeMap<ClockEntry, ClockEntry> timeline = new TreeMap<ClockEntry, ClockEntry>();
	private static ScheduledExecutorService pool = Executors
			.newScheduledThreadPool(50);
	private static ScheduledFuture<?> future = null;

	public void run() {
		synchronized (timeline) {
			while (!timeline.isEmpty()) {
				ClockEntry ce = timeline.firstEntry().getValue();
				DateTime now = DateTime.now();
				if (ce.due.isBefore(now)) {
					timeline.remove(ce);
					pool.execute(ce.callback);
					continue;
				}
				if (future != null) {
					future.cancel(false);
					future = null;
				}
				//log.warning("Delaying next trigger until:"+ce.due);
				long interval = new Interval(now, ce.due).toDurationMillis();
				future = pool.schedule(this, interval, TimeUnit.MILLISECONDS);
				break;
			}
		}
	}

	@Override
	public void requestTrigger(String agentId, DateTime due, Runnable callback) {
		synchronized (timeline) {
			ClockEntry ce = new ClockEntry(agentId, due, callback);
			ClockEntry oldVal = timeline.get(ce);
			if (oldVal == null || oldVal.due.isAfter(due)) {
//				 log.warning("Adding/replacing "+agentId+"'s trigger to:"+due);
				timeline.put(ce, ce);
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
	String agentId;
	DateTime due;
	Runnable callback;

	public ClockEntry(String agentId, DateTime due, Runnable callback) {
		this.agentId = agentId;
		this.due = due;
		this.callback = callback;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o)
			return true;
		if (!(o instanceof ClockEntry))
			return false;
		ClockEntry other = (ClockEntry) o;
		return agentId.equals(other.agentId);
	}

	@Override
	public int hashCode() {
		return agentId.hashCode();
	}

	@Override
	public int compareTo(ClockEntry o) {
		if (due.equals(o.due))
			return 0;
		return due.compareTo(o.due);
	}
}
