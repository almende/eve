/*
 * Copyright: Almende B.V. (2014), Rotterdam, The Netherlands
 * License: The Apache Software License, Version 2.0
 */
package com.almende.eve.state;

import java.lang.reflect.Type;
import java.util.Set;

import com.almende.util.TypeUtil;
import com.fasterxml.jackson.databind.JavaType;

/**
 * The Interface State.
 */
public interface State {
	/** key name for agent type. */
	String	KEY_AGENT_TYPE	= "_type";
	
	/**
	 * Inits the State
	 */
	void init();
	
	/**
	 * Destroy's the State
	 */
	void destroy();
	
	/**
	 * Gets the agent id.
	 * 
	 * @return the agent id
	 */
	String getAgentId();
	
	/**
	 * Sets the agent type.
	 * 
	 * @param agentType
	 *            the new agent type
	 */
	void setAgentType(Class<?> agentType);
	
	/**
	 * Gets the agent type.
	 * 
	 * @return the agent type
	 * @throws ClassNotFoundException
	 *             the class not found exception
	 */
	Class<?> getAgentType() throws ClassNotFoundException;
	
	/**
	 * Put.
	 * 
	 * @param key
	 *            the key
	 * @param value
	 *            the value
	 * @return the object
	 */
	Object put(String key, Object value);
	
	/**
	 * Removes the.
	 * 
	 * @param key
	 *            the key
	 * @return the object
	 */
	Object remove(String key);
	
	/**
	 * Put if unchanged.
	 * 
	 * @param key
	 *            the key
	 * @param newVal
	 *            the new val
	 * @param oldVal
	 *            the old val
	 * @return true, if successful
	 */
	boolean putIfUnchanged(String key, Object newVal, Object oldVal);
	
	/**
	 * Contains key.
	 * 
	 * @param key
	 *            the key
	 * @return true, if successful
	 */
	boolean containsKey(String key);
	
	/**
	 * Key set.
	 * 
	 * @return the sets the
	 */
	Set<String> keySet();
	
	/**
	 * Clear.
	 */
	void clear();
	
	/**
	 * Size.
	 * 
	 * @return the int
	 */
	int size();
	
	/**
	 * Gets the.
	 * 
	 * @param <T>
	 *            the generic type
	 * @param key
	 *            the key
	 * @param type
	 *            the type
	 * @return the t
	 */
	<T> T get(String key, Class<T> type);
	
	/**
	 * Gets the.
	 * 
	 * @param <T>
	 *            the generic type
	 * @param key
	 *            the key
	 * @param type
	 *            the type
	 * @return the t
	 */
	<T> T get(String key, Type type);
	
	/**
	 * Gets the.
	 * 
	 * @param <T>
	 *            the generic type
	 * @param key
	 *            the key
	 * @param type
	 *            the type
	 * @return the t
	 */
	<T> T get(String key, JavaType type);
	
	/**
	 * Gets the.
	 * 
	 * @param <T>
	 *            the generic type
	 * @param key
	 *            the key
	 * @param type
	 *            the type
	 * @return the t
	 */
	<T> T get(String key, TypeUtil<T> type);
	
	/**
	 * Gets the.
	 * 
	 * @param <T>
	 *            the generic type
	 * @param key
	 *            the key
	 * @return the t
	 */
	<T> T get(TypedKey<T> key);
}
