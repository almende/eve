package com.almende.eve.agent.example;

import com.almende.eve.agent.AgentInterface;
import com.almende.eve.json.annotation.Name;

public interface TestAgentInterface extends AgentInterface {
	public Double add(@Name("a") Double a, @Name("b") Double b);
	public Double multiply(@Name("a") Double a, @Name("b") Double b);
	public Double increment();
}
