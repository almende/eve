package com.almende.eve.agent.log;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import com.almende.eve.agent.Agent;
import com.almende.eve.rpc.jsonrpc.JSONRequest;

public class LogAgent extends Agent {
	private static long TIME_TO_LIVE = 20 * 60 * 1000; // milliseconds
	
	public void log(Log log) {
		@SuppressWarnings("unchecked")
		// TODO: use a database instead of the context - when you register
		//       more and more logs this will be very unreliable.
		List<Log> logs = (List<Log>) getContext().get("logs");
		if (logs == null) {
			logs = new ArrayList<Log>();
		}
		logs.add(log);
		
		// TODO: limit to a maximum number and age of the logs?
		
		getContext().put("logs", logs);
	}
	
	public List<Log> getLogs(Long since) throws Exception {
		@SuppressWarnings("unchecked")
		List<Log> logs = (List<Log>) getContext().get("logs");

		// TODO: use a database for the logs. It is very inefficient to
		//       retrieve them all and then filter them.
		List<Log> output = new ArrayList<Log>();
		if (logs != null) {
			for (Log log : logs) {
				if (((since == null) || (log.getTimestamp() > since))) {
					output.add(log);
				}
			}
		}
		
		// reset the time to live for the agent. It will stay alive when
		// regularly requested for logs
		setTimeToLive(TIME_TO_LIVE);
		
		return output;
	}

	/**
	 * Remove existing time to live
	 */
	public void cancelTimeToLive() {
		String timeoutId = (String) getContext().get("timeoutId");
		if (timeoutId != null) {
			getScheduler().cancelTask(timeoutId);
		}
		getContext().remove("timeoutId");
	}
	
	/**
	 * Set a time-to-live for the LogAgent. After this timeout, it will
	 * delete itself.
	 * This is useful for a temporary LogAgent used for a single session in a
	 * browser.
	 * @param interval      interval in milliseconds
	 * @throws Exception 
	 */
	public void setTimeToLive(long interval) 
			throws Exception {
		// remove existing timeout
		cancelTimeToLive();
		
		// create a new timeout
		JSONRequest request = new JSONRequest("killMe", null);
		String timeoutId = getScheduler().createTask(request, interval);
		getContext().put("timeoutId", timeoutId);
	}

	/**
	 * Delete the log agent.
	 * @throws Exception 
	 */
	public void killMe () throws Exception {
		getAgentFactory().deleteAgent(getId());
	}
	
	@Override
	public String getDescription() {
		return "The LogAgent can temporarily log events of an agent. " +
				"The agent is meant for internal use by the AgentFactory.";
	}

	@Override
	public String getVersion() {
		return "0.1";
	}

	Logger logger = Logger.getLogger(this.getClass().getSimpleName());
}
