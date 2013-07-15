package com.almende.eve.scheduler.google;

import static com.google.appengine.api.taskqueue.TaskOptions.Builder.withUrl;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.joda.time.DateTime;

import com.almende.eve.agent.Agent;
import com.almende.eve.agent.AgentHost;
import com.almende.eve.rpc.jsonrpc.JSONRequest;
import com.almende.eve.scheduler.AbstractScheduler;
import com.almende.eve.scheduler.SchedulerFactory;
import com.almende.eve.transport.TransportService;
import com.almende.util.TwigUtil;
import com.google.appengine.api.datastore.Query.FilterOperator;
import com.google.appengine.api.datastore.QueryResultIterator;
import com.google.appengine.api.taskqueue.Queue;
import com.google.appengine.api.taskqueue.QueueFactory;
import com.google.appengine.api.taskqueue.TaskHandle;
import com.google.code.twig.ObjectDatastore;
import com.google.code.twig.annotation.AnnotationObjectDatastore;

public class GaeSchedulerFactory implements SchedulerFactory {
	private AgentHost agentFactory = null;

	// private Logger logger = Logger.getLogger(this.getClass().getSimpleName());

	/**
	 * This constructor is called when constructed by the AgentFactory
	 * @param agentFactory
	 * @param params
	 */
	public GaeSchedulerFactory (AgentHost agentFactory, 
			Map<String, Object> params) {
		this.agentFactory = agentFactory;
		TwigUtil.register(GaeTask.class);
	}
	
	protected GaeSchedulerFactory () {}
	
	/**
	 * Get a scheduler for specified agent
	 * @param agentId
	 * @return scheduler
	 */
	public AppEngineScheduler getScheduler(Agent agent) {
		return new AppEngineScheduler(agent.getId());
	}

	/**
	 * AppEngineScheduler
	 * A scheduler for a single agent.
	 *
	 */
	public class AppEngineScheduler extends AbstractScheduler {
		/**
		 * constructor
		 * @param agentId
		 */
		private AppEngineScheduler(String agentId) {
			this.agentId = agentId;
		}
		

		/**
		 * Schedule a task
		 * @param request   A JSONRequest with method and params
		 * @param delay     The delay in milliseconds
		 * @return taskId
		 */
		@Override
		public String createTask(JSONRequest request, long delay) {
			return createTask(request, delay, false, false);
		}
		/**
		 * Schedule a task
		 * @param request   A JSONRequest with method and params
		 * @param delay     The delay in milliseconds
		 * @param interval   Should the task be repeated at an interval?
		 * @param sequential Should (long running) tasks run sequential, or may they run in parallel?
		 * @return taskId
		 */
		@Override
		public String createTask(JSONRequest request, long delay, boolean interval, boolean sequential) {
			try {
				
				if (interval){
					//TODO: fix this!
					System.err.println("WARNING: interval scheduling is not supported by Eve on Google App Engine, only running once!");
				}
				
				// TODO: getting an arbitrary http service which knows this agent
				//       is not safe
				//       -> Scheduler should be configured with the servlet_url 
				//          that it should use specified?
				TransportService service = null;
				for (TransportService s : agentFactory.getTransportServices("http")) {
					if (s.getAgentUrl(agentId) != null) {
						service = s;
						break;
					}
				}
				String url = null;
				if (service != null) {
					url = service.getAgentUrl(agentId);
				}
				
				URL uri = new URL(url);
				String path = uri.getPath();		
				Queue queue = QueueFactory.getDefaultQueue();
				TaskHandle task = queue.add(withUrl(path)
						.payload(request.toString())
						.countdownMillis(delay));
				
				// store task information
				DateTime timestamp = DateTime.now().plus(delay);
				GaeTask storedTask = new GaeTask(task.getName(), 
						agentId, timestamp.toString());
				ObjectDatastore datastore = new AnnotationObjectDatastore();
				datastore.store(storedTask);
				
				return task.getName();			
			} catch (MalformedURLException e) {
				e.printStackTrace();
			}

			// TODO: throw error?
			return null;
		}

		/**
		 * Cancel a scheduled task by its id
		 * @param taskId
		 */
		@Override
		public void cancelTask(String id) {
			// stop the task
			Queue queue = QueueFactory.getDefaultQueue();
			queue.deleteTask(id);
			
			// remove stored task
			ObjectDatastore datastore = new AnnotationObjectDatastore();
			GaeTask storedTask = datastore.load(GaeTask.class, id);
			if (storedTask != null) {
				datastore.delete(storedTask);
			}			
		}
		
		/**
		 * Get the ids of all currently scheduled tasks
		 * @return tasksIds
		 */
		@Override 
		public Set<String> getTasks() {
			ObjectDatastore datastore = new AnnotationObjectDatastore();
			QueryResultIterator<GaeTask> query = datastore.find()
					.type(GaeTask.class)
					.addFilter("agentId", FilterOperator.EQUAL, agentId).now();
			
			Set<String> taskIds = new HashSet<String>();
			while (query.hasNext()) {
				GaeTask task = query.next();
				if (new DateTime(task.getTimestamp()).isAfterNow()) {
					taskIds.add(task.getTaskId());
				}
				else {
					// clean up expired entry
					datastore.delete(task);
				}
			}
			
			return taskIds;
		}
		
		private String agentId = null;
	}

	@Override
	public void destroyScheduler(String agentId) {
		// TODO 
		//How?
	}
}
