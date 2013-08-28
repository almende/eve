package com.almende.eve.rpc.jsonrpc;

public interface JSONAuthorizor {
	/**
	 * Internal method, implementing this method allows adding authorization to
	 * your JSONRPC calls.
	 * All methods annotated with AccessType.PRIVATE will only be called if this
	 * method returns true.
	 * The function_tag parameter can be used to check against
	 * 
	 * @Access(tag="foobar") annotation on the called method.
	 *                       ( e.g. add roles to methods )
	 * 
	 * @param senderUrl
	 * @param functionTag
	 * @return
	 */
	boolean onAccess(String senderUrl, String functionTag);
	
	/**
	 * Internal method, implementing this method allows adding authorization to
	 * your JSONRPC calls.
	 * All methods annotated with AccessType.PRIVATE will only be called if this
	 * method returns true.
	 * 
	 * @param senderUrl
	 * @return
	 */
	boolean onAccess(String senderUrl);
	
	/**
	 * This method should check if the sender equals the recipient.
	 * All methods annotated with AccessType.SELF will only be called if this method returns true.
	 * 
	 * Default implementation checks all urls for senderUrl and allows all "web" sources.
	 * 
	 * @param senderUrl
	 * @return
	 */
	boolean ifSelf(String senderUrl);
}
