package com.almende.eve.rpc;

import java.lang.annotation.Annotation;
import java.util.HashMap;
import java.util.Map;

public class SystemParams {
	public SystemParams() {
	}
	
	public void put(Class<?> annotationType, Object value) {
		params.put(annotationType.getName(), value);
	}
	
	public Object get(Class<?> annotationType) {
		return params.get(annotationType.getName());
	}
	
	public boolean has(Class<?> annotationType) {
		return params.containsKey(annotationType.getName());
	}
	
	public Object get(Annotation annotation) {
		return get(annotation.annotationType());
	}
	
	public boolean has(Annotation annotation) {
		return has(annotation.annotationType());
	}
	
	// map with full class path of an annotation type as key,
	// and an arbitrary object as value
	Map<String, Object> params = new HashMap<String, Object>();
}
