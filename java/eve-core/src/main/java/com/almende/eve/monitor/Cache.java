package com.almende.eve.monitor;

import org.joda.time.DateTime;

import com.fasterxml.jackson.databind.node.ObjectNode;

public class Cache implements ResultMonitorConfigType {
	DateTime stored= null;
	Object value = null;
	
	public boolean filter(ObjectNode params){
		if (!params.has("maxAge") || !params.get("maxAge").isInt() || stored == null) return false;
		return stored.plusMillis(params.get("maxAge").intValue()).isAfterNow();
	}
	
	public void store(Object value){
		this.stored = DateTime.now();
		this.value = value;
	}
	
	public Object get(){
		return value;
	}
	public DateTime getStored(){
		return stored;
	}
}
