package com.almende.eve.state;

import java.io.Serializable;
import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @class MemoryState
 * 
 * A state for an Eve Agent, which stores the data in memory.
 * (After a restart of the application, the data will be gone!)
 * 
 * The state provides general information for the agent (about itself,
 * the environment, and the system configuration), and the agent can store its 
 * state in the state. 
 * The state extends a standard Java Map.
 * 
 * <b>Warning, this implementation is not thread-safe!</b>
 * 
 * Usage:<br>
 *     AgentFactory factory = new AgentFactory(config);<br>
 *     FileState state = new State("agentId");<br>
 *     state.put("key", "value");<br>
 *     System.out.println(state.get("key")); // "value"<br>
 * 
 * @author jos
 */
public class MemoryState extends AbstractState {
	private Map<String, Serializable> properties = new ConcurrentHashMap<String, Serializable>();

	public MemoryState() {}
	
	public MemoryState(String agentId) {
		super(agentId);
	}

	@Override
	public void clear() {
		properties.clear();
	}

	@Override
	public Set<String> keySet() {
		return properties.keySet();
	}

	@Override
	public boolean containsKey(Object key) {
		return properties.containsKey(key);
	}

	@Override
	public boolean containsValue(Object value) {
		return properties.containsValue(value);
	}

	@Override
	public Set<java.util.Map.Entry<String, Serializable>> entrySet() {
		return properties.entrySet();
	}

	@Override
	public Serializable get(Object key) {
		return properties.get(key);
	}

	@Override
	public boolean isEmpty() {
		return properties.isEmpty();
	}

	@Override
	public Serializable put(String key, Serializable value) {
		return properties.put(key, value);
	}

	@Override
	public void putAll(Map<? extends String, ? extends Serializable> map) {
		properties.putAll(map);
	}

	@Override
	public boolean putIfUnchanged(String key, Serializable newVal, Serializable oldVal) {
		boolean result=false;
		if (!(oldVal == null && properties.containsKey(key)) || properties.get(key).equals(oldVal)){
			properties.put(key, newVal);
			result=true;
		}
		return result;
	}
	
	@Override
	public Serializable remove(Object key) {
		return properties.remove(key);
	}

	@Override
	public int size() {
		return properties.size();
	}

	@Override
	public Collection<Serializable> values() {
		return properties.values();
	}

	/**
	 * init is executed once before the agent method is invoked
	 */
	@Override
	public void init() {}

	/**
	 * destroy is executed once after the agent method is invoked
	 */
	@Override
	public void destroy() {}


}
