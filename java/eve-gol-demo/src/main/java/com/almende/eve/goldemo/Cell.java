package com.almende.eve.goldemo;

import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;

import com.almende.eve.agent.Agent;
import com.almende.eve.agent.annotation.ThreadSafe;
import com.almende.eve.agent.callback.AsyncCallback;
import com.almende.eve.rpc.annotation.Access;
import com.almende.eve.rpc.annotation.AccessType;
import com.almende.eve.rpc.annotation.Name;
import com.almende.eve.rpc.annotation.Sender;
import com.almende.eve.rpc.jsonrpc.JSONRPCException;
import com.almende.eve.rpc.jsonrpc.jackson.JOM;
import com.almende.util.TypeUtil;
import com.fasterxml.jackson.databind.node.ObjectNode;

@Access(AccessType.PUBLIC)
@ThreadSafe(true)
public class Cell extends Agent {
	private ArrayList<String> neighbors = null;
	
	public void create(@Name("neighbors") ArrayList<String> neighbors,
			@Name("state") Boolean initState) {
		
		getState().put("neighbors", neighbors);
		getState().put("val_0", new CycleState(0, initState));
		getState().put("current_cycle", 1);
		
	}
	
	public void register() throws JSONRPCException, IOException {
		if (neighbors == null){
			neighbors = getState().get("neighbors",
					new TypeUtil<ArrayList<String>>() {
					});
		}
		for (String neighbor : neighbors) {
			getEventsFactory().subscribe(URI.create(neighbor),
					"cycleCalculated", "askCycleState");
		}
	}
	
	public void stop() throws IOException, JSONRPCException{
		if (neighbors == null){
			neighbors = getState().get("neighbors",
					new TypeUtil<ArrayList<String>>() {
					});
		}
		for (String neighbor : neighbors) {
			getEventsFactory().unsubscribe(URI.create(neighbor),
					"cycleCalculated", "askCycleState");
		}
	}
	
	public void start() throws IOException {
		getEventsFactory().trigger("cycleCalculated");
	}
	
	public void askCycleState(@Sender final String neighbor) throws JSONRPCException,
			IOException {
		
		ObjectNode params = JOM.createObjectNode();
		params.put("cycle", getState().get("current_cycle", Integer.class) - 1);
		sendAsync(URI.create(neighbor), "getCycleState", params, new AsyncCallback<CycleState>(){

			@Override
			public void onSuccess(CycleState state) {
				if (state != null) {
					getState().put(neighbor + "_" + state.getCycle(), state);
					calcCycle();
				}
			}

			@Override
			public void onFailure(Exception exception) {
				// TODO Auto-generated method stub
				
			}
			
		},
		CycleState.class);
	}
	
	//TODO: find a way to do this without synchronized
	private synchronized void calcCycle() {
		if (getState().containsKey("current_cycle")) {
			Integer currentCycle = getState().get("current_cycle",
					Integer.class);

			if (neighbors == null){
				neighbors = getState().get("neighbors",
						new TypeUtil<ArrayList<String>>() {
						});
			}

			int aliveNeighbors = 0;
			for (String neighbor : neighbors) {
				if (!getState()
						.containsKey(neighbor + "_" + (currentCycle - 1))) {
					return;
				}
				CycleState nState = getState().get(
						neighbor + "_" + (currentCycle - 1), CycleState.class);
				if (nState.isAlive()) aliveNeighbors++;
			}
			CycleState myState = getState().get("val_" + (currentCycle - 1),
					CycleState.class);
			if (aliveNeighbors < 2 || aliveNeighbors > 3) {
				getState().put("val_" + currentCycle,
						new CycleState(currentCycle, false));
			} else if (aliveNeighbors == 3) {
				getState().put("val_" + currentCycle,
						new CycleState(currentCycle, true));
			} else {
				getState().put("val_" + currentCycle,
						new CycleState(currentCycle, myState.isAlive()));
			}
			getState().put("current_cycle", currentCycle + 1);
			try {
				getEventsFactory().trigger("cycleCalculated");
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}
	
	public CycleState getCycleState(@Name("cycle") Integer cycle) {
		if (getState().containsKey("val_" + cycle)) {
			return getState().get("val_" + cycle, CycleState.class);
		}
		return null;
	}
	
	public ArrayList<CycleState> getAllCycleStates() {
		ArrayList<CycleState> result = new ArrayList<CycleState>();
		int count = 0;
		while (getState().containsKey("val_" + count)) {
			result.add(getState().get("val_" + count, CycleState.class));
			count++;
		}
		return result;
	}
}
