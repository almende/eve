package com.almende.eve.lock;

import java.util.HashMap;

import com.almende.eve.agent.Agent;
import com.almende.eve.state.StateEntry;

public class StateLock implements TemporalLock {
	private static final int MINWAIT = 10;
	private final Agent	myAgent;
	private final StateEntry<HashMap<String, Long>>	MYMETHODTIMEOUTS	= 
			new StateEntry<HashMap<String, Long>>("methodTimeouts") {
				@Override
				public HashMap<String, Long> defaultValue() {
					return new HashMap<String, Long>();
				}
			};

	public StateLock(Agent myAgent) {
		this.myAgent = myAgent;
	}
	
	@Override
	public long getLockMillisRemaining(final String semaphoreID) {
		final Long timeout = MYMETHODTIMEOUTS.getValue(myAgent.getState()).get(
				semaphoreID);
		return timeout == null ? -1L : timeout.longValue()
				- System.currentTimeMillis();
	}
	
	@Override
	public boolean lock(final String semaphoreID, final long remainingMS,
			final boolean block) {
		long millis = getLockMillisRemaining(semaphoreID);
		if (block) {
			while (millis > 0L) {
				try {
					synchronized (this) {
						wait(Math.min(MINWAIT, millis));
					}
				} catch (final InterruptedException ignore) {
				}
			}
		} else if (millis > 0L) {
			return false;
		}
		updateLock(semaphoreID, remainingMS);
		return true;
	}
	
	/** */
	protected void updateLock(final String semaphoreID, final long remainingMS) {
		HashMap<String, Long> currentTimeouts, newTimeouts;
		do {
			currentTimeouts = MYMETHODTIMEOUTS.getValue(myAgent.getState());
			newTimeouts = new HashMap<String, Long>(currentTimeouts);
			newTimeouts.put(semaphoreID, remainingMS <= 0L ? Long.valueOf(0L)
					: System.currentTimeMillis() + remainingMS);
		} while (!MYMETHODTIMEOUTS.putValueIfUnchanged(myAgent.getState(),
				newTimeouts, currentTimeouts));
	}
	
	@Override
	public void unlock(final String semaphoreID) {
		updateLock(semaphoreID, -1);
	}
	
	@Override
	public boolean isLocked(final String semaphoreID) {
		return getLockMillisRemaining(semaphoreID) > 0;
	}
}
