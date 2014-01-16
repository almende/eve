/*
 * Copyright: Almende B.V. (2014), Rotterdam, The Netherlands
 * License: The Apache Software License, Version 2.0
 */
package com.almende.util;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.logging.Logger;

import org.jodah.typetools.TypeResolver;

import com.almende.eve.rpc.jsonrpc.jackson.JOM;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * The Class TypeUtil.
 *
 * @param <T> the generic type
 */
public abstract class TypeUtil<T> {
	static final Logger		LOG	= Logger.getLogger(TypeUtil.class.getName());
	private final JavaType	valueType;
	
	/**
	 * Usage example: <br>
	 * 
	 * TypeUtil&lt;TreeSet&lt;TaskEntry>> injector = new
	 * TypeUtil&lt;TreeSet&lt;TaskEntry>>(){};<br>
	 * TreeSet&lt;TaskEntry> value = injector.inject(Treeset_with_tasks);<br>
	 * <br>
	 * Note the trivial anonymous class declaration, extending abstract class
	 * TypeUtil...
	 */
	public TypeUtil() {
		this.valueType = JOM.getTypeFactory()
				.constructType(
						((ParameterizedType) TypeResolver.resolveGenericType(
								TypeUtil.class, getClass()))
								.getActualTypeArguments()[0]);
	}
	
	/**
	 * Gets the type.
	 *
	 * @return the {@link TypeUtil} value's type
	 */
	public Type getType() {
		return this.valueType;
	}
	
	/**
	 * Inject.
	 *
	 * @param value the value
	 * @return the t
	 */
	public T inject(final Object value) {
		return inject(value, valueType);
	}
	
	/**
	 * Inject.
	 *
	 * @param <T> the generic type
	 * @param value the value
	 * @param type the type
	 * @return the t
	 */
	public static <T> T inject(final Object value, final Class<T> type) {
		return inject(value, JOM.getTypeFactory().constructType(type));
	}
	
	/**
	 * Inject.
	 *
	 * @param <T> the generic type
	 * @param value the value
	 * @param type the type
	 * @return the t
	 */
	public static <T> T inject(final Object value, final Type type) {
		return inject(value, JOM.getTypeFactory().constructType(type));
	}
	
	/**
	 * Inject.
	 *
	 * @param <T> the generic type
	 * @param value the value
	 * @param fullType the full type
	 * @return the t
	 */
	@SuppressWarnings("unchecked")
	public static <T> T inject(final Object value, final JavaType fullType) {
		if (value == null) {
			return null;
		}
		if (fullType.hasRawClass(Void.class)) {
			return null;
		}
		final ObjectMapper mapper = JOM.getInstance();
		if (value instanceof JsonNode) {
			if (((JsonNode) value).isNull()) {
				return null;
			}
			try {
				return mapper.convertValue(value, fullType);
			} catch (final Exception e) {
				ClassCastException cce = new ClassCastException("Failed to convert value:" + value
						+ " -----> " + fullType);
				cce.initCause(e);
				throw cce;
			}
		}
		if (fullType.getRawClass().isAssignableFrom(value.getClass())) {
			return (T) value;
		} else {
			throw new ClassCastException(value.getClass().getCanonicalName()
					+ " can't be converted to: "
					+ fullType.getRawClass().getCanonicalName());
		}
	}
	
}
