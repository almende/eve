package com.almende.eve.state.google;

import com.almende.eve.state.log.AgentDetailRecord;
import com.almende.eve.state.log.RequestLogger;
import com.google.code.twig.ObjectDatastore;
import com.google.code.twig.annotation.AnnotationObjectDatastore;

public class DatastoreRequestLogger implements RequestLogger {
	@Override
	public void log(AgentDetailRecord record) {
		ObjectDatastore datastore = new AnnotationObjectDatastore();
		datastore.store(record);
	}
}
