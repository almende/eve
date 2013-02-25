package com.almende.eve.state;

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
	private Map<String, Object> properties = new ConcurrentHashMap<String, Object>();

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
	public Set<java.util.Map.Entry<String, Object>> entrySet() {
		return properties.entrySet();
	}

	@Override
	public Object get(Object key) {
		return properties.get(key);
	}

	@Override
	public boolean isEmpty() {
		return properties.isEmpty();
	}

	@Override
	public Object put(String key, Object value) {
		return properties.put(key, value);
	}

	@Override
	public void putAll(Map<? extends String, ? extends Object> map) {
		properties.putAll(map);
	}

	@Override
	public boolean putIfUnchanged(String key, Object newVal, Object oldVal) {
		boolean result=false;
		if (!(oldVal == null && !properties.containsKey(key)) || properties.get(key).equals(oldVal)){
			properties.put(key, newVal);
			result=true;
		}
		return result;
	}
	
	@Override
	public Object remove(Object key) {
		return properties.remove(key);
	}

	@Override
	public int size() {
		return properties.size();
	}

	@Override
	public Collection<Object> values() {
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
