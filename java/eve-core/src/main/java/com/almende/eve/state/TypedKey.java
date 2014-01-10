/*
 * Copyright: Almende B.V. (2014), Rotterdam, The Netherlands
 * License: The Apache Software License, Version 2.0
 */
package com.almende.eve.state;

import java.lang.reflect.ParameterizedType;

import org.jodah.typetools.TypeResolver;

import com.almende.eve.rpc.jsonrpc.jackson.JOM;
import com.fasterxml.jackson.databind.JavaType;

/**
 * The Class TypedKey.
 *
 * @param <T> the generic type
 */
public abstract class TypedKey<T> {
	private final JavaType	valueType;
	private final String		key;
	
	/**
	 * Instantiates a new typed key.
	 *
	 * @param key the key
	 */
	public TypedKey(final String key) {
		this.key = key;
		this.valueType = JOM.getTypeFactory()
				.constructType(
						((ParameterizedType) TypeResolver.resolveGenericType(
								TypedKey.class, getClass()))
								.getActualTypeArguments()[0]);
	}
	
	/**
	 * Gets the type.
	 *
	 * @return the type
	 */
	public JavaType getType() {
		return valueType;
	}
	
	/**
	 * Gets the key.
	 *
	 * @return the key
	 */
	public String getKey() {
		return key;
	}
	
	/* (non-Javadoc)
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(final Object o) {
		if (this == o) {
			return true;
		}
		if (!(o instanceof TypedKey)) {
			return false;
		}
		final TypedKey<?> other = (TypedKey<?>) o;
		return key.equals(other.key) && valueType.equals(other.valueType);
	}
	
	/* (non-Javadoc)
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
		return key.hashCode() & valueType.hashCode();
	}
	
}
