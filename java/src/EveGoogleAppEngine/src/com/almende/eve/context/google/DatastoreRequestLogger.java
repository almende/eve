package com.almende.eve.context.google;

import com.almende.eve.context.log.RequestLogger;
import com.almende.eve.context.log.AgentDetailRecord;
import com.google.code.twig.ObjectDatastore;
import com.google.code.twig.annotation.AnnotationObjectDatastore;

public class DatastoreRequestLogger implements RequestLogger {
	@Override
	public void log(AgentDetailRecord record) {
		ObjectDatastore datastore = new AnnotationObjectDatastore();
		datastore.store(record);
	}
}
