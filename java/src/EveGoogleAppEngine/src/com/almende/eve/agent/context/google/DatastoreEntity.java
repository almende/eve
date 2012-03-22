package com.almende.eve.agent.context.google;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import com.almende.eve.agent.context.AgentContext;
import com.google.appengine.api.datastore.Blob;
import com.google.code.twig.annotation.Id;
import com.google.code.twig.annotation.Type;

// TODO: it is not so nice needing this DataStoreEntity, which is a duplicate 
//       of SimpleContext, except that it has some annotations for TwigPersist
@SuppressWarnings("serial")
public class DatastoreEntity implements AgentContext, Serializable {
	@Id private String id = null;
	@Type(Blob.class) private Map<String, Object> properties = 
		new HashMap<String, Object>();
	
	protected DatastoreEntity () {}
	
	protected DatastoreEntity (String id) {
		setId(id);
	}

	@Override
	public Object get(String key) {
		return properties.get(key);
	}

	@Override
	public void put(String key, Object value) {
		properties.put(key, value);
	}

	@Override
	public boolean has(String key) {
		return properties.containsKey(key);
	}

	@Override
	public void setId(String id) {
		this.id = id;
	}

	@Override
	public String getId() {
		return id;
	}

	@Override
	public void setServletUrlFromRequest(HttpServletRequest req) {
		// this class does not utilize the request
	}
	
	@Override
	public void setAgentClass(String agentClass) {
		// this class does not utilize the request
	}

	@Override
	public String getAgentUrl() {
		return null;
	}
}	