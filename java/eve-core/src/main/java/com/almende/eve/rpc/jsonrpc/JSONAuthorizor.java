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
	 * @param senderId
	 * @param functionTag
	 * @return
	 */
	boolean onAccess(String senderId, String functionTag);
	
	/**
	 * Internal method, implementing this method allows adding authorization to
	 * your JSONRPC calls.
	 * All methods annotated with AccessType.PRIVATE will only be called if this
	 * method returns true.
	 * 
	 * @param senderId
	 * @return
	 */
	boolean onAccess(String senderId);
	 
}
