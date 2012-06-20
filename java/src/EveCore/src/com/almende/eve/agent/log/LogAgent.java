/**
 * @file LogAgent.java
 * 
 * @brief 
 * LogAgent can register itself on events from other agents, and log these
 * events.
 *
 * @license
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy 
 * of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 *
 * Copyright © 2012 Almende B.V.
 *
 * @author 	Jos de Jong, <jos@almende.org>
 * @date	  2012-04-04
 */
package com.almende.eve.agent.log;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import com.almende.eve.agent.Agent;
import com.almende.eve.json.JSONRequest;
import com.almende.eve.json.annotation.Name;
import com.almende.eve.json.annotation.Required;
import com.almende.eve.json.jackson.JOM;
import com.fasterxml.jackson.databind.node.ObjectNode;

public class LogAgent extends Agent {
	/**
	 * start logging an agent
	 * @param url
	 * @throws Exception 
	 */
	public void addAgent(@Name("url") String url) throws Exception {
		// send a subscribe request to the agent
		String method = "subscribe";
		ObjectNode params = JOM.createObjectNode();
		params.put("event", "*");
		params.put("callbackUrl", getUrl());
		params.put("callbackMethod", "onEvent");
		send(url, method, params);
		
		// store the agents url
		@SuppressWarnings("unchecked")
		List<String> urls = (List<String>) getContext().get("urls");
		if (urls == null) {
			urls = new ArrayList<String>();
		}
		urls.add(url);
		getContext().put("urls", urls);
	}
	
	/**
	 * stop logging an agent
	 * @param url
	 * @throws Exception 
	 */
	public void removeAgent(@Name("url") String url) throws Exception {
		String method = "unsubscribe";
		ObjectNode params = JOM.createObjectNode();
		params.put("event", "*");
		params.put("callbackUrl", getUrl());
		params.put("callbackMethod", "onEvent");
		send(url, method, params);
		
		// remove the agents url
		@SuppressWarnings("unchecked")
		List<String> urls = (List<String>) getContext().get("urls");
		if (urls != null) {
			urls.remove(url);
			getContext().put("urls", urls);
		}
	}

	/**
	 * Get all agents being logged by this LogAgent
	 * @return List with agent urls
	 * @throws Exception 
	 */
	public List<String> getAgents() throws Exception {
		@SuppressWarnings("unchecked")
		List<String> urls =  (List<String>) getContext().get("urls");
		if (urls != null) {
			return urls;
		}
		else {
			return new ArrayList<String>();
		}
	}

	/**
	 * Clear all logged data and unsubscribe from all logged agents
	 * @throws Exception 
	 */
	public void clear() throws Exception {
		@SuppressWarnings("unchecked")
		List<String> urls =  (List<String>) getContext().get("urls");
		if (urls != null) {
			for (String url : urls) {
				String method = "unsubscribe";
				ObjectNode params = JOM.createObjectNode();
				params.put("event", "*");
				params.put("callbackUrl", getUrl());
				params.put("callbackMethod", "onEvent");
				send(url, method, params);
			}
		}
		getContext().remove("urls");
		getContext().remove("logs");
		
		cancelTimeToLive();
		
		// TODO: remove this
		Logger logger = Logger.getLogger(this.getClass().getName());		
		logger.info("clear");
		
		super.clear();
	}
	
	/**
	 * Remove existing time to live
	 */
	public void cancelTimeToLive() {
		String timeoutId =  (String) getContext().get("timeoutId");
		if (timeoutId != null) {
			getContext().getScheduler().cancelTimer(timeoutId);
		}
		getContext().remove("timeoutId");
	}
	
	/**
	 * Set a time-to-live for the LogAgent. After this timeout, it will
	 * cleanup and destroy itself.
	 * This is useful for a temporary LogAgent used for a single session in a
	 * browser.
	 * @param interval      interval in milliseconds
	 * @throws Exception 
	 */
	public void setTimeToLive(@Name("interval") long interval) 
			throws Exception {
		// remove existing timeout
		cancelTimeToLive();
		
		// create a new timeout
		JSONRequest request = new JSONRequest("clear", null);
		String timeoutId = 
			getContext().getScheduler().setTimeout(getUrl(), request, interval);
		getContext().put("timeoutId", timeoutId);
		
		// TODO: remove this
		Logger logger = Logger.getLogger(this.getClass().getName());		
		logger.info("setTimeToLive " + interval);		
	}

	
	/**
	 * Get logs
	 * @param since     Optional timestamp. If provided, all logs with a 
	 *                  timstamp > since will be returned
	 * @param url       Optional url of an agent. If provided, all logs 
	 *                  will be filtered by this url.
	 * @return
	 */
	public List<Log> getLogs(
			@Required(false) @Name("since") Long since,
			@Required(false) @Name("url") String url) {
		@SuppressWarnings("unchecked")
		List<Log> logs = (List<Log>) getContext().get("logs");

		List<Log> output = new ArrayList<Log>();
		if (logs != null) {
			for (Log log : logs) {
				boolean timestampOk = ((since == null) || (log.getTimestamp() > since));
				boolean urlOk = ((url == null) || (url.equals(log.getAgent())));
				if (timestampOk && urlOk) {
					output.add(log);
				}
			}
		}
		
		return output;
	}
	
	/**
	 * Add a log to the saved logs
	 * @param log
	 */
	private void addLog(Log log) {
		@SuppressWarnings("unchecked")
		// TODO: use a database instead of the context - when you register
		//       more and more logs this will be very unreliable.
		List<Log> logs = (List<Log>) getContext().get("logs");
		if (logs == null) {
			logs = new ArrayList<Log>();
		}
		logs.add(log);
		
		getContext().put("logs", logs);
	}
	
	/**
	 * onEvent is the callback method where triggered events will be send
	 * to from the subscribed agents. 
	 * @param agent
	 * @param event
	 * @param params
	 * @throws Exception
	 */
	public void onEvent(
			@Name("agent") String agent, 
			@Name("event") String event, 
			@Required(false) @Name("params") ObjectNode params) 
			throws Exception {
		Logger logger = Logger.getLogger(this.getClass().getName());		
		logger.info(agent + " " + event + " " + 
				((params != null) ? params.toString() : ""));

		addLog(new Log(agent, event, params));
		
		// re-trigger the event
		trigger(event, params);
	}
	
	@Override
	public String getDescription() {
		return "LogAgent can register itself on events from other agents, " +
			"and log these events. " + 
			"It offers functionality to search through logged events.";
	}

	@Override
	public String getVersion() {
		return "0.1";
	}
}
