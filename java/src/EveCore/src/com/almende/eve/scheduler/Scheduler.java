package com.almende.eve.scheduler;

import com.almende.eve.json.JSONRequest;

public interface Scheduler {
	// TODO: implement scheduler
	
	int setTimeout(JSONRequest request, long delay);
	int setInterval(JSONRequest request, long interval);
}
