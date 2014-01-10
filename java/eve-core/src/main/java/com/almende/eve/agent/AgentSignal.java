package com.almende.eve.agent;

public class AgentSignal<T> {
	
	public static final String	CREATE				= "create";
	public static final String	INIT				= "init";
	public static final String	DESTROY				= "destroy";
	public static final String	DELETE				= "delete";
	public static final String	INVOKE				= "invoke";
	public static final String	RESPOND				= "respond";
	public static final String	RESPONSE			= "response";
	public static final String	EXCEPTION			= "exception";
	public static final String	SEND				= "send";
	public static final String	ADDTRANSPORTSERVICE	= "addTransportService";
	public static final String	DELTRANSPORTSERVICE	= "removeTransportService";
	public static final String	SETSCHEDULERFACTORY	= "setSchedulerFactory";
	public static final String	SETSTATEFACTORY		= "setStateFactory";
	
	private String				event				= "";
	private T					data				= null;
	
	public AgentSignal(final String event) {
		this.event = event;
	}
	
	public AgentSignal(final String event, final T data) {
		this.event = event;
		this.data = data;
	}
	
	public String getEvent() {
		return event;
	}
	
	public void setEvent(final String event) {
		this.event = event;
	}
	
	public T getData() {
		return data;
	}
	
	public void setData(final T data) {
		this.data = data;
	}
}
