/*
 * Copyright: Almende B.V. (2014), Rotterdam, The Netherlands
 * License: The Apache Software License, Version 2.0
 */
package com.almende.eve.lock;

/**
 * The Interface TemporalLock.
 */
public interface TemporalLock {
	
	/**
	 * Gets the lock millis remaining.
	 * 
	 * @param semaphoreID
	 *            the lock identifier
	 * @return the milliseconds remaining until the lock times out
	 * @see #lock(String, long, boolean)
	 * @see #isLocked(String)
	 * @see #unlock(String)
	 */
	long getLockMillisRemaining(String semaphoreID);
	
	/**
	 * Lock.
	 * 
	 * @param semaphoreID
	 *            the lock identifier
	 * @param remainingMS
	 *            the remaining millis of this lock
	 * @param block
	 *            the block
	 * @return {@code true} if locked was obtained, {@code false} otherwise
	 *         {@code true} to block {@link Thread#currentThread()} until
	 *         lock becomes available
	 * @see #getLockMillisRemaining(String)
	 * @see #isLocked(String)
	 * @see #unlock(String)
	 */
	boolean lock(String semaphoreID, long remainingMS, boolean block);
	
	/**
	 * Unlock.
	 * 
	 * @param semaphoreID
	 *            the lock identifier
	 * @see #isLocked(String)
	 * @see #lock(String, long, boolean)
	 * @see #getLockMillisRemaining(String)
	 */
	void unlock(String semaphoreID);
	
	/**
	 * Checks if is locked.
	 * 
	 * @param semaphoreID
	 *            the lock identifier
	 * @return {@code true} if locked and not timed out, {@code false} otherwise
	 * @see #lock(String, long, boolean)
	 * @see #getLockMillisRemaining(String)
	 * @see #unlock(String)
	 */
	boolean isLocked(String semaphoreID);
}
