package com.almende.eve.ggdemo;

import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

import com.almende.eve.agent.Agent;
import com.almende.eve.agent.annotation.ThreadSafe;
import com.almende.eve.rpc.annotation.Access;
import com.almende.eve.rpc.annotation.AccessType;
import com.almende.eve.rpc.annotation.Name;
import com.almende.eve.rpc.annotation.Sender;
import com.almende.eve.rpc.jsonrpc.JSONRPCException;
import com.almende.eve.rpc.jsonrpc.JSONRequest;
import com.almende.eve.rpc.jsonrpc.jackson.JOM;
import com.almende.util.TypeUtil;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.node.ObjectNode;

@Access(AccessType.PUBLIC)
@ThreadSafe(true)
public class HolonAgent extends Agent implements LampAgent {
	private Set<String>								neighbours	= null;
	private static final TypeUtil<ArrayList<Sub>>	type		= new TypeUtil<ArrayList<Sub>>() {
																};
	
	public void create(@Name("neighbours") ArrayList<String> nbs,
			@Name("stepSize") Integer stepSize) throws JSONRPCException,
			IOException {
		
		neighbours = new HashSet<String>(nbs);
		getState().put("neighbours", neighbours);
		
		if (stepSize > neighbours.size()) {
			stepSize = neighbours.size();
		}
		getState().put("stepSize", stepSize);
		
		String taskId = getScheduler().createTask(new JSONRequest("checkMerge", null), (int)Math.random() * 1500);
		System.out.println(getId()+ ": schedulertask created:"+taskId + " --> "+getScheduler());
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
	
	public boolean isOnBlock() throws InterruptedException {
		Boolean isOn = getState().get("lamp", Boolean.class);
		while (isOn == null) {
			Thread.sleep(1000);
			isOn = getState().get("lamp", Boolean.class);
		}
		return isOn;
	}
	
	@Access(AccessType.UNAVAILABLE)
	public Set<String> getNeighbours() {
		Set<String> result = getState().get("neighbours",
				new TypeUtil<Set<String>>() {
				});
		if (result == null) {
			result = new HashSet<String>(0);
		}
		return result;
	}
	
	public void checkMerge() throws JSONRPCException, IOException {
		System.out.println(getId() + ": checkMerge()");
		if (getState().containsKey("parent")){
			System.out.println(getId() + ": Already merged, not doubling");
			return;
		}
		if (neighbours == null) {
			neighbours = getNeighbours();
		}
		ArrayList<Sub> subs = getState().get("subs", type);
		if (subs == null) {
			subs = new ArrayList<Sub>(0);
		}
		System.out.println(getId() + ": n:"+neighbours.size()+" - s:"+subs.size());
		if (neighbours.size() - subs.size() == 1) {
			Set<String> nbs = new HashSet<String>(neighbours);
			int size = 1;
			for (Sub sub : subs) {
				nbs.remove(sub.getAddress());
				size += sub.getSize();
			}
			String neighbour = nbs.toArray(new String[0])[0];
			getState().put("parent", neighbour);
			
			ObjectNode params = JOM.createObjectNode();
			params.put("size", size);
			try {
				System.out.println(getId() + ": Merging with "+neighbour);
				Boolean res = send(URI.create(neighbour), "merge", params,Boolean.class);
				if (!res){
					getState().remove("parent");
					getScheduler().createTask(new JSONRequest("checkMerge", null),
							(int)Math.random() * 500);
				} else {
					System.out.println(getId() + ": Merged with "+neighbour);	
				}
			} catch (Exception e) {
				getScheduler().createTask(new JSONRequest("checkMerge", null),
						(int)Math.random() * 500);
			}
		}
	}
	
	public Boolean merge(@Name("size") int size, @Sender String sender)
			throws JSONRPCException, IOException {
		if (neighbours == null) {
			neighbours = getNeighbours();
		}
		String parent = getState().get("parent",String.class);
		if (parent != null && parent.equals(sender)){
			return false;
		}
		ArrayList<Sub> oldsubs = getState().get("subs", type);
		ArrayList<Sub> subs = null;
		if (oldsubs == null) {
			subs = new ArrayList<Sub>();
		} else {
			subs = type.inject(oldsubs.clone());
		}
		for (Sub sub : subs) {
			if (sub.getAddress().equals(sender)) {
				return true;
			}
		}
		if (!neighbours.contains(sender)) {
			System.err.println("Merge requested by non-neighbour??? : "
					+ sender);
			return false;
		}
		Sub sub = new Sub();
		sub.setAddress(sender);
		sub.setSize(size);
		subs.add(sub);
		if (!getState().putIfUnchanged("subs", subs, oldsubs)) {
			merge(size, sender);
		}
		getScheduler().createTask(new JSONRequest("checkMerge", null), 0);
		return true;
	}
	
	public void handleTask(@Name("count") Integer count, @Sender String sender)
			throws IOException, JSONRPCException {
		if (sender != null
				&& !getState().get("parent", String.class).equals(sender)) {
			System.out.println("Warning: got task from non-parent!!!" + sender);
		}
		if (count > 0) {
			lampOn();
			count--;
		} else {
			lampOff();
		}
		
		ArrayList<Sub> subs = getState().get("subs", type);
		if (subs == null) {
			return;
		}
		for (Sub sub : subs) {
			int cnt = Math.min(count, sub.getSize());
			count -= cnt;
			ObjectNode params = JOM.createObjectNode();
			params.put("count", cnt);
			sendAsync(URI.create(sub.getAddress()), "handleTask", params, null,
					Void.class);
		}
		
	}
	
	public void handleGoal(@Name("goal") Goal goal, @Sender String sender)
			throws IOException, JSONRPCException, JsonProcessingException {
		String parent = getState().get("parent", String.class);
		
		if (parent != null) {
			System.err.println(getId()
					+ ": HandleGoal received, sending it to my parent:"
					+ parent);
			ObjectNode params = JOM.createObjectNode();
			params.put("goal", JOM.getInstance().valueToTree(goal));
			sendAsync(URI.create(parent), "handleGoal", params, null,
					Void.class);
		} else {
			System.err.println(getId()
					+ ": HandleGoal received, handling it myself");
			
			Set<String> nbs = new HashSet<String>(neighbours);
			
			ArrayList<Sub> subs = getState().get("subs", type);
			
			int subSize = 0;
			if (subs != null) {
				for (Sub sub : subs) {
					nbs.remove(sub.getAddress());
					subSize += sub.getSize();
				}
			}
			Integer pointer = getState().get(goal.getId(), Integer.class);
			if (pointer == null) {
				for (int i = 0; i < nbs.size(); i++) {
					if (nbs.toArray(new String[0])[i].equals(sender)) {
						pointer = i;
					}
				}
				if (pointer == null) {
					pointer = 0;
				}
			}
			Integer stepSize = getState().get("stepSize", Integer.class);
			if (!getState().containsKey(goal.getId())) {
				// Determine my own influence on the goal
				double noOn = (goal.getPercentage() * goal.getAgentCnt()) / 100;
				goal.setAgentCnt(goal.getAgentCnt() + subSize + 1);
				
				double max = (((noOn + subSize + 1) * 100) / (goal
						.getAgentCnt()));
				double min = (((noOn) * 100) / (goal.getAgentCnt()));
				int subOn = -1;
				if (max < goal.getGoalPct()) {
					subOn = subSize + 1;
				} else if (min > goal.getGoalPct()) {
					subOn = 0;
				} else {
					double noLampsOn = (goal.getGoalPct() * goal.getAgentCnt()) / 100;
					subOn = (int) Math.round(noLampsOn - noOn);
				}
				handleTask(subOn, null);
				goal.setPercentage((noOn + subOn) * 100 / goal.getAgentCnt());
				goal.setTtl(0);
			} else {
				goal.setTtl(goal.getTtl() + 1);
			}
			getState().put("goal", goal);
			
			if (goal.getTtl() > 15) {
				// No changes, drop this goal.
				return;
			}
			if (nbs.size() == 0) {
				return;
			}
			// Send goal further to neighbours
			ObjectNode params = JOM.createObjectNode();
			params.put("goal", JOM.getInstance().valueToTree(goal));
			
			int count = 0;
			int original_pointer = pointer;
			boolean stop = false;
			
			while (count < stepSize && !stop) {
				String neighbour = nbs.toArray(new String[0])[pointer];
				pointer++;
				if (pointer >= nbs.size()) {
					pointer = 0;
				}
				getState().put(goal.getId(), pointer);
				if (nbs.size() > 1) {
					if (neighbour.equals(sender)) {
						continue;
					}
					if (pointer == original_pointer) {
						stop = true;
					}
				}
				count++;
				sendAsync(URI.create(neighbour), "handleGoal", params, null,
						Void.class);
			}
		}
	}
	
	public Goal getGoal() {
		return getState().get("goal", Goal.class);
	}
	
	class Sub {
		private int		size	= 0;
		private String	address	= "";
		
		public Sub() {
		}
		
		public int getSize() {
			return size;
		}
		
		public void setSize(int size) {
			this.size = size;
		}
		
		public String getAddress() {
			return address;
		}
		
		public void setAddress(String address) {
			this.address = address;
		}
	}
}
