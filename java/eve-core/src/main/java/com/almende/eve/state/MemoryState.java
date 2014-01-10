package com.almende.eve.state;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

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
	private static final Logger				LOG			= Logger.getLogger(MemoryState.class
																.getName());
	private final Map<String, Serializable>	properties	= new ConcurrentHashMap<String, Serializable>();
	
	public MemoryState() {
	}
	
	public MemoryState(final String agentId) {
		super(agentId);
	}
	
	@Override
	public void clear() {
		
		final String agentType = (String) properties.get(KEY_AGENT_TYPE);
		properties.clear();
		properties.put(KEY_AGENT_TYPE, agentType);
	}
	
	@Override
	public Set<String> keySet() {
		return new HashSet<String>(properties.keySet());
	}
	
	@Override
	public boolean containsKey(final String key) {
		return properties.containsKey(key);
	}
	
	@Override
	public Serializable get(final String key) {
		try {
			return properties.get(key);
			// return ClassUtil.cloneThroughSerialize(properties.get(key));
		} catch (final Exception e) {
			LOG.log(Level.WARNING, "Couldn't clone object: " + key
					+ ", returning pointer to original object.", e);
			return properties.get(key);
		}
	}
	
	public boolean isEmpty() {
		return properties.isEmpty();
	}
	
	@Override
	public Serializable locPut(final String key, final Serializable value) {
		return properties.put(key, value);
	}
	
	@Override
	public boolean locPutIfUnchanged(final String key,
			final Serializable newVal, final Serializable oldVal) {
		boolean result = false;
		if (!(oldVal == null && properties.containsKey(key) && properties
				.get(key) != null)
				|| (properties.get(key) != null && properties.get(key).equals(
						oldVal))) {
			properties.put(key, newVal);
			result = true;
		}
		return result;
	}
	
	@Override
	public Serializable remove(final String key) {
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
