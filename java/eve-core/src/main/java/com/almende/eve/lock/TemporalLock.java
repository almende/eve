package com.almende.eve.lock;

public interface TemporalLock {
	/**
	 * @param semaphoreID the lock identifier
	 * @return the milliseconds remaining until the lock times out
	 * @see #lock(String, long)
	 * @see #isLocked(String)
	 * @see #unlock(String)
	 */
	long getLockMillisRemaining(String semaphoreID);

	/**
	 * @param semaphoreID the lock identifier
	 * @param remainingMS the remaining millis of this lock
	 * @param block {@code true} to block {@link Thread#currentThread()} until
	 *        lock becomes available
	 * @return {@code true} if locked was obtained, {@code false} otherwise
	 * @see #getLockMillisRemaining(String)
	 * @see #isLocked(String)
	 * @see #unlock(String)
	 */
	boolean lock(String semaphoreID, long remainingMS, boolean block);

	/**
	 * @param semaphoreID the lock identifier
	 * @see #isLocked(String)
	 * @see #lock(String, long)
	 * @see #getLockMillisRemaining(String)
	 */
	void unlock(String semaphoreID);

	/**
	 * @param semaphoreID the lock identifier
	 * @return {@code true} if locked and not timed out, {@code false} otherwise
	 * @see #lock(String, long)
	 * @see #getLockMillisRemaining(String)
	 * @see #unlock(String)
	 */
	boolean isLocked(String semaphoreID);
}
