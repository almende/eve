package com.almende.eve.agent;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.almende.eve.config.Config;

public class AgentCache {
	private static int						maxSize	= 100;
	private static Map<String, MetaInfo>	cache	= new ConcurrentHashMap<String, MetaInfo>(
															maxSize);
	protected static List<MetaInfo>			scores	= new ArrayList<MetaInfo>(
															maxSize);
	
	/**
	 * Reinitialize cache, using given configuration. (currently only "AgentCache"."maxSize" is used from config)
	 * @param config
	 */
	public static void configCache(Config config) {
		synchronized (cache) {
			Integer maxSize = config.get("AgentCache", "maxSize");
			if (maxSize != null) {
				AgentCache.maxSize = maxSize;
			}
			AgentCache.cache = new ConcurrentHashMap<String, MetaInfo>(
					AgentCache.maxSize + 1);
			AgentCache.scores = new ArrayList<MetaInfo>(AgentCache.maxSize);
		}
	}
	
	/**
	 * Get agent instance from cache, if exiting. Returns null if no agent is stored in cache.
	 * @param agentId
	 * @return
	 */
	public static Agent get(String agentId) {
		MetaInfo result = cache.get(agentId);
		if (result != null) {
			result.use();
			return result.agent;
		}
		return null;
	}
	
	/**
	 * Put agent instance into the cache from later retrieval. Runs eviction policy after entry of agent.
	 * @param agentId
	 * @param agent
	 */
	public static void put(String agentId, Agent agent) {
		synchronized (cache) {
			MetaInfo entry = new MetaInfo(agent);
			cache.put(agentId, entry);
			int overshoot = cache.size() - maxSize;
			if (overshoot > 0) {
				evict(overshoot);
			}
			scores.add(entry);
		}
	}
	
	protected static void evict(int amount) {
		synchronized (cache) {
			Collections.sort(scores);
			ArrayList<MetaInfo> toEvict = new ArrayList<MetaInfo>(amount);
			for (int i = 0; i < amount; i++) {
				MetaInfo entry = scores.get(i);
				toEvict.add(entry);
			}
			scores = (List<MetaInfo>) scores.subList(amount, scores.size());
			for (MetaInfo entry : toEvict) {
				cache.remove(entry.agent.getId());
			}
		}
	}
	
	/** 
	 * Remove specific agent from cache.
	 * @param agentId
	 */
	public static void delete(String agentId) {
		cache.remove(agentId);
	}
}

class MetaInfo implements Comparable<MetaInfo> {
	Agent	agent;
	int		count	= 0;
	long	created;
	
	public MetaInfo(Agent agent) {
		created = System.currentTimeMillis();
		this.agent = agent;
	}
	
	public void use() {
		count++;
	}
	
	public long getAge() {
		return (System.currentTimeMillis() - created);
	}
	
	public double score() {
		return count / getAge();
	}
	
	@Override
	public int compareTo(MetaInfo o) {
		return Double.compare(score(), o.score());
	}
	
}