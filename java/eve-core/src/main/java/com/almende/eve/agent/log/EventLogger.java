package com.almende.eve.agent.log;

import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.almende.eve.agent.AgentFactory;

public class EventLogger {
	private static final Logger	LOG	= Logger.getLogger(EventLogger.class
											.getCanonicalName());
	
	protected EventLogger() {
	}
	
	public EventLogger(AgentFactory agentFactory) {
		this.agentFactory = agentFactory;
	}
	
	public void log(String agentId, String event, Object params) {
		try {
			String logAgentId = getLogAgentId(agentId);
			LogAgent agent = (LogAgent) agentFactory.getAgent(logAgentId);
			if (agent != null) {
				// log only if the log agent exists
				agent.log(new Log(agentId, event, params));
			}
		} catch (Exception e) {
			LOG.log(Level.WARNING, "", e);
		}
	}
	
	public List<Log> getLogs(String agentId, Long since) throws Exception {
		String logAgentId = getLogAgentId(agentId);
		LogAgent agent = (LogAgent) agentFactory.getAgent(logAgentId);
		if (agent == null) {
			// create the log agent if it does not yet exist
			agent = (LogAgent) agentFactory.createAgent(LogAgent.class,
					logAgentId);
		}
		return agent.getLogs(since);
	}
	
	private String getLogAgentId(String agentId) {
		// TODO: use a naming here which cannot conflict with other agents.
		// introduce a separate namespace or something like that?
		return "_logagent_" + agentId;
	}
	
	private AgentFactory	agentFactory	= null;
}
