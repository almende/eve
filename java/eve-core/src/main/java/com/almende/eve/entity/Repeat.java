package com.almende.eve.entity;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.almende.eve.agent.Agent;
import com.almende.eve.agent.AgentFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;

public class Repeat implements Serializable {
	private static final long						serialVersionUID	= -6738643681425840533L;
	
	public String									id;
	public String									agentId;
	public String									url;
	public String									method;
	public ObjectNode								params;
	public String									callbackMethod;
	public List<String>								schedulerIds		= new ArrayList<String>();
	public List<String>								remoteIds			= new ArrayList<String>();
	public String									cacheType;
	
	transient private static HashMap<String, Cache>	caches				= new HashMap<String, Cache>();
	
	public <T> Repeat(String agentId, String url, String method, ObjectNode params,
			String callbackMethod) {
		this.id = UUID.randomUUID().toString();
		this.agentId = agentId;
		this.url = url;
		this.method = method;
		this.params = params;
		this.callbackMethod = callbackMethod;
	}
	
	public boolean hasCache() {
		return cacheType != null;
	}
	
	public void addCache(Cache config) {
		cacheType = config.getClass().getName();
		synchronized (caches) {
			caches.put(id, config);
		}
	}
	
	public Cache getCache() {
		synchronized (caches) {
			return caches.get(id);
		}
	}
	
	public void addPoll(Poll config) {
		AgentFactory factory = AgentFactory.getInstance();
		
		try {
			Agent agent = factory.getAgent(agentId);
			String taskId = config.init(id, agent);
			schedulerIds.add(taskId);
		} catch (Exception e){
			System.err.println("Couldn't init polling!");
			e.printStackTrace();
		}
	}
	
	public void addPush(Push config) {
		
	}
	
	public void store() {
		AgentFactory factory = AgentFactory.getInstance();
		
		try {
			Agent agent = factory.getAgent(agentId);
			@SuppressWarnings("unchecked")
			Map<String, Repeat> repeats = (Map<String, Repeat>) agent
					.getState().get("_repeats");
			Map<String, Repeat> newRepeats = new HashMap<String, Repeat>();
			if (repeats != null) {
				newRepeats.putAll(repeats);
			}
			newRepeats.put(id, this);
			
			if (!agent.getState().putIfUnchanged("_repeats",
					(Serializable) newRepeats, (Serializable) repeats)) {
				store(); // recursive retry.
			}
		} catch (Exception e) {
			System.err.println("Couldn't find repeats:" + agentId + "." + id);
		}
	}
	
	public static Repeat getRepeatById(String agentId, String id) {
		AgentFactory factory = AgentFactory.getInstance();
		
		try {
			Agent agent = factory.getAgent(agentId);
			@SuppressWarnings("unchecked")
			Map<String, Repeat> repeats = (Map<String, Repeat>) agent
					.getState().get("_repeats");
			if (repeats == null) {
				repeats = new HashMap<String, Repeat>();
			}
			Repeat result = repeats.get(id);
			if (result != null && !caches.containsKey(id)
					&& result.cacheType != null) result.addCache((Cache) Class
					.forName(result.cacheType).newInstance());
			return result;
			
		} catch (Exception e) {
			System.err.println("Couldn't find repeat:" + agentId + "." + id);
		}
		return null;
	}
}
