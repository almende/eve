package com.almende.util;

import java.util.ArrayList;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;

import com.almende.eve.config.Config;

public class ObjectCache {
	private int								maxSize	= 1000;
	private Map<String, MetaInfo<?>>		cache	= new ConcurrentHashMap<String, MetaInfo<?>>(
															maxSize);
	private SortedSet<MetaInfo<?>>			scores	= new TreeSet<MetaInfo<?>>();
	
	private static Map<String, ObjectCache>	caches	= new ConcurrentHashMap<String, ObjectCache>();
	
	protected ObjectCache(final String label, final Config config) {
		if (config != null) {
			configCache(config);
		}
		caches.put(label, this);
	}
	
	protected ObjectCache(final String label) {
		this(label, null);
	}
	
	public static ObjectCache get(final String label) {
		if (!caches.containsKey(label)) {
			new ObjectCache(label);
		}
		return caches.get(label);
	}
	
	/**
	 * Reinitialize cache, using given configuration. (currently only
	 * "ObjectCache"."maxSize" is used from config)
	 * 
	 * @param config
	 */
	public void configCache(final Config config) {
		synchronized (cache) {
			final Integer max = config.get("ObjectCache", "maxSize");
			if (max != null) {
				maxSize = max;
			}
			cache = new ConcurrentHashMap<String, MetaInfo<?>>(
					maxSize + 1);
			scores = new TreeSet<MetaInfo<?>>();
		}
	}
	
	/**
	 * Get value instance from cache, if exiting. Returns null if no va;ue is
	 * stored in cache.
	 * 
	 * @param key
	 * @return
	 */
	public <T> T get(final String key, final Class<T> type) {
		synchronized (cache) {
			final MetaInfo<?> result = cache.get(key);
			if (result != null
					&& type.isAssignableFrom(result.getValue().getClass())) {
				result.use();
				return type.cast(result.getValue());
			}
			return null;
		}
	}
	
	public boolean containsKey(final String key) {
		return cache.containsKey(key);
	}
	
	/**
	 * Put agent instance into the cache from later retrieval. Runs eviction
	 * policy after entry of agent.
	 * 
	 * @param key
	 * @param value
	 */
	public <T> void put(final String key, final T value) {
		synchronized (cache) {
			final MetaInfo<T> entry = new MetaInfo<T>(key, value);
			cache.put(key, entry);
			final int overshoot = cache.size() - maxSize;
			if (overshoot > 0) {
				evict(overshoot);
			}
			scores.add(entry);
		}
	}
	
	protected void evict(final int amount) {
		synchronized (cache) {
			final ArrayList<MetaInfo<?>> toEvict = new ArrayList<MetaInfo<?>>(amount);
			if (scores.size() <= amount) {
				cache.clear();
				scores.clear();
				return;
			}
			for (int i = 0; i < amount; i++) {
				if (scores.size() > 0) {
					final MetaInfo<?> entry = scores.first();
					toEvict.add(entry);
					scores.remove(entry);
				}
			}
			for (final MetaInfo<?> entry : toEvict) {
				cache.remove(entry.getKey());
			}
		}
	}
	
	/**
	 * Remove specific agent from cache.
	 * 
	 * @param key
	 */
	public void delete(final String key) {
		final MetaInfo<?> item = cache.remove(key);
		if (item != null) {
			scores.remove(item);
		}
	}
	
	public int size() {
		return cache.size();
	}
	
	public boolean isEmpty() {
		return cache.isEmpty();
	}
	
	public void clear() {
		cache.clear();
	}
	
}

class MetaInfo<T> implements Comparable<MetaInfo<?>> {
	private final String	key;
	private T		value;
	private int		count	= 0;
	private final long	created;
	
	public MetaInfo(final String key, final T value) {
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
	
	public void setValue(final T value) {
		this.value = value;
	}
	
	@Override
	public int compareTo(final MetaInfo<?> o) {
		return Double.compare(score(), o.score());
	}
	
}