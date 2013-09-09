package com.almende.eve.state;

import com.almende.util.TypeUtil;

public class TypedKey<T> {
	private TypeUtil<T> type;
	private String key;
	
	public TypedKey(String key){
		this.key = key;
		this.type = new TypeUtil<T>(){};
	}

	public TypeUtil<T> getType() {
		return type;
	}

	public String getKey() {
		return key;
	}

}
