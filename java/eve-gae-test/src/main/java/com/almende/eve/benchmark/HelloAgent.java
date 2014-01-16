package com.almende.eve.benchmark;

import java.net.URI;

import com.almende.eve.agent.Agent;
import com.almende.eve.rpc.annotation.Access;
import com.almende.eve.rpc.annotation.AccessType;
import com.almende.eve.rpc.annotation.Name;
import com.almende.eve.rpc.annotation.Optional;
import com.almende.eve.state.State;
import com.almende.eve.transport.TransportService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

@Access(AccessType.PUBLIC)
public class HelloAgent extends Agent {
	private int n = 0; // the number of this agent

	@Override
	public void onInit() {
		String id = getId(); 
		n = agentIdToNum(id);
	}	
	
	public int ping (@Optional @Name("counter") int counter) {
		return counter + 1;
	}
	
	/**
	 * Send a request and response to each of the other agents
	 * @param num  The total number of agents
	 */
	public void run (@Optional @Name("num") int num) {
		State state = getState();
		TransportService service = getAgentHost().getTransportService("http");
		ObjectMapper mapper = new ObjectMapper();
		
		state.put("status", "running");
		Integer counter = 0;
		for (int i = 0; i < num; i++) {
			if (i != n) {
				try {
					String agentId = numToAgentId(i, num);
					URI url = service.getAgentUrl(agentId);
					String method = "ping";
					ObjectNode params = mapper.createObjectNode();
					params.put("counter", counter);
					counter = send(url, method, params, Integer.class);
				} catch (Exception e) {
					//e.printStackTrace();
				}
			}
		}
		
		state.put("counter", counter);
		state.put("status", "done");
	}
	
	/**
	 * Format an agentId, for example when n = 7 and num = 100,
	 * the returned agentId will be "helloagent007"
	 * @param n
	 * @param num
	 * @return agentId
	 */
	public static String numToAgentId (int n, int num) {
		String format = "helloagent%0" + (int)Math.ceil(Math.log10(num)) + "d";
		return String.format(format, n);
	}

	/**
	 * Retrieve the number from an agentId, for example when 
	 * agentId = "helloagent007", the returned n is 7 
	 * @param agentId
	 * @return n
	 */
	public static int agentIdToNum (String agentId) {
		return Integer.parseInt(agentId.substring("helloagent".length()));
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
		status.put("counter", getState().get("counter", Integer.class));
		
		return status;
	}
}
