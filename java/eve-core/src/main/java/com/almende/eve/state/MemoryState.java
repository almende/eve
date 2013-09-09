package com.almende.eve.state;

import java.io.Serializable;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

import com.almende.util.ClassUtil;

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
	
	@Override
	public void clear() {
		properties.clear();
	}
	
	@Override
	public Set<String> keySet() {
		return properties.keySet();
	}
	
	@Override
	public boolean containsKey(String key) {
		return properties.containsKey(key);
	}
	
	@Override
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
	
	@Override
	public Serializable locPut(String key, Serializable value) {
		return properties.put(key, value);
	}
	
	@Override
	public boolean locPutIfUnchanged(String key, Serializable newVal,
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
	
	@Override
	public Serializable remove(String key) {
		return properties.remove(key);
	}
	
	@Override
	public int size() {
		return properties.size();
	}
	
	/**
	 * init is executed once before the agent method is invoked
	 */
	@Override
	public void init() {
	}
	
	/**
	 * destroy is executed once after the agent method is invoked
	 */
	@Override
	public void destroy() {
	}
	
}
