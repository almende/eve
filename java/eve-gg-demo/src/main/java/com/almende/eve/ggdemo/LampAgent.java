package com.almende.eve.ggdemo;

import java.io.IOException;
import java.util.ArrayList;

import com.almende.eve.agent.AgentInterface;
import com.almende.eve.agent.annotation.ThreadSafe;
import com.almende.eve.rpc.annotation.Access;
import com.almende.eve.rpc.annotation.AccessType;
import com.almende.eve.rpc.annotation.Name;
import com.almende.eve.rpc.annotation.Sender;
import com.almende.eve.rpc.jsonrpc.JSONRPCException;
import com.fasterxml.jackson.core.JsonProcessingException;

@Access(AccessType.PUBLIC)
@ThreadSafe(true)
public interface LampAgent extends AgentInterface {
	
	public void create(@Name("neighbours") ArrayList<String> neighbours,
			@Name("stepSize") Integer stepSize) throws JSONRPCException, IOException;
	
	public boolean isOn();
	public boolean isOnBlock() throws InterruptedException;
	public void handleGoal(@Name("goal") Goal goal, @Sender String sender) throws 
			JSONRPCException, JsonProcessingException, IOException;
	public Iterable<String> getNeighbours();
}
