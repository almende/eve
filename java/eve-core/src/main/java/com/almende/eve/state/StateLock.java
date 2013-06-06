package com.almende.eve.state;

import java.util.HashMap;

import com.almende.eve.agent.Agent;

public class StateLock implements TemporalLock {
	
	Agent myAgent=null;
	
	public StateLock(Agent myAgent){
		this.myAgent=myAgent;
	}
	
	StateEntry<HashMap<String, Long>> MY_METHOD_TIMEOUTS = new StateEntry<HashMap<String, Long>>(
			"methodTimeouts")
	{
		@Override
		public HashMap<String, Long> defaultValue()
		{
			return new HashMap<String, Long>();
		}
	};

	@Override
	public long getLockMillisRemaining(final String semaphoreID)
	{
		final Long timeout = MY_METHOD_TIMEOUTS.getValue(myAgent.getState()).get(semaphoreID);
		return timeout == null ? -1L : timeout.longValue()
				- System.currentTimeMillis();
	}

	@Override
	public boolean lock(final String semaphoreID, final long remainingMS,
			final boolean block)
	{
		long millis = getLockMillisRemaining(semaphoreID);
		if (block)
			while (millis > 0L)
			{
				try
				{
					// log("Waiting for lock: "+semaphoreID);
					synchronized (this)
					{
						wait(Math.min(10, millis));
					}
				} catch (final InterruptedException ignore)
				{

				}
			}
		else if (millis > 0L)
			return false;

		updateLock(semaphoreID, remainingMS);
		return true;
	}

	/** */
	protected void updateLock(final String semaphoreID, final long remainingMS)
	{
		HashMap<String, Long> currentTimeouts, newTimeouts;
		do
		{
			currentTimeouts = MY_METHOD_TIMEOUTS.getValue(myAgent.getState());
			newTimeouts = new HashMap<String, Long>(currentTimeouts);
			newTimeouts.put(semaphoreID, remainingMS <= 0L ? Long.valueOf(0L)
					: System.currentTimeMillis() + remainingMS);
		} while (!MY_METHOD_TIMEOUTS.putValueIfUnchanged(myAgent.getState(),newTimeouts,
				currentTimeouts));
	}

	@Override
	public void unlock(final String semaphoreID)
	{
		updateLock(semaphoreID, -1);
	}

	@Override
	public boolean isLocked(final String semaphoreID)
	{
		return getLockMillisRemaining(semaphoreID) > 0;
	}
	
}
