package com.almende.eve.agent.example;

import com.almende.eve.agent.AgentInterface;
import com.almende.eve.agent.annotation.Name;
import com.almende.eve.agent.example.TestAgent.STATUS;

public interface TestAgentInterface extends AgentInterface {
	public Double add(@Name("a") Double a, @Name("b") Double b);
	public Double multiply(@Name("a") Double a, @Name("b") Double b);
	public Double increment();
	public STATUS testEnum(@Name("status") STATUS status);
}
