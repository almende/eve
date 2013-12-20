package com.almende.eve.ggdemo;

import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;

import com.almende.eve.agent.Agent;
import com.almende.eve.agent.annotation.ThreadSafe;
import com.almende.eve.rpc.annotation.Access;
import com.almende.eve.rpc.annotation.AccessType;
import com.almende.eve.rpc.annotation.Name;
import com.almende.eve.rpc.annotation.Sender;
import com.almende.eve.rpc.jsonrpc.JSONRPCException;
import com.almende.eve.rpc.jsonrpc.jackson.JOM;
import com.almende.util.TypeUtil;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.node.ObjectNode;

@Access(AccessType.PUBLIC)
@ThreadSafe(true)
public class LinPathAgent extends Agent implements LampAgent  {
	private ArrayList<String>	neighbours	= null;
	
	public void create(@Name("neighbours") ArrayList<String> neighbours,
			@Name("stepSize") Integer stepSize) {
		getState().put("neighbours", neighbours);
		if (stepSize > neighbours.size()){
			stepSize = neighbours.size();
		}
		getState().put("stepSize", stepSize);
	}
	
	public void lampOn() {
		getState().put("lamp", true);
	}
	
	public void lampOff() {
		getState().put("lamp", false);
	}
	
	public boolean isOn() {
		Boolean isOn = getState().get("lamp", Boolean.class);
		if (isOn == null) isOn = false;
		return isOn;
	}
	
	public boolean isOnBlock() throws InterruptedException{
		Boolean isOn = getState().get("lamp", Boolean.class);
		while (isOn == null){
			Thread.sleep(1000);
			isOn = getState().get("lamp", Boolean.class);	
		}
		return isOn;
	}
	
	public ArrayList<String> getNeighbours(){
		ArrayList<String> result = getState().get("neighbours", new TypeUtil<ArrayList<String>>(){});
		if (result == null){
			result = new ArrayList<String>(0);
		}
		return result;
	}
	
	public void handleGoal(@Name("goal") Goal goal, @Sender String sender) throws IOException,
			JSONRPCException, JsonProcessingException {
		if (neighbours == null) {
			neighbours = getNeighbours();
		}
		

		Integer pointer = getState().get(goal.getId(), Integer.class);
		if (pointer == null) {
			for (int i=0; i< neighbours.size(); i++){
				if (neighbours.get(i).equals(sender)){
					pointer=i;
				}
			}
			if (pointer == null){
				pointer=0; //should not happen.
			}
		}
		Integer stepSize = getState().get("stepSize", Integer.class);
		if (!getState().containsKey(goal.getId())) {
			// Determine my own influence on the goal
			double noOn = (goal.getPercentage() * goal.getAgentCnt()) / 100;
			goal.setAgentCnt(goal.getAgentCnt() + 1);
			
			double plus = (((noOn + 1) * 100) / (goal.getAgentCnt()));
			double minus = (((noOn) * 100) / (goal.getAgentCnt()));
			if (plus - goal.getGoalPct() < goal.getGoalPct() - minus) {
				lampOn();
				goal.setPercentage(plus);
			} else {
				lampOff();
				goal.setPercentage(minus);
			}
			goal.setTtl(0);
		} else {
			double noOn = (goal.getPercentage() * goal.getAgentCnt()) / 100;
			double newPerc = (((noOn + (isOn()?-1:1)) * 100) / (goal.getAgentCnt()));
			if (Math.abs(goal.getGoalPct()-goal.getPercentage()) > Math.abs(goal.getGoalPct()-newPerc)){
				goal.setPercentage(newPerc);
				if (isOn()){
					lampOff();
				} else {
					lampOn();
				}
				goal.setTtl(0);
			} else {
				goal.setTtl(goal.getTtl()+1);
			}
		}
		
		if (goal.getTtl() > 15){
			//No changes, drop this goal.
			return;
		}
		// Send goal further to neighbours
		ObjectNode params = JOM.createObjectNode();
		params.put("goal", JOM.getInstance().valueToTree(goal));
		
		int count = 0;
		int original_pointer=pointer;
		boolean stop=false;
		while (count < stepSize && !stop){
			String neighbour = neighbours.get(pointer);
			pointer++;
			if (pointer >= neighbours.size()) {
				pointer = 0;
			}
			getState().put(goal.getId(), pointer);
			if (neighbours.size()>1){
				if (neighbour.equals(sender)){
					continue; 
				}
				if (pointer == original_pointer){
					stop=true;
				}
			}
			count++;
			sendAsync(URI.create(neighbour), "handleGoal", params, null, Void.class);
		}
		getState().put("goal", goal);
	}
	
	public Goal getGoal() {
		return getState().get("goal", Goal.class);
	}
}
