package com.almende.eve.state;

import java.io.Serializable;
import java.util.Collection;
import java.util.Map;
import java.util.Set;

/**
 * @class FileState
 * 
 * A persistent state for an Eve Agent, which stores the data on disk.
 * Data is stored in the path provided by the configuration file.
 * 
 * The state provides general information for the agent (about itself,
 * the environment, and the system configuration), and the agent can store 
 * persistent data in the state. 
 * The state extends a standard Java Map.
 * 
 * Usage:<br>
 *     AgentHost factory = AgentHost.getInstance(config);<br>
 *     State state = new State("agentId");<br>
 *     state.put("key", "value");<br>
 *     System.out.println(state.get("key")); // "value"<br>
 * 
 * @author jos
 */
public abstract class FileState extends AbstractState {
	protected FileState() {}

	public FileState(String agentId) {
		super(agentId);
	}

	/**
	 * init is executed once before the agent method is invoked
	 */
	@Override
	public abstract void init();
	
	/**
	 * destroy is executed once after the agent method is invoked
	 * if the properties are changed, they will be saved
	 */
	@Override
	public abstract void destroy();

	@Override
	public abstract void clear();

	@Override
	public abstract Set<String> keySet();

	@Override
	public abstract boolean containsKey(Object key);

	@Override
	public abstract boolean containsValue(Object value);
	
	@Override
	public abstract Set<java.util.Map.Entry<String, Serializable>> entrySet();

	@Override
	public abstract Serializable get(Object key);

	@Override
	public abstract boolean isEmpty();

	@Override
	public abstract Serializable put(String key, Serializable value);

	@Override
	public abstract void putAll(Map<? extends String, ? extends Serializable> map);

	@Override
	public abstract Serializable remove(Object key);

	
	@Override
	public abstract int size();
	
	@Override
	public abstract Collection<Serializable> values();
	
}