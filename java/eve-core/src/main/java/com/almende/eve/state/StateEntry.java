package com.almende.eve.state;

import java.io.Serializable;
import java.lang.reflect.Type;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.almende.eve.rpc.jsonrpc.jackson.JOM;
import com.almende.util.ClassUtil;
import com.almende.util.TypeUtil;
import com.fasterxml.jackson.databind.JavaType;

/**
 * {@link StateEntry} wraps agent state entry key and provides respective value
 * type meta data
 * 
 * @date $Date: 2013-05-28 17:31:07 +0200 (Tue, 28 May 2013) $
 * @version $Revision: 911 $ $Author: a4g.almende $
 * @author <a href="mailto:rick@almende.org">Rick van Krevelen</a>
 * 
 * @param <T> the type of value stored in this state entry
 */
public abstract class StateEntry<T extends Serializable>
{
	
	/** */
	private static final Logger LOG = Logger.getLogger(StateEntry.class.getCanonicalName());

	/** */
	private final String key;

	/** */
	private final JavaType valueType;

	/**
	 * Constructs a {@link StateEntry} with specified {@code key} and
	 * {@code valueType} meta information
	 * 
	 * @param key
	 * @param valueType
	 */
	public StateEntry(final String key)
	{
		this.key = key;
		this.valueType = JOM.getTypeFactory().constructType(
				ClassUtil.getTypeArguments(StateEntry.class, getClass()).get(0)
						.getGenericSuperclass());
	}

	/** @return the {@link StateEntry} key's {@link String} value */
	public String getKey()
	{
		return this.key;
	}

	/** @return the {@link StateEntry} value's type */
	public Type getValueType()
	{
		return this.valueType;
	}

	/** @return the value to persist/provide if none exists yet */
	public T defaultValue()
	{
		return null;
	}

	/**
	 * @param state the {@link State} to search for this {@link StateEntry}
	 * @return {@code true} is specified {@code state} contains this
	 *         {@link StateEntry}'s key, {@code false} otherwise
	 * @see java.util.Map#containsKey(Object)
	 */
	public boolean exists(final State state)
	{
		return state.containsKey(getKey());
	}

	/**
	 * @param state the {@link State} from which to retrieve this
	 *        {@code StateEntry}'s value
	 * @return the value retrieved from specified {@code state} or {@code null}
	 *         if none exists
	 * @see java.util.Map#get(Object)
	 */
	public T getValue(final State state)
	{
		try
		{
			return state.get(getKey(),valueType);
		} catch (final ClassCastException e)
		{
			LOG.log(Level.WARNING,"Problem casting agent's state value, key: " + key, e);
			return null;
		}
	}

	/**
	 * @param state the {@link State} to update with specified {@code value}
	 * @param value the new value for this {@link StateEntry} in specified
	 *        {@code state}
	 * @return the previous value or {@code null} if none existed
	 * @see java.util.Map#put(String, Object)
	 */
	public T putValue(final State state, final T value)
	{
		try
		{
			return TypeUtil.inject(state.put(getKey(), value), valueType);
		} catch (final ClassCastException e)
		{
			LOG.log(Level.WARNING,"Problem casting agent's previous state value, key: "
					+ key, e);
			return null;
		}
	}

	/**
	 * @param newValue the new value for this {@link StateEntry} in specified
	 *        {@code state}
	 * @param currentValue the current/precondition value for this
	 *        {@link StateEntry} in specified {@code state}
	 * @param state the {@link State} to update with specified {@code value}
	 * @return the previous value or {@code null} if none existed
	 * @see java.util.Map#put(String, Object)
	 */
	public boolean putValueIfUnchanged(final State state, final T newValue, final T currentValue)
	{
		return state.putIfUnchanged(getKey(), newValue, currentValue);
	}
	
	
}
