package com.almende.eve.goldemo;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.HashMap;

import com.almende.eve.agent.AgentHost;
import com.almende.eve.rpc.jsonrpc.JSONRPCException;
import com.almende.eve.scheduler.ClockSchedulerFactory;
import com.almende.eve.state.MemoryStateFactory;

public class Goldemo {
//	final static String	PATH	= "zmq:ipc:///tmp/zmq-socket-";
	final static String	PATH	= "local:";
	
	public static void main(String[] args) throws IOException,
			JSONRPCException, ClassNotFoundException, InstantiationException,
			IllegalAccessException, InvocationTargetException,
			NoSuchMethodException {
		AgentHost host = AgentHost.getInstance();
		host.setDoesShortcut(false);
		
		// host.setStateFactory(new FileStateFactory(".eveagents_gol",true));
		host.setStateFactory(new MemoryStateFactory());
		
/*		HashMap<String, Object> params = new HashMap<String, Object>();
		params.put("baseUrl", "ipc:///tmp/zmq-socket-");
		host.addTransportService(new ZmqService(host, params));
*/	
		host.setSchedulerFactory(new ClockSchedulerFactory(host,
				"_myRunnableScheduler"));
		// host.setSchedulerFactory(new RunnableSchedulerFactory(host,
		// "_myRunnableScheduler"));
		
		if (args.length < 3) {
			throw new IllegalArgumentException(
					"Please use at least 3 arguments: X seconds & N rows & M columns");
		}
		Integer X = Integer.valueOf(args[0]);
		Integer N = Integer.valueOf(args[1]);
		Integer M = Integer.valueOf(args[2]);
		
		Boolean annimate = false;
		if (args.length > 3) {
			annimate = Boolean.valueOf(args[3]);
		}
		
		boolean[][] grid = new boolean[N][M];
		BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
		
		String input;
		
		int cN = 0;
		while ((input = br.readLine()) != null && cN < N) {
			String trimmedInput = input.trim();
			if (trimmedInput.isEmpty()) break;
			if (trimmedInput.length() != M) throw new IllegalArgumentException(
					"Incorrect input line detected:" + input);
			for (int cM = 0; cM < M; cM++) {
				grid[cN][cM] = (trimmedInput.charAt(cM) == '+');
				createAgent(host, N, M, cN, cM,
						(trimmedInput.charAt(cM) == '+'));
			}
			cN++;
		}
		for (cN = 0; cN < N; cN++) {
			for (int cM = 0; cM < M; cM++) {
				Cell cell = (Cell) host.getAgent("agent_" + cN + "_" + cM);
				cell.register();
			}
		}
		for (cN = 0; cN < N; cN++) {
			for (int cM = 0; cM < M; cM++) {
				Cell cell = (Cell) host.getAgent("agent_" + cN + "_" + cM);
				cell.start();
			}
		}
		try {
			Thread.sleep(X * 1000);
		} catch (InterruptedException e) {
			System.err.println("Early interrupt");
		}
		for (cN = 0; cN < N; cN++) {
			for (int cM = 0; cM < M; cM++) {
				Cell cell = (Cell) host.getAgent("agent_" + cN + "_" + cM);
				cell.stop();
			}
		}
		HashMap<String, ArrayList<CycleState>> results = new HashMap<String, ArrayList<CycleState>>();
		int max_full=0;
		for (cN = 0; cN < N; cN++) {
			for (int cM = 0; cM < M; cM++) {
				Cell cell = (Cell) host.getAgent("agent_" + cN + "_" + cM);
				ArrayList<CycleState> res = cell.getAllCycleStates();
				max_full = (max_full==0||max_full>res.size()?res.size():max_full);
				results.put(cell.getId(), res);
			}
		}
		int cycle = 0;
		for (int j=0; j<max_full; j++) {
			if (annimate) {
				try {
					Thread.sleep(500);
				} catch (InterruptedException e) {
				}
				final String ESC = "\033[";
				System.out.print(ESC + "2J");
			}
			System.out.println("Cycle:" + cycle +"/"+(max_full-1));
			System.out.print("/");
			for (int i = 0; i < M * 2; i++) {
				System.out.print("-");
			}
			System.out.println("-\\");
			for (cN = 0; cN < N; cN++) {
				System.out.print("| ");
				for (int cM = 0; cM < M; cM++) {
					String id = ("agent_" + cN + "_" + cM);
					ArrayList<CycleState> states = results.get(id);
					if (states.size() <= cycle) {
						break;
					}
					System.out.print(states.get(cycle).isAlive() ? "# " : "- ");
				}
				System.out.println("|");
			}
			System.out.print("\\");
			for (int i = 0; i < M * 2; i++) {
				System.out.print("-");
			}
			System.out.println("-/");
			cycle++;
		}
		// System.out.println(results);
		System.exit(0);
	}
	
	public static void createAgent(AgentHost host, int N, int M, int cN,
			int cM, boolean state) throws JSONRPCException,
			InstantiationException, IllegalAccessException,
			InvocationTargetException, NoSuchMethodException, IOException {
		
		String agentId = "agent_" + cN + "_" + cM;
		ArrayList<String> neighbors = new ArrayList<String>(8);
		neighbors.add(PATH + "agent_" + ((N + cN - 1) % N) + "_"
				+ ((M + cM - 1) % M));
		neighbors.add(PATH + "agent_" + ((N + cN) % N) + "_"
				+ ((M + cM - 1) % M));
		neighbors.add(PATH + "agent_" + ((N + cN + 1) % N) + "_"
				+ ((M + cM - 1) % M));
		neighbors.add(PATH + "agent_" + ((N + cN - 1) % N) + "_"
				+ ((M + cM) % M));
		neighbors.add(PATH + "agent_" + ((N + cN + 1) % N) + "_"
				+ ((M + cM) % M));
		neighbors.add(PATH + "agent_" + ((N + cN - 1) % N) + "_"
				+ ((M + cM + 1) % M));
		neighbors.add(PATH + "agent_" + ((N + cN) % N) + "_"
				+ ((M + cM + 1) % M));
		neighbors.add(PATH + "agent_" + ((N + cN + 1) % N) + "_"
				+ ((M + cM + 1) % M));
		Cell cell = host.createAgent(Cell.class, agentId);
		cell.create(neighbors, state);
	}
	
}
