package com.almende.eve.monitor;

import org.joda.time.DateTime;

import com.fasterxml.jackson.databind.node.ObjectNode;

public interface CacheInterface extends ResultMonitorConfigType {

	void setValue(Object value);

	Object getValue();

	void setStored(DateTime stored);

	DateTime getStored();

	Object get();

	void store(Object value);

	boolean filter(ObjectNode params);
	
}
