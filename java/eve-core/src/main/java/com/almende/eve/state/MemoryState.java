package com.almende.eve.state;

import java.io.Serializable;
import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

import com.almende.util.ClassUtil;
import com.fasterxml.jackson.databind.JsonNode;

/**
 * @class MemoryState
 * 
 *        A state for an Eve Agent, which stores the data in memory.
 *        (After a restart of the application, the data will be gone!)
 * 
 *        The state provides general information for the agent (about itself,
 *        the environment, and the system configuration), and the agent can
 *        store its
 *        state in the state.
 *        The state extends a standard Java Map.
 * 
 *        <b>Warning, this implementation is not thread-safe!</b>
 * 
 *        Usage:<br>
 *        AgentHost factory = AgentHost.getInstance(config);<br>
 *        FileState state = new State("agentId");<br>
 *        state.put("key", "value");<br>
 *        System.out.println(state.get("key")); // "value"<br>
 * 
 * @author jos
 */
public class MemoryState extends AbstractState<Serializable> implements State {
	private static final Logger			LOG			= Logger.getLogger(MemoryState.class
															.getName());
	private Map<String, Serializable>	properties	= new ConcurrentHashMap<String, Serializable>();
	
	public MemoryState() {
	}
	
	public MemoryState(String agentId) {
		super(agentId);
	}
	
	public void clear() {
		properties.clear();
	}
	
	public Set<String> keySet() {
		return properties.keySet();
	}
	
	public boolean containsKey(String key) {
		return properties.containsKey(key);
	}
	
	@SuppressWarnings("unchecked")
	public Serializable get(String key) {
		try {
			return ClassUtil.cloneThroughSerialize(properties.get(key));
		} catch (Exception e) {
			LOG.warning("Couldn't clone object: " + key
					+ ", returning pointer to original object.");
			e.printStackTrace();
			return properties.get(key);
		}
	}
	
	public boolean isEmpty() {
		return properties.isEmpty();
	}
	
	public Serializable _put(String key, Serializable value) {
		return properties.put(key, value);
	}
	
	public JsonNode _put(String key, JsonNode value) {
		properties.put(key, value.toString());
		return value;
	}
	
	public void putAll(Map<? extends String, ? extends Serializable> map) {
		properties.putAll(map);
	}
	
	public boolean _putIfUnchanged(String key, Serializable newVal,
			Serializable oldVal) {
		boolean result = false;
		if (!(oldVal == null && properties.containsKey(key) && properties
				.get(key) != null)
				|| (properties.get(key) != null && properties.get(key).equals(
						oldVal))) {
			properties.put(key, (Serializable) newVal);
			result = true;
		}
		return result;
	}

	public boolean _putIfUnchanged(String key, JsonNode newVal,
			JsonNode oldVal) {
		boolean result = false;
		if (!(oldVal == null && properties.containsKey(key) && properties
				.get(key) != null)
				|| (properties.get(key) != null && properties.get(key).equals(
						oldVal))) {
			properties.put(key, newVal.toString());
			result = true;
		}
		return result;
	}

	
	public Serializable remove(String key) {
		return properties.remove(key);
	}
	
	public int size() {
		return properties.size();
	}
	
	public Collection<Serializable> values() {
		return properties.values();
	}
	
	/**
	 * init is executed once before the agent method is invoked
	 */
	public void init() {
	}
	
	/**
	 * destroy is executed once after the agent method is invoked
	 */
	public void destroy() {
	}
	
}
