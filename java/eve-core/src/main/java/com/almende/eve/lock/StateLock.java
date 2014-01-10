/*
 * Copyright: Almende B.V. (2014), Rotterdam, The Netherlands
 * License: The Apache Software License, Version 2.0
 */
package com.almende.eve.lock;

import java.util.HashMap;

import com.almende.eve.agent.Agent;
import com.almende.eve.state.StateEntry;

/**
 * The Class StateLock.
 */
public class StateLock implements TemporalLock {
	private static final int						MINWAIT				= 10;
	private final Agent								myAgent;
	private final StateEntry<HashMap<String, Long>>	myMethodTimeouts	= new StateEntry<HashMap<String, Long>>(
																				"methodTimeouts") {
																			@Override
																			public HashMap<String, Long> defaultValue() {
																				return new HashMap<String, Long>();
																			}
																		};
	
	/**
	 * Instantiates a new state lock.
	 * 
	 * @param myAgent
	 *            the my agent
	 */
	public StateLock(final Agent myAgent) {
		this.myAgent = myAgent;
	}
	
	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * com.almende.eve.lock.TemporalLock#getLockMillisRemaining(java.lang.String
	 * )
	 */
	@Override
	public long getLockMillisRemaining(final String semaphoreID) {
		final Long timeout = myMethodTimeouts.getValue(myAgent.getState()).get(
				semaphoreID);
		return timeout == null ? -1L : timeout.longValue()
				- System.currentTimeMillis();
	}
	
	/*
	 * (non-Javadoc)
	 * 
	 * @see com.almende.eve.lock.TemporalLock#lock(java.lang.String, long,
	 * boolean)
	 */
	@Override
	public boolean lock(final String semaphoreID, final long remainingMS,
			final boolean block) {
		final long millis = getLockMillisRemaining(semaphoreID);
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
	
	/**
	 * Update lock.
	 * 
	 * @param semaphoreID
	 *            the semaphore id
	 * @param remainingMS
	 *            the remaining ms
	 */
	protected void updateLock(final String semaphoreID, final long remainingMS) {
		HashMap<String, Long> currentTimeouts, newTimeouts;
		do {
			currentTimeouts = myMethodTimeouts.getValue(myAgent.getState());
			newTimeouts = new HashMap<String, Long>(currentTimeouts);
			newTimeouts.put(semaphoreID, remainingMS <= 0L ? Long.valueOf(0L)
					: System.currentTimeMillis() + remainingMS);
		} while (!myMethodTimeouts.putValueIfUnchanged(myAgent.getState(),
				newTimeouts, currentTimeouts));
	}
	
	/*
	 * (non-Javadoc)
	 * 
	 * @see com.almende.eve.lock.TemporalLock#unlock(java.lang.String)
	 */
	@Override
	public void unlock(final String semaphoreID) {
		updateLock(semaphoreID, -1);
	}
	
	/*
	 * (non-Javadoc)
	 * 
	 * @see com.almende.eve.lock.TemporalLock#isLocked(java.lang.String)
	 */
	@Override
	public boolean isLocked(final String semaphoreID) {
		return getLockMillisRemaining(semaphoreID) > 0;
	}
}
