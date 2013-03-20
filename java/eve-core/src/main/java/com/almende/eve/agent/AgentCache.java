package com.almende.eve.agent;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.almende.eve.config.Config;

public class AgentCache {
	private static int maxSize = 100;
	private static Map<String,MetaInfo> cache = new ConcurrentHashMap<String, MetaInfo>(maxSize);
	protected static List<MetaInfo> scores = new ArrayList<MetaInfo>(maxSize);
	
	public static void configCache(Config config){
		synchronized(cache){
			Integer maxSize = config.get("AgentCache","maxSize");
			if (maxSize != null) AgentCache.maxSize=maxSize;
			//System.err.println("Init AgentCache on maxSize:"+this.maxSize);
			AgentCache.cache = new ConcurrentHashMap<String, MetaInfo>(AgentCache.maxSize+1);
			AgentCache.scores = new ArrayList<MetaInfo>(AgentCache.maxSize);
		}
	}
	
	
	public static Agent get(String agentId){
		MetaInfo result = cache.get(agentId);
		if (result != null){
			result.use();
			//System.err.println("Got:"+result.agent.getId());
			return result.agent;
		}
		return null;
	}
	
	public static void put(String agentId, Agent agent){
		synchronized(cache){
			MetaInfo entry = new MetaInfo(agent);
			cache.put(agentId, entry);
			int overshoot = cache.size()-maxSize;
			if (overshoot > 0){
				evict(overshoot);
			}
			scores.add(entry);
			//System.err.println("Added:"+agent.getId());
		}
	}
	static protected void evict(int amount){
		synchronized(cache){
			Collections.sort(scores);
			ArrayList<MetaInfo> toEvict = new ArrayList<MetaInfo>(amount);
			for (int i=0; i<amount; i++){
				MetaInfo entry = scores.get(i);
				toEvict.add(entry);
			}
			scores = (List<MetaInfo>) scores.subList(amount, scores.size());
			for (MetaInfo entry: toEvict){
				cache.remove(entry.agent.getId());
			}
			//System.err.println("Evicted:"+amount+" records");
		}
	}
	static public void delete(String agentId) {
		cache.remove(agentId);
	}
}
class MetaInfo implements Comparable<MetaInfo> {
	Agent agent;
	int count=0;
	long created;
	public MetaInfo(Agent agent){
		created = System.currentTimeMillis();
		this.agent = agent;
	}
	public void use(){
		count++;
	}
	public long getAge(){
		return (System.currentTimeMillis()-created);
	}
	public double score(){
		return count/getAge();
	}
	@Override
	public int compareTo(MetaInfo o) {
		return Double.compare(score(), o.score());
	}
	
}