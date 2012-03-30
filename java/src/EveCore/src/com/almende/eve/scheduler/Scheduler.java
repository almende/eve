package com.almende.eve.scheduler;

import java.util.Set;

import com.almende.eve.json.JSONRequest;

public interface Scheduler {
	public String setTimeout(String url, JSONRequest request, long delay);
	public String setInterval(String url, JSONRequest request, long interval);
	public void cancelTimer(String id);
	public Set<String> getTimers();
}
