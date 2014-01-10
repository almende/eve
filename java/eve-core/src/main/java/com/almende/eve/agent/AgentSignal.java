/*
 * Copyright: Almende B.V. (2014), Rotterdam, The Netherlands
 * License: The Apache Software License, Version 2.0
 */
package com.almende.eve.agent;

/**
 * The Class AgentSignal.
 *
 * @param <T> the generic type
 */
public class AgentSignal<T> {
	
	/** The Constant CREATE. */
	public static final String	CREATE				= "create";
	
	/** The Constant INIT. */
	public static final String	INIT				= "init";
	
	/** The Constant DESTROY. */
	public static final String	DESTROY				= "destroy";
	
	/** The Constant DELETE. */
	public static final String	DELETE				= "delete";
	
	/** The Constant INVOKE. */
	public static final String	INVOKE				= "invoke";
	
	/** The Constant RESPOND. */
	public static final String	RESPOND				= "respond";
	
	/** The Constant RESPONSE. */
	public static final String	RESPONSE			= "response";
	
	/** The Constant EXCEPTION. */
	public static final String	EXCEPTION			= "exception";
	
	/** The Constant SEND. */
	public static final String	SEND				= "send";
	
	/** The Constant ADDTRANSPORTSERVICE. */
	public static final String	ADDTRANSPORTSERVICE	= "addTransportService";
	
	/** The Constant DELTRANSPORTSERVICE. */
	public static final String	DELTRANSPORTSERVICE	= "removeTransportService";
	
	/** The Constant SETSCHEDULERFACTORY. */
	public static final String	SETSCHEDULERFACTORY	= "setSchedulerFactory";
	
	/** The Constant SETSTATEFACTORY. */
	public static final String	SETSTATEFACTORY		= "setStateFactory";
	
	private String				event				= "";
	private T					data				= null;
	
	/**
	 * Instantiates a new agent signal.
	 *
	 * @param event the event
	 */
	public AgentSignal(final String event) {
		this.event = event;
	}
	
	/**
	 * Instantiates a new agent signal.
	 *
	 * @param event the event
	 * @param data the data
	 */
	public AgentSignal(final String event, final T data) {
		this.event = event;
		this.data = data;
	}
	
	/**
	 * Gets the event.
	 *
	 * @return the event
	 */
	public String getEvent() {
		return event;
	}
	
	/**
	 * Sets the event.
	 *
	 * @param event the new event
	 */
	public void setEvent(final String event) {
		this.event = event;
	}
	
	/**
	 * Gets the data.
	 *
	 * @return the data
	 */
	public T getData() {
		return data;
	}
	
	/**
	 * Sets the data.
	 *
	 * @param data the new data
	 */
	public void setData(final T data) {
		this.data = data;
	}
}
