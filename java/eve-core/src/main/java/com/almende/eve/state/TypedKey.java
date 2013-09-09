package com.almende.eve.state;

import com.almende.eve.rpc.jsonrpc.jackson.JOM;
import com.almende.util.ClassUtil;
import com.fasterxml.jackson.databind.JavaType;

public abstract class TypedKey<T> {
	private JavaType valueType;
	private String key;
	
	public TypedKey(String key){
		this.key = key;
		this.valueType = JOM.getTypeFactory().constructType(
				ClassUtil.getTypeArguments(TypedKey.class, getClass()).get(0)
						.getGenericSuperclass());
	}

	public JavaType getType() {
		return valueType;
	}

	public String getKey() {
		return key;
	}

}
