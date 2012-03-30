package com.almende.eve.scheduler.google;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Set;

import com.almende.eve.json.JSONRequest;
import com.almende.eve.scheduler.Scheduler;

import com.google.appengine.api.taskqueue.Queue;
import com.google.appengine.api.taskqueue.QueueFactory;
import com.google.appengine.api.taskqueue.TaskHandle;
import static com.google.appengine.api.taskqueue.TaskOptions.Builder.withUrl;

public class AppEngineScheduler implements Scheduler {
	public AppEngineScheduler () {}
	
	@Override
	public String setTimeout(String url, JSONRequest request, 
			long delay) {
		try {
			URL uri = new URL(url);
			String path = uri.getPath();
			
			Queue queue = QueueFactory.getDefaultQueue();
			TaskHandle task = queue.add(withUrl(path)
					.payload(request.toString())
					.countdownMillis(delay));		
			
			return task.getName();			
		} catch (MalformedURLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		// TODO: throw error?
		return null;
	}

	@Override
	public String setInterval(String url, JSONRequest request, 
			long interval) {
		// TODOL implement setInterval
		
		System.out.println("setInterval not yet supported by AppEngineScheduler");
		
		return null;
	}

	@Override
	public void cancelTimer(String id) {
		Queue queue = QueueFactory.getDefaultQueue();
		queue.deleteTask(id);
	}

	@Override
	public Set<String> getTimers() {
		// TODOL implement getTimers
		
		System.out.println("getTimers not yet supported by AppEngineScheduler");
		
		return null;
	}
}
