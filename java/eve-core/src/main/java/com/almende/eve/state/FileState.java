package com.almende.eve.state;

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
 *     AgentFactory factory = new AgentFactory(config);<br>
 *     State state = new State("agentId");<br>
 *     state.put("key", "value");<br>
 *     System.out.println(state.get("key")); // "value"<br>
 * 
 * @author jos
 */
// TODO: create an in memory cache and reduce the number of reads/writes
public abstract class FileState extends State {
	protected FileState() {}

	public FileState(String agentId) {
		super(agentId);
	}

	/**
	 * init is executed once before the agent method is invoked
	 */
	@Override
	abstract public void init();
	
	/**
	 * destroy is executed once after the agent method is invoked
	 * if the properties are changed, they will be saved
	 */
	@Override
	abstract public void destroy();

	@Override
	abstract public void clear();

	@Override
	abstract public Set<String> keySet();

	@Override
	abstract public boolean containsKey(Object key);

	@Override
	abstract public boolean containsValue(Object value);
	
	@Override
	abstract public Set<java.util.Map.Entry<String, Object>> entrySet();

	@Override
	abstract public Object get(Object key);

	@Override
	abstract public boolean isEmpty();

	@Override
	abstract public Object put(String key, Object value);

	@Override
	abstract public void putAll(Map<? extends String, ? extends Object> map);

	@Override
	abstract public Object remove(Object key);

	@Override
	abstract public int size();
	
	@Override
	abstract public Collection<Object> values();
	
}