package com.almende.eve.agent;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.almende.eve.config.Config;

public class AgentCache {
	Map<String,MetaInfo> cache;
	List<MetaInfo> scores;
	
	int maxSize = 100;
	
	public AgentCache(){
		cache = new ConcurrentHashMap<String, MetaInfo>(this.maxSize);
		scores = new ArrayList<MetaInfo>(this.maxSize);
	}
	public AgentCache(Config config){
		Integer maxSize = config.get("AgentCache","maxSize");
		if (maxSize != null) this.maxSize=maxSize;
		//System.err.println("Init AgentCache on maxSize:"+this.maxSize);
		cache = new ConcurrentHashMap<String, MetaInfo>(this.maxSize+1);
		scores = new ArrayList<MetaInfo>(this.maxSize);
	}
	
	
	Agent get(String agentId){
		MetaInfo result = cache.get(agentId);
		if (result != null){
			result.use();
			//System.err.println("Got:"+result.agent.getId());
			return result.agent;
		}
		return null;
	}
	
	void put(String agentId, Agent agent){
		synchronized(scores){
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
	private void evict(int amount){
		synchronized(scores){
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