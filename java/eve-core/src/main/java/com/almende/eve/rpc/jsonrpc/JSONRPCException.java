/*
 * Copyright: Almende B.V. (2014), Rotterdam, The Netherlands
 * License: The Apache Software License, Version 2.0
 */
package com.almende.eve.rpc.jsonrpc;

import java.util.logging.Level;
import java.util.logging.Logger;

import com.almende.eve.rpc.jsonrpc.jackson.JOM;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * The Class JSONRPCException.
 */
public class JSONRPCException extends Exception {
	private static final long	serialVersionUID	= -4258336566828038603L;
	private static final Logger	LOG					= Logger.getLogger(JSONRPCException.class
															.getCanonicalName());
	private final ObjectNode	error				= JOM.createObjectNode();
	static final String			CODE_S				= "code";
	static final String			MESSAGE_S			= "message";
	static final String			DATA_S				= "data";
	
	/**
	 * The Enum CODE.
	 */
	public static enum CODE {
		
		/** The unknown error. */
		UNKNOWN_ERROR,
		/** The parse error. */
		PARSE_ERROR,
		/** The invalid request. */
		INVALID_REQUEST,
		/** The remote exception. */
		REMOTE_EXCEPTION,
		/** The method not found. */
		METHOD_NOT_FOUND,
		/** The invalid params. */
		INVALID_PARAMS,
		/** The internal error. */
		INTERNAL_ERROR,
		/** The not found. */
		NOT_FOUND,
		/** The unauthorized. */
		UNAUTHORIZED
	};
	
	/**
	 * Instantiates a new jSONRPC exception.
	 */
	public JSONRPCException() {
		super();
		init(CODE.UNKNOWN_ERROR, null, null);
	}
	
	/**
	 * Instantiates a new jSONRPC exception.
	 * 
	 * @param code
	 *            the code
	 */
	public JSONRPCException(final CODE code) {
		super();
		init(code, null, null);
	}
	
	/**
	 * Instantiates a new jSONRPC exception.
	 * 
	 * @param code
	 *            the code
	 * @param description
	 *            the description
	 */
	public JSONRPCException(final CODE code, final String description) {
		super(description);
		init(code, description, null);
	}
	
	/**
	 * Instantiates a new jSONRPC exception.
	 * 
	 * @param code
	 *            the code
	 * @param description
	 *            the description
	 * @param t
	 *            the t
	 */
	public JSONRPCException(final CODE code, final String description,
			final Throwable t) {
		super(description, t);
		init(code, description, t);
	}
	
	/**
	 * Instantiates a new jSONRPC exception.
	 * 
	 * @param error
	 *            the error
	 */
	public JSONRPCException(final JSONRPCException error) {
		super(error);
		if (error != null) {
			setCode(error.getCode());
			setMessage(error.getMessage());
			if (error.hasData()) {
				setData(error.getData());
			}
		} else {
			init(CODE.UNKNOWN_ERROR, null, null);
		}
	}
	
	/**
	 * Instantiates a new jSONRPC exception.
	 * 
	 * @param message
	 *            the message
	 */
	public JSONRPCException(final String message) {
		super(message);
		setCode(0);
		setMessage(message);
	}
	
	/**
	 * Instantiates a new jSONRPC exception.
	 * 
	 * @param message
	 *            the message
	 * @param t
	 *            the t
	 */
	public JSONRPCException(final String message, final Throwable t) {
		super(message, t);
		setCode(0);
		setMessage(message);
		try {
			setData(JOM.getInstance().writeValueAsString(t));
		} catch (final JsonProcessingException e) {
			LOG.log(Level.SEVERE, "Failed to init JSONRPCException!", e);
		}
	}
	
	/**
	 * Instantiates a new jSONRPC exception.
	 * 
	 * @param code
	 *            the code
	 * @param message
	 *            the message
	 */
	public JSONRPCException(final Integer code, final String message) {
		super(message);
		setCode(code);
		setMessage(message);
	}
	
