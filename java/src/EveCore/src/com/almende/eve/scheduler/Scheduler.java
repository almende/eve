package com.almende.eve.scheduler;

import java.util.Set;

import com.almende.eve.context.Context;
import com.almende.eve.json.JSONRequest;

public interface Scheduler {
	public void setContext(Context context);
	
	public String createTask(JSONRequest request, long delay);
	public String createRepeatingTask(JSONRequest request, long interval);
	public void cancelTask(String id);
	public Set<String> getTasks();
}
