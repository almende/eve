/*
 * Copyright: Almende B.V. (2014), Rotterdam, The Netherlands
 * License: The Apache Software License, Version 2.0
 */
package com.almende.eve.rpc.jsonrpc;

/**
 * The Interface JSONAuthorizor.
 */
public interface JSONAuthorizor {
	
	/**
	 * Internal method, implementing this method allows adding authorization to
	 * your JSONRPC calls.
	 * All methods annotated with AccessType.PRIVATE will only be called if this
	 * method returns true.
	 * The function_tag parameter can be used to check against
	 *
	 * @param senderUrl the sender url
	 * @param functionTag the function tag
	 * @return true, if successful
	 * @Access(tag="foobar") annotation on the called method.
	 * ( e.g. add roles to methods )
	 */
	boolean onAccess(String senderUrl, String functionTag);
	
	/**
	 * Internal method, implementing this method allows adding authorization to
	 * your JSONRPC calls.
	 * All methods annotated with AccessType.PRIVATE will only be called if this
	 * method returns true.
	 *
	 * @param senderUrl the sender url
	 * @return true, if successful
	 */
	boolean onAccess(String senderUrl);
	
	/**
	 * This method should check if the sender equals the recipient.
	 * All methods annotated with AccessType.SELF will only be called if this
	 * method returns true.
	 * 
	 * Default implementation checks all urls for senderUrl and allows all "web"
	 * sources.
	 *
	 * @param senderUrl the sender url
	 * @return true, if is self
	 */
	boolean isSelf(String senderUrl);
}
