/*
 * Copyright: Almende B.V. (2014), Rotterdam, The Netherlands
 * License: The Apache Software License, Version 2.0
 */
package com.almende.eve.rpc.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

// TODO: replace Access annotation by something more simple (see Jersey JSON
// annotation)
/**
 * The Interface Access.
 *
 * @author Almende
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.METHOD, ElementType.TYPE })
public @interface Access {
	
	/**
	 * Value.
	 *
	 * @return AccessType
	 */
	AccessType value();
	
	/**
	 * Tag.
	 *
	 * @return String
	 */
	String tag() default "";
}
