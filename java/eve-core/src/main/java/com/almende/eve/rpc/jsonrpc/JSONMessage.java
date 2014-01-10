/*
 * Copyright: Almende B.V. (2014), Rotterdam, The Netherlands
 * License: The Apache Software License, Version 2.0
 */
package com.almende.eve.rpc.jsonrpc;

import java.io.Serializable;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * The Class JSONMessage.
 */
public abstract class JSONMessage implements Serializable {
	private static final long		serialVersionUID	= -3324436908445901707L;
	protected static final String	JSONRPC				= "jsonrpc";
	protected static final String	ID					= "id";
	protected static final String	METHOD				= "method";
	protected static final String	PARAMS				= "params";
	protected static final String	ERROR				= "error";
	protected static final String	RESULT				= "result";
	protected static final String	URL					= "url";
	protected static final String	CALLBACK			= "callback";
	protected static final String	VERSION				= "2.0";
	
	/**
	 * Gets the id.
	 * 
	 * @return the id
	 */
	public abstract JsonNode getId();
	
}
