package com.almende.util;

import java.util.ArrayList;
import java.util.Map;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;

import com.almende.eve.config.Config;

public final class ObjectCache {
	private static int						maxSize	= 1000;
	private static Map<String, MetaInfo<?>>	cache	= new ConcurrentHashMap<String, MetaInfo<?>>(
															maxSize);
	private static TreeSet<MetaInfo<?>>		scores	= new TreeSet<MetaInfo<?>>();
	
	private ObjectCache() {
	};
	
	/**
	 * Reinitialize cache, using given configuration. (currently only
	 * "ObjectCache"."maxSize" is used from config)
	 * 
	 * @param config
	 */
	public static void configCache(Config config) {
		synchronized (cache) {
			Integer max = config.get("ObjectCache", "maxSize");
			if (max != null) {
				ObjectCache.maxSize = max;
			}
			ObjectCache.cache = new ConcurrentHashMap<String, MetaInfo<?>>(
					ObjectCache.maxSize + 1);
			ObjectCache.scores = new TreeSet<MetaInfo<?>>();
		}
	}
	
	/**
	 * Get value instance from cache, if exiting. Returns null if no va;ue is
	 * stored in cache.
	 * 
	 * @param key
	 * @return
	 */
	public static <T> T get(String key, Class<T> type) {
		synchronized (cache) {
			MetaInfo<?> result = cache.get(key);
			if (result != null
					&& type.isAssignableFrom(result.getValue().getClass())) {
				result.use();
				return type.cast(result.getValue());
			}
			return null;
		}
	}
	
	/**
	 * Put agent instance into the cache from later retrieval. Runs eviction
	 * policy after entry of agent.
	 * 
	 * @param key
	 * @param value
	 */
	public static <T> void put(String key, T value) {
		synchronized (cache) {
			MetaInfo<T> entry = new MetaInfo<T>(key, value);
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
			ArrayList<MetaInfo<?>> toEvict = new ArrayList<MetaInfo<?>>(amount);
			if (scores.size()<= amount){
				cache.clear();
				scores.clear();
				return;
			}
			for (int i = 0; i < amount; i++) {
				if (scores.size()>0){
					MetaInfo<?> entry = scores.first();
					toEvict.add(entry);
					scores.remove(entry);
				}
			}
			for (MetaInfo<?> entry : toEvict) {
				cache.remove(entry.getKey());
			}
		}
	}
	
	/**
	 * Remove specific agent from cache.
	 * 
	 * @param key
	 */
	public static void delete(String key) {
		MetaInfo<?> item =cache.remove(key); 
		if (item != null){
			scores.remove(item);
		}
	}
}

class MetaInfo<T> implements Comparable<MetaInfo<?>> {
	private String	key;
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
	
	public String getKey() {
		return this.key;
	}
	
	public long getAge() {
		return (System.currentTimeMillis() - created);
	}
	
	public double score() {
		if (getAge() == 0) {
			return count / 0.001;
		}
		return count / getAge();
	}
	
	public T getValue() {
		return value;
	}
	
	public void setValue(T value) {
		this.value = value;
	}
	
	@Override
	public int compareTo(MetaInfo<?> o) {
		return Double.compare(score(), o.score());
	}
	
}