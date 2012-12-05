package com.almende.eve.context;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import com.almende.eve.agent.AgentFactory;
import com.almende.eve.scheduler.RunnableScheduler;
import com.almende.eve.scheduler.Scheduler;

/**
 * @class MemoryContext
 * 
 * A context for an Eve Agent, which stores the data in memory.
 * (After a restart of the application, the data will be gone!)
 * 
 * The context provides general information for the agent (about itself,
 * the environment, and the system configuration), and the agent can store its 
 * state in the context. 
 * The context extends a standard Java Map.
 * 
 * Usage:<br>
 *     AgentFactory factory = new AgentFactory(config);<br>
 *     FileContext context = new Context(factory, "agentClass", "agentId");<br>
 *     context.put("key", "value");<br>
 *     System.out.println(context.get("key")); // "value"<br>
 * 
 * @author jos
 */
public class MemoryContext extends Context {
	public MemoryContext() {}
	
	public MemoryContext(AgentFactory agentFactory, String agentId) {
		super(agentFactory, agentId);
		
		// TODO: register this context in a singleton map
	}

	@Override
	public Scheduler getScheduler() {
		if (scheduler == null) {
			scheduler = new RunnableScheduler();
			scheduler.setContext(this);
		}
		return scheduler;
	}

	@Override
	public synchronized void clear() {
		properties.clear();
	}

	@Override
	public synchronized Set<String> keySet() {
		return properties.keySet();
	}

	@Override
	public synchronized boolean containsKey(Object key) {
		return properties.containsKey(key);
	}

	@Override
	public synchronized boolean containsValue(Object value) {
		return properties.containsValue(value);
	}

	@Override
	public synchronized Set<java.util.Map.Entry<String, Object>> entrySet() {
		return properties.entrySet();
	}

	@Override
	public synchronized Object get(Object key) {
		return properties.get(key);
	}

	@Override
	public synchronized boolean isEmpty() {
		return properties.isEmpty();
	}

	@Override
	public synchronized Object put(String key, Object value) {
		return properties.put(key, value);
	}

	@Override
	public synchronized void putAll(Map<? extends String, ? extends Object> map) {
		properties.putAll(map);
	}

	@Override
	public synchronized Object remove(Object key) {
		return properties.remove(key);
	}

	@Override
	public synchronized int size() {
		return properties.size();
	}

	@Override
	public synchronized Collection<Object> values() {
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

	private Map<String, Object> properties = new HashMap<String, Object>();
	private Scheduler scheduler = null;
}
