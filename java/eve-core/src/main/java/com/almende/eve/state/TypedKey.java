package com.almende.eve.state;

import java.lang.reflect.ParameterizedType;

import org.jodah.typetools.TypeResolver;

import com.almende.eve.rpc.jsonrpc.jackson.JOM;
import com.fasterxml.jackson.databind.JavaType;

public abstract class TypedKey<T> {
	private JavaType	valueType;
	private String		key;
	
	public TypedKey(String key) {
		this.key = key;
		this.valueType = JOM.getTypeFactory()
				.constructType(
						((ParameterizedType) TypeResolver.resolveGenericType(
								TypedKey.class, getClass()))
								.getActualTypeArguments()[0]);
	}
	
	public JavaType getType() {
		return valueType;
	}
	
	public String getKey() {
		return key;
	}
	
	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (!(o instanceof TypedKey)) {
			return false;
		}
		TypedKey<?> other = (TypedKey<?>) o;
		return key.equals(other.key) && valueType.equals(other.valueType);
	}
	
	@Override
	public int hashCode() {
		return key.hashCode() & valueType.hashCode();
	}
	
}
