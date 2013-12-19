package com.almende.eve.agent;

public class AgentSignal<T> {
	
	public static final String	CREATE	= "create";
	public static final String	INIT	= "init";
	public static final String	DELETE	= "delete";
	public static final String  INVOKE	= "invoke";
	public static final String  RESPOND	= "respond";
	public static final String	ADDTRANSPORTSERVICE	= "addTransportService";
	public static final String	DELTRANSPORTSERVICE	= "removeTransportService";
	public static final String	SETSCHEDULERFACTORY	= "setSchedulerFactory";
	public static final String	SETSTATEFACTORY		= "setStateFactory";
	
	
	private String				event				= "";
	private T					data				= null;
	
	public AgentSignal(String event) {
		this.event = event;
	}
	
	public AgentSignal(String event, T data) {
		this.event = event;
		this.data = data;
	}
	
	public String getEvent() {
		return event;
	}
	
	public void setEvent(String event) {
		this.event = event;
	}
	
	public T getData() {
		return data;
	}
	
	public void setData(T data) {
		this.data = data;
	}
}
