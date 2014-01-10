/*
 * Copyright: Almende B.V. (2014), Rotterdam, The Netherlands
 * License: The Apache Software License, Version 2.0
 */
package com.almende.eve.rpc.annotation;

/**
 * The Enum AccessType.
 *
 * @author Almende
 * 
 * Basic authorization constrains for methods for JSON-RPC access.
 */
public enum AccessType {
	/**
	 * This method(s) have public access through JSON-RPC, not authentication
	 * nor authorization is needed.
	 */
	PUBLIC,
	
	/** This method(s) can only be accessed through JSON-RPC by sender's for which onAccess(SenderUrl,tag) returns true;. */
	PRIVATE,
	
	/** This method(s) can only be accessed through JSON-RPC by the agent itself (or a scheduler task on its behalve). */
	SELF,
	/**
	 * This method(s) can not be accessed through JSON-RPC.
	 */
	UNAVAILABLE
}
