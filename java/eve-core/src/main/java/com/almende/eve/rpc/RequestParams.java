/*
 * Copyright: Almende B.V. (2014), Rotterdam, The Netherlands
 * License: The Apache Software License, Version 2.0
 */
package com.almende.eve.rpc;

import java.lang.annotation.Annotation;
import java.util.HashMap;
import java.util.Map;

// TODO: rework the RequestParams class to something more generic
/**
 * The Class RequestParams.
 */
public class RequestParams {
	/**
	 * Map with full class path of an annotation type as key,
	 * and an arbitrary object as value.
	 */
	private final Map<String, Object>	params	= new HashMap<String, Object>();
	
	/**
	 * Instantiates a new request params.
	 */
	public RequestParams() {
	}
	
	/**
	 * Put.
	 * 
	 * @param annotationType
	 *            the annotation type
	 * @param value
	 *            the value
	 */
	public void put(final Class<?> annotationType, final Object value) {
		params.put(annotationType.getName(), value);
	}
	
	/**
	 * Gets the.
	 * 
	 * @param annotationType
	 *            the annotation type
	 * @return the object
	 */
	public Object get(final Class<?> annotationType) {
		return params.get(annotationType.getName());
	}
	
	/**
	 * Checks for.
	 * 
	 * @param annotationType
	 *            the annotation type
	 * @return true, if successful
	 */
	public boolean has(final Class<?> annotationType) {
		return params.containsKey(annotationType.getName());
	}
	
	/**
	 * Gets the.
	 * 
	 * @param annotation
	 *            the annotation
	 * @return the object
	 */
	public Object get(final Annotation annotation) {
		return get(annotation.annotationType());
	}
	
	/**
	 * Checks for.
	 * 
	 * @param annotation
	 *            the annotation
	 * @return true, if successful
	 */
	public boolean has(final Annotation annotation) {
		return has(annotation.annotationType());
	}
	
}
