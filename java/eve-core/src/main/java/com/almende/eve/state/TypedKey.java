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
	
}
