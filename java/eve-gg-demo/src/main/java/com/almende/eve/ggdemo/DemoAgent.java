package com.almende.eve.ggdemo;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.HashSet;

import com.almende.eve.agent.Agent;
import com.almende.eve.agent.AgentHost;
import com.almende.eve.rpc.annotation.Access;
import com.almende.eve.rpc.annotation.AccessType;
import com.almende.eve.rpc.annotation.Name;
import com.almende.eve.rpc.jsonrpc.JSONRPCException;
import com.almende.eve.rpc.jsonrpc.jackson.JOM;
import com.almende.util.TypeUtil;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

@Access(AccessType.PUBLIC)
public class DemoAgent extends Agent {
	
	public void startGoal(@Name("goal") Goal goal,
			@Name("startLamp") String startLamp) throws JSONRPCException,
			ClassNotFoundException, InstantiationException,
			IllegalAccessException, InvocationTargetException,
			NoSuchMethodException, IOException {
		LampAgent firstLamp = (LampAgent) getAgentHost().getAgent(startLamp);
		firstLamp.handleGoal(goal, "");
	}
	
	public void genTopology(@Name("type") String type,
			@Name("size") Integer agentCount,
			@Name("stepSize") Integer stepSize,
			@Name("agentType") String agentType)
			throws JSONRPCException, InstantiationException,
			IllegalAccessException, InvocationTargetException,
			NoSuchMethodException, IOException, ClassNotFoundException {
		AgentHost host = getAgentHost();
		ArrayList<String> agents = getState().get("agents",
				new TypeUtil<ArrayList<String>>() {
				});
		if (agents != null) {
			for (String agentId : agents) {
				host.deleteAgent(agentId);
			}
		}
		
		if ("fully".equals(type)){
			genFully(host, agentCount, stepSize, agentType);
		} else if ("line".equals(type)) {
			genLine(host, agentCount, stepSize, agentType);
		} else if ("circle".equals(type)) {
			genCircle(host, agentCount, stepSize, agentType);
		} else if ("star".equals(type)) {
			genStar(host, agentCount, stepSize, agentType);
		} else if ("binTree".equals(type)) {
			genBinaryTree(host, agentCount, stepSize, agentType);
		} else {
			throw new JSONRPCException("Unknown topology type given:" + type);
		}
	}
	
	public ObjectNode getLights() throws JSONRPCException,
			ClassNotFoundException, InstantiationException,
			IllegalAccessException, InvocationTargetException,
			NoSuchMethodException, IOException {
		ObjectNode result = JOM.createObjectNode();
		
		ArrayList<String> agents = getState().get("agents",
				new TypeUtil<ArrayList<String>>() {
				});
		if (agents != null) {
			boolean labels = agents.size()<50;
			result.put("init", true);
			ArrayNode nodes = JOM.createArrayNode();
			ArrayNode edges = JOM.createArrayNode();
			int off = 0;
			int on = 0;
			HashSet<String> uniqueEdges = new HashSet<String>();
			for (String agent : agents) {
				ObjectNode node = JOM.createObjectNode();
				LampAgent lamp = (LampAgent) getAgentHost().getAgent(agent);
				if (lamp == null){
					System.err.println("Warning, agent doesn't exists:"+agent);
					continue;
				}
				String id = lamp.getId().substring(4);
				Boolean isOn = lamp.isOn();
				if (isOn == null) {
					isOn = false;
				}
				if (isOn) {
					on++;
				} else {
					off++;
				}
				
				node.put("id", id);
				if (labels){
					node.put("label", lamp.getId());
				}
				node.put("radius",10);
				node.put("shape","dot");
				node.put("group", isOn ? "On" : "Off");
				
				nodes.add(node);
				for (String other : lamp.getNeighbours()) {
					String otherId = other.substring(10);
					if (!uniqueEdges.contains(otherId + ":" + id)) {
						ObjectNode edge = JOM.createObjectNode();
						edge.put("from", id);
						edge.put("to", otherId);
						edges.add(edge);
						uniqueEdges.add(id + ":" + otherId);
					}
				}
			}
			result.put("nodes", nodes);
			result.put("edges", edges);
			result.put("on", on);
			result.put("off", off);
		} else {
			result.put("init", false);
			result.put("nodes", JOM.createArrayNode());
			result.put("edges", JOM.createArrayNode());
			result.put("on", false);
			result.put("off", false);
		}
		return result;
	}
	
