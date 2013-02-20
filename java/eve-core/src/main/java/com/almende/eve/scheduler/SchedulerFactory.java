package com.almende.eve.scheduler;

import com.almende.eve.agent.AgentFactory;

public abstract class SchedulerFactory {
	protected AgentFactory agentFactory = null;
	
	public abstract Scheduler getScheduler(String agentId);

	/**
	 * Perform bootstrap tasks. bootstrap is called by the AgentFactory
	 * after the agentfactory is fully initialized.
	 */
	public abstract void bootstrap();

	/**
	 * Set the agent factory for this transport service.
	 * This method is called by the AgentFactory itself when the TransportService
	 * is added to the agentFactory using addTransportService
	 * @param agentFactory
	 */
	public final void setAgentFactory(AgentFactory agentFactory) {
		this.agentFactory = agentFactory;
	}	
}