	/**
	 * Instantiates a new jSONRPC exception.
	 * 
	 * @param code
	 *            the code
	 * @param message
	 *            the message
	 * @param data
	 *            the data
	 */
	public JSONRPCException(final Integer code, final String message,
			final Object data) {
		super(message);
		setCode(code);
		setMessage(message);
		setData(data);
	}
	
	/**
	 * Instantiates a new jSONRPC exception.
	 * 
	 * @param exception
	 *            the exception
	 */
	public JSONRPCException(final ObjectNode exception) {
		super();
		if (exception != null && !exception.isNull()) {
			// set code, message, and optional data
			if (exception.has(CODE_S)) {
				setCode(exception.get(CODE_S).asInt());
			}
			if (exception.has(MESSAGE_S)) {
				setMessage(exception.get(MESSAGE_S).asText());
				
			}
			if (exception.has(DATA_S)) {
				setData(exception.get(DATA_S));
				initCause(JOM.getInstance().convertValue(exception.get(DATA_S),
						Exception.class));
			}
		}
	}
	
	/**
	 * Inits the.
	 * 
	 * @param code
	 *            the code
	 * @param message
	 *            the message
	 * @param t
	 *            the t
	 */
	private void init(final CODE code, final String message, final Throwable t) {
		switch (code) {
			case UNKNOWN_ERROR:
				setCode(-32000);
				setMessage("Unknown error");
				break;
			case PARSE_ERROR:
				setCode(-32700);
				setMessage("Parse error");
				break;
			case INVALID_REQUEST:
				setCode(-32600);
				setMessage("Invalid request");
				break;
			case REMOTE_EXCEPTION:
				setCode(-32500);
				setMessage("Remote application error");
				break;
			case METHOD_NOT_FOUND:
				setCode(-32601);
				setMessage("Method not found");
				break;
			case INVALID_PARAMS:
				setCode(-32602);
				setMessage("Invalid params");
				break;
			case INTERNAL_ERROR:
				setCode(-32603);
				setMessage("Internal error");
				break;
			case NOT_FOUND:
				setCode(404);
				setMessage("Not found");
				break;
			case UNAUTHORIZED:
				setCode(-32401);
				setMessage("Unauthorized");
				break;
		}
		
		if (message != null) {
			error.put(MESSAGE_S, message);
		}
	}
	
	/**
	 * Sets the code.
	 * 
	 * @param code
	 *            the new code
	 */
	public final void setCode(final int code) {
		error.put(CODE_S, code);
	}
	
	/**
	 * Gets the code.
	 * 
	 * @return the code
	 */
	public int getCode() {
		return error.get(CODE_S).asInt();
	}
	
	/**
	 * Sets the message.
	 * 
	 * @param message
	 *            the new message
	 */
	public final void setMessage(final String message) {
		error.put(MESSAGE_S, message != null ? message : "");
	}
	
	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Throwable#getMessage()
	 */
	@Override
	public String getMessage() {
		return error.get(MESSAGE_S).asText();
	}
	
	/**
	 * Sets the data.
	 * 
	 * @param data
	 *            the new data
	 */
	public final void setData(final Object data) {
		final ObjectMapper mapper = JOM.getInstance();
		error.put(DATA_S,
				data != null ? mapper.convertValue(data, JsonNode.class) : null);
	}
	
	/**
	 * Gets the data.
	 * 
	 * @return the data
	 */
	public Object getData() {
		return error.get(DATA_S);
	}
	
	/**
	 * Checks for data.
	 * 
	 * @return true, if successful
	 */
	public boolean hasData() {
		return error.has(DATA_S);
	}
	
	/**
	 * Gets the object node.
	 * 
	 * @return the object node
	 */
	public ObjectNode getObjectNode() {
		return error;
	}
	
	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Throwable#toString()
	 */
	@Override
	public String toString() {
		try {
			return JOM.getInstance().writeValueAsString(this);
		} catch (final JsonProcessingException e) {
			LOG.log(Level.SEVERE, "Couldn't JSON serialize JSONRPCException!",
					e);
		}
		return this.getClass().getCanonicalName() + ": "
				+ error.get("message").textValue();
	}
	
}