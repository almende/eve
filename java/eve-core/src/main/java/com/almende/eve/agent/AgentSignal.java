package com.almende.eve.agent;

public class AgentSignal<T> {
	
	public static final String	ADDTRANSPORTSERVICE	= "addTransportService";
	public static final String	DELTRANSPORTSERVICE	= "removeTransportService";
	public static final String	SETSCHEDULERFACTORY	= "setSchedulerFactory";
	public static final String	SETSTATEFACTORY		= "setStateFactory";
	
	private String				event				= "";
	private T					service				= null;
	
	public AgentSignal(String event) {
		this.event = event;
	}
	
	public AgentSignal(String event, T service) {
		this.event = event;
		this.service = service;
	}
	
	public String getEvent() {
		return event;
	}
	
	public void setEvent(String event) {
		this.event = event;
	}
	
	public T getService() {
		return service;
	}
	
	public void setService(T service) {
		this.service = service;
	}
}
