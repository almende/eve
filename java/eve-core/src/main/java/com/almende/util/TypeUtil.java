package com.almende.util;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.logging.Logger;

import org.jodah.typetools.TypeResolver;

import com.almende.eve.rpc.jsonrpc.jackson.JOM;
import com.almende.eve.state.StateEntry;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

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
	
	/** @return the {@link StateEntry} value's type */
	public Type getType() {
		return this.valueType;
	}
	
	public T inject(Object value) {
		return inject(value, valueType);
	}
	
	public static <T> T inject(Object value, Class<T> type) {
		return inject(value, JOM.getTypeFactory().constructType(type));
	}
	
	public static <T> T inject(Object value, Type type) {
		return inject(value, JOM.getTypeFactory().constructType(type));
	}
	
	@SuppressWarnings("unchecked")
	public static <T> T inject(Object value, JavaType fullType) {
		if (value == null) {
			return null;
		}
		if (fullType.hasRawClass(Void.class)) {
			return null;
		}
		ObjectMapper mapper = JOM.getInstance();
		if (value instanceof JsonNode) {
			if (((JsonNode) value).isNull()) {
				return null;
			}
			try {
				return mapper.convertValue(value, fullType);
			} catch (Exception e) {
				throw new ClassCastException("Failed to convert value:" + value + " -----> " + fullType);
			}
		}
		if (fullType.getRawClass().isAssignableFrom(value.getClass())){
			return (T) value;
		} else {
			throw new ClassCastException(value.getClass().getCanonicalName() + " can't be converted to: "+ fullType.getRawClass().getCanonicalName());
		}
	}
	
}
