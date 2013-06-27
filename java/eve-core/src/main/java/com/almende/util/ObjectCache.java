package com.almende.util;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.almende.eve.config.Config;

public class ObjectCache {
	private static int						maxSize	= 100;
	private static Map<String, MetaInfo<?>>	cache	= new ConcurrentHashMap<String, MetaInfo<?>>(
															maxSize);
	private static List<MetaInfo<?>>			scores	= new ArrayList<MetaInfo<?>>(
															maxSize);
	
	/**
	 * Reinitialize cache, using given configuration. (currently only "ObjectCache"."maxSize" is used from config)
	 * @param config
	 */
	public static void configCache(Config config) {
		synchronized (cache) {
			Integer maxSize = config.get("ObjectCache", "maxSize");
			if (maxSize != null) {
				ObjectCache.maxSize = maxSize;
			}
			ObjectCache.cache = new ConcurrentHashMap<String, MetaInfo<?>>(
					ObjectCache.maxSize + 1);
			ObjectCache.scores = new ArrayList<MetaInfo<?>>(ObjectCache.maxSize);
		}
	}
	
	/**
	 * Get value instance from cache, if exiting. Returns null if no va;ue is stored in cache.
	 * @param agentId
	 * @return
	 */
	public static <T>T get(String agentId, Class<T> type) {
		MetaInfo<?> result = cache.get(agentId);
		if (result != null && result.getClass().isAssignableFrom(type)) {
			result.use();
			return type.cast(result.getValue());
		}
		return null;
	}
	
	/**
	 * Put agent instance into the cache from later retrieval. Runs eviction policy after entry of agent.
	 * @param key
	 * @param value
	 */
	public static <T> void put(String key, T value) {
		synchronized (cache) {
			MetaInfo<T> entry = new MetaInfo<T>(key,value);
			cache.put(key, entry);
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
			ArrayList<MetaInfo<?>> toEvict = new ArrayList<MetaInfo<?>>(amount);
			for (int i = 0; i < amount; i++) {
				MetaInfo<?> entry = scores.get(i);
				toEvict.add(entry);
			}
			scores = (List<MetaInfo<?>>) scores.subList(amount, scores.size());
			for (MetaInfo<?> entry : toEvict) {
				cache.remove(entry.getKey());
			}
		}
	}
	
	/** 
	 * Remove specific agent from cache.
	 * @param key
	 */
	public static void delete(String key) {
		cache.remove(key);
	}
}

class MetaInfo<T> implements Comparable<MetaInfo<T>> {
	private String  key;
	private T		value;
	private int		count	= 0;
	private long	created;
	
	public MetaInfo(String key, T value) {
		created = System.currentTimeMillis();
		this.key = key;
		this.value = value;
	}
	
	public void use() {
		count++;
	}
	
	public String getKey(){
		return this.key;
	}
	
	public long getAge() {
		return (System.currentTimeMillis() - created);
	}
	
	public double score() {
		return count / getAge();
	}
	
	public T getValue() {
		return value;
	}

	public void setValue(T value) {
		this.value = value;
	}

	@Override
	public int compareTo(MetaInfo<T> o) {
		return Double.compare(score(), o.score());
	}
	
}