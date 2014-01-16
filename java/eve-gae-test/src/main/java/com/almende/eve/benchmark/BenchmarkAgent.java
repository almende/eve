package com.almende.eve.benchmark;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.net.URI;
import java.util.Date;

import com.almende.eve.agent.Agent;
import com.almende.eve.agent.AgentHost;
import com.almende.eve.rpc.annotation.Access;
import com.almende.eve.rpc.annotation.AccessType;
import com.almende.eve.rpc.annotation.Name;
import com.almende.eve.rpc.annotation.Optional;
import com.almende.eve.rpc.jsonrpc.JSONRPCException;
import com.almende.eve.rpc.jsonrpc.JSONRequest;
import com.almende.eve.state.State;
import com.almende.eve.transport.TransportService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * BenchmarAgent can set up a test with a set of agents, and test performance
 * of these agents interacting with each other.
 * {@link BenchmarkAgent}
 */
@Access(AccessType.PUBLIC)
public class BenchmarkAgent extends Agent {
	@Override
	public void onCreate () {
		getState().put("status", "none");
	}
	
	public void startAsTask (@Optional @Name("num") Integer num) {
		JSONRequest task = new JSONRequest();
		task.setMethod("start");
		ObjectNode params = new ObjectMapper().createObjectNode();
		params.put("num", num);
		task.setParams(params);
		long delay = 0;
		getScheduler().createTask(task, delay);
	}
	
	/**
	 * Start a new benchmark with the specified number of agents
	 * @param num		Number of agents
	 * @throws IOException 
	 * @throws JSONRPCException 
	 * @throws NoSuchMethodException 
	 * @throws InvocationTargetException 
	 * @throws IllegalAccessException 
	 * @throws InstantiationException 
	 */
	public ObjectNode start (@Optional @Name("num") Integer num) {
		// cancel any currently running test
		cancel();
		
		State state = getState();
		AgentHost host = getAgentHost();
		TransportService service = host.getTransportService("http");
		
		// create the required number of agents
		for (int i = 0; i < num; i++) {
			try {
				String id = HelloAgent.numToAgentId(i, num);
				host.createAgent(HelloAgent.class, id);
			} catch (Exception e) {
				e.printStackTrace();
				// TODO: handle error
			}
		}
		int pairs = num * (num - 1) / 2;
		int messages = num * (num - 1);
		state.put("num", num);
		state.put("pairs", pairs);
		state.put("messages", messages);
		state.remove("duration");
		state.put("status", "running");

		Date start = new Date();
		
		// give the agents a start command
		ObjectMapper mapper = new ObjectMapper();
		for (int i = 0; i < num; i++) {
			try {
				String agentId = HelloAgent.numToAgentId(i, num);
				URI url = service.getAgentUrl(agentId);
				String method = "run";
				ObjectNode params = mapper.createObjectNode();
				params.put("num", num);
				send(url, method, params);
			} catch (Exception e) {
				//e.printStackTrace();
			}
		}

		Date end = new Date();
		state.put("duration", (end.getTime() - start.getTime())); // in millisec
		state.put("status", "finished");

		return status();
	}

	/**
	 * Stop a running test
	 */
	public void cancel () {
		State state = getState();
		AgentHost host = getAgentHost();
		
		Integer num = state.get("num", Integer.class);
		
		// delete the agents
		if (num != null) {
			for (int i = 0; i < num; i++) {
				try {
					String id = HelloAgent.numToAgentId(i, num);
					host.deleteAgent(id);
				} catch (Exception e) {
					e.printStackTrace();
					// TODO: handle error
				}
			}
		}
		
		state.put("num", 0);
		state.put("status", "cancelled");
	}
	
	/**
	 * Get the status of the test
	 * @return status 	A JSON object containing properties: 
	 *                  {String} status
	 *                  {Integer} num
	 */
	public ObjectNode status () {
		ObjectMapper mapper = new ObjectMapper();
		ObjectNode status = mapper.createObjectNode();
		
		status.put("status", getState().get("status", String.class));
		status.put("num", getState().get("num", Integer.class));
		status.put("pairs", getState().get("pairs", Integer.class));
		
		Integer messages = getState().get("messages", Integer.class);
		Long duration = getState().get("duration", Long.class);
		status.put("messages", messages);
		status.put("duration", duration);
		if (messages != null & duration != null) {
			status.put("durationPerMessage", (double)duration / (double)messages);
		}
		
		return status;
	}
}