	private void genFully(AgentHost host, int agentCount, int stepSize, String agentType)
			throws JSONRPCException, InstantiationException,
			IllegalAccessException, InvocationTargetException,
			NoSuchMethodException, IOException, ClassNotFoundException {
		ArrayList<String> agents = new ArrayList<String>(agentCount);
		for (int i = 0; i < agentCount; i++) {
			String agentId = "lamp" + i;
			LampAgent agent = (LampAgent)host.createAgent(agentType, agentId);
			agents.add(agentId);
			ArrayList<String> neighbours = new ArrayList<String>(agentCount - 1);
			for (int j = 0; j < agentCount; j++) {
				if (j == i) {
					continue;
				}
				neighbours.add("local:lamp" + j);
			}
			agent.create(neighbours, stepSize);
		}
		getState().put("agents", agents);
	}
	
	private void genCircle(AgentHost host, int agentCount, int stepSize, String agentType)
			throws JSONRPCException, InstantiationException,
			IllegalAccessException, InvocationTargetException,
			NoSuchMethodException, IOException, ClassNotFoundException {
		ArrayList<String> agents = new ArrayList<String>(agentCount);
		for (int i = 0; i < agentCount; i++) {
			String agentId = "lamp" + i;
			LampAgent agent = (LampAgent)host.createAgent(agentType, agentId);
			agents.add(agentId);
			ArrayList<String> neighbours = new ArrayList<String>(2);
			neighbours.add("local:lamp" + (agentCount + i - 1) % agentCount);
			neighbours.add("local:lamp" + (i + 1) % agentCount);
			agent.create(neighbours, stepSize);
		}
		getState().put("agents", agents);
	}
	
	private void genLine(AgentHost host, int agentCount, int stepSize, String agentType)
			throws JSONRPCException, InstantiationException,
			IllegalAccessException, InvocationTargetException,
			NoSuchMethodException, IOException, ClassNotFoundException {
		ArrayList<String> agents = new ArrayList<String>(agentCount);
		for (int i = 0; i < agentCount; i++) {
			String agentId = "lamp" + i;
			LampAgent agent = (LampAgent)host.createAgent(agentType, agentId);
			agents.add(agentId);
			ArrayList<String> neighbours = new ArrayList<String>(2);
			if (i > 0) {
				neighbours.add("local:lamp" + (i - 1));
			}
			if (i < agentCount - 1) {
				neighbours.add("local:lamp" + (i + 1));
			}
			agent.create(neighbours, stepSize);
		}
		getState().put("agents", agents);
	}
	
	private void genStar(AgentHost host, int agentCount, int stepSize, String agentType)
			throws JSONRPCException, InstantiationException,
			IllegalAccessException, InvocationTargetException,
			NoSuchMethodException, IOException, ClassNotFoundException {
		ArrayList<String> agents = new ArrayList<String>(agentCount);
		LampAgent agent = (LampAgent)host.createAgent(agentType, "lamp0");
		agents.add("lamp0");
		ArrayList<String> neighbours = new ArrayList<String>(agentCount);
		for (int i = 1; i < agentCount; i++) {
			String agentId = "lamp" + i;
			LampAgent leafAgent = (LampAgent)host.createAgent(agentType, agentId);
			agents.add(agentId);
			
			ArrayList<String> locNeighbours = new ArrayList<String>(0);
			locNeighbours.add("local:lamp0");
			neighbours.add("local:lamp" + i);
			leafAgent.create(locNeighbours, stepSize);
		}
		agent.create(neighbours, stepSize);
		getState().put("agents", agents);
	}
	
	private void genBinaryTree(AgentHost host, int agentCount, int stepSize, String agentType)
			throws JSONRPCException, InstantiationException,
			IllegalAccessException, InvocationTargetException,
			NoSuchMethodException, IOException, ClassNotFoundException {
		ArrayList<String> agents = new ArrayList<String>(agentCount);
		
		int level = 0;
		int first = 0;
		int nextFirst = 1;
		int count = 0;
		while (count < agentCount) {
			if (count == nextFirst) {
				level++;
				first = count;
				nextFirst = (int) (count + Math.pow(2, level));
			}
			String agentId = "lamp" + count;
			LampAgent agent = (LampAgent)host.createAgent(agentType, agentId);
			agents.add(agentId);
			ArrayList<String> neighbours = new ArrayList<String>(2);
			int child = nextFirst + 2 * (count - first);
			if (child < agentCount) {
				neighbours.add("local:lamp" + child);
			}
			if (child + 1 < agentCount) {
				neighbours.add("local:lamp" + (child + 1));
			}
			if (count > 0) {
				int parent = (int) (first - Math.pow(2, level - 1) + Math
						.floor((count - first) / 2));
				neighbours.add("local:lamp" + parent);
			}
			agent.create(neighbours, stepSize);
			count++;
		}
		getState().put("agents", agents);
	}
	
}
