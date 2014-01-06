package com.almende.eve.monitor.impl;

import org.joda.time.DateTime;

import com.almende.eve.monitor.CacheInterface;
import com.fasterxml.jackson.databind.node.ObjectNode;

public class Cache implements CacheInterface {
	private static final long	serialVersionUID	= 2159298023743341010L;
	private DateTime	stored	= null;
	private Object		value	= null;
	
	@Override
	public boolean filter(ObjectNode params) {
		if (!params.has("maxAge") || !params.get("maxAge").isInt()
				|| stored == null) {
			return false;
		}
		return stored.plusMillis(params.get("maxAge").intValue()).isAfterNow();
	}
	
	@Override
	public void store(Object value) {
		this.stored = DateTime.now();
		this.value = value;
	}
	
	@Override
	public Object get() {
		return value;
	}
	
	@Override
	public DateTime getStored() {
		return stored;
	}

	@Override
	public void setStored(DateTime stored) {
		this.stored = stored;
	}
	
	@Override
	public Object getValue() {
		return value;
	}

	@Override
	public void setValue(Object value) {
		this.value = value;
	}


}
