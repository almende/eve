package com.almende.eve.scheduler;

import java.io.IOException;
import java.util.Map;
import java.util.HashMap;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import com.almende.eve.json.JSONRPC;
import com.almende.eve.json.JSONRequest;

public class RunnableScheduler implements Scheduler {
	// http://docs.oracle.com/javase/1.5.0/docs/api/java/util/concurrent/ScheduledExecutorService.html
	// http://www.javapractices.com/topic/TopicAction.do?Id=54
	private static long count = 0;
	private final ScheduledExecutorService scheduler = 
		Executors.newScheduledThreadPool(1);
	private final Map<String, ScheduledFuture<?>> tasks = 
		new HashMap<String, ScheduledFuture<?>>();
	
	public RunnableScheduler () {}
	
	@Override
	public synchronized String setTimeout(String url, JSONRequest request, long delay) {
		Task task = new Task(url, request);
	    ScheduledFuture<?> future = scheduler.schedule(
	    		task, delay, TimeUnit.MILLISECONDS );
	    
	    putFuture(task.getId(), future);
	    
		return task.getId();	
	}

	@Override
	public synchronized String setInterval(String url, JSONRequest request, long interval) {
		Task task = new Task(url, request);
	    ScheduledFuture<?> future = scheduler.scheduleWithFixedDelay(
	    		task, interval, interval, TimeUnit.MILLISECONDS );
	    
	    putFuture(task.getId(), future);
	    
		return task.getId();
	}

	@Override
	public synchronized void cancelTimer(String id) {
		ScheduledFuture<?> scheduledFuture = tasks.get(id);

		if (scheduledFuture != null) {
			boolean mayInterruptIfRunning = false;
			scheduledFuture.cancel(mayInterruptIfRunning);
		}
	}

	public Set<String> getTimers() {
		return tasks.keySet();
	}
	
	private synchronized String createId() {
		count++;
		long id = count;
		return Long.toString(id);
	}
	
	private synchronized void putFuture(String id, ScheduledFuture<?> future) {
		if (id != null) {
			tasks.put(id, future);
		}
	}

	private synchronized void removeFuture(String id) {
		if (id != null) {
			tasks.remove(id);
		}
	}
	
	private synchronized boolean isDone(String id) {
		if (id != null) {
			ScheduledFuture<?> scheduledFuture = tasks.get(id);
			
			if (scheduledFuture != null) {
				return scheduledFuture.isDone();
			}
		}
		
		return true;
	}
	
	
	private class Task implements Runnable {
		private String id = null;
		private String url = null;
		private JSONRequest request = null;
		
		public Task (String url, JSONRequest request) {
			this.id = createId();
			this.url = url;
			this.request = request;
		}
		
		public String getId() {
			return id;
		}
		
		@Override
		public void run() {
			try {
				JSONRPC.send(url, request);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
			if (isDone(id)) {
				removeFuture(id);
			}
		}	
	}
}
