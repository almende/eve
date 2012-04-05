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
package com.almende.eve.agent;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.logging.Logger;

import net.sf.json.JSONArray;
import net.sf.json.JSONObject;

import com.almende.eve.json.annotation.ParameterName;
import com.almende.eve.json.annotation.ParameterRequired;

@SuppressWarnings("serial")
public class LogAgent extends Agent {
	/**
	 * start logging an agent
	 * @param url
	 * @throws Exception 
	 */
	public void addAgent(@ParameterName("url") String url) throws Exception {
		// send a subscribe request to the agent
		String method = "subscribe";
		JSONObject params = new JSONObject();
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
	public void removeAgent(@ParameterName("url") String url) throws Exception {
		String method = "unsubscribe";
		JSONObject params = new JSONObject();
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
		List<String> urls = (List<String>) getContext().get("urls");
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
		List<String> urls = (List<String>) getContext().get("urls");
		if (urls != null) {
			for (String url : urls) {
				String method = "unsubscribe";
				JSONObject params = new JSONObject();
				params.put("event", "*");
				params.put("callbackUrl", getUrl());
				params.put("callbackMethod", "onEvent");
				send(url, method, params);
			}
		}
		getContext().remove("urls");
		getContext().remove("logs");
	}
	
	/**
	 * Helper class to store logs
	 */
	private class Log {
		Date timestamp = new Date();
		String agent;
		String event;
		JSONObject params;
		
		Log(String agent, String event, JSONObject params) {
			this.agent = agent;
			this.event = event;
			this.params = params;
		}
	}
	
	/**
	 * Get logs
	 * @param from      Optional timestamp. If provided, all logs with a 
	 *                  timstamp > from will be returned
	 * @param url       Optional url of an agent. If provided, all logs 
	 *                  will be filtered by this url.
	 * @return
	 */
	public JSONArray getLogs(
			@ParameterRequired(false) @ParameterName("from") Long from,
			@ParameterRequired(false) @ParameterName("url") String url) {
		@SuppressWarnings("unchecked")
		List<Log> logs = (List<Log>) getContext().get("logs");
		
		JSONArray arr = new JSONArray();
		if (logs != null) {
			for (Log log : logs) {
				Long timestamp = log.timestamp.getTime();
				boolean timestampOk = ((from == null) || (timestamp > from));
				boolean urlOk = ((url == null) || (url.equals(log.agent)));
				if (timestampOk && urlOk) {
					JSONObject obj = new JSONObject();
					obj.put("timestamp", timestamp);
					obj.put("agent", log.agent);
					obj.put("event", log.event);
					obj.put("params", log.params);
					arr.add(obj);
				}
			}
		}
		
		return arr;
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
			@ParameterName("agent") String agent, 
			@ParameterName("event") String event, 
			@ParameterRequired(false) @ParameterName("params") JSONObject params) 
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
