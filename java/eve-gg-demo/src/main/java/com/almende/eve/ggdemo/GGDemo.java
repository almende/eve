package com.almende.eve.ggdemo;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;

import com.almende.eve.agent.AgentHostDefImpl;
import com.almende.eve.rpc.jsonrpc.JSONRPCException;
import com.almende.eve.scheduler.ClockSchedulerFactory;
import com.almende.eve.state.MemoryStateFactory;

public class GGDemo {
	
	/**
	 * @param args
	 * @throws IOException
	 * @throws NoSuchMethodException
	 * @throws InvocationTargetException
	 * @throws IllegalAccessException
	 * @throws InstantiationException
	 * @throws JSONRPCException
	 * @throws ClassNotFoundException
	 * @throws InterruptedException
	 */
	public static void main(String[] args) throws JSONRPCException,
			InstantiationException, IllegalAccessException,
			InvocationTargetException, NoSuchMethodException, IOException,
			ClassNotFoundException, InterruptedException {
		AgentHostDefImpl host = AgentHostDefImpl.getInstance();
		host.setStateFactory(new MemoryStateFactory());
		host.setSchedulerFactory(new ClockSchedulerFactory(host, ""));
		if (args.length < 4) {
			throw new IllegalArgumentException(
					"Please use at least 4 arguments: ['line':'circle':'star':'binTree'] lamps goalPct startLampNo (stepSize)");
		}
		String type = args[0];
		Integer agentCount = Integer.valueOf(args[1]);
		Double goalPct = Double.valueOf(args[2]);
		Integer startLamp = Integer.valueOf(args[3]);
		Integer stepSize = 99999;
		if (args.length > 4) {
			stepSize = Integer.valueOf(args[4]);
		}
		final Thread mainThread = Thread.currentThread();
		Runtime.getRuntime().addShutdownHook(new Thread() {
			public void run() {
				mainThread.interrupt();
			}
		});
		
		Goal goal = new Goal();
		goal.setGoalPct(goalPct);
		DemoAgent demo = host.createAgent(DemoAgent.class, "demo");
		
		demo.genTopology(type, agentCount, stepSize, "LinPathAgent");
		demo.startGoal(goal, "lamp"+startLamp);
		
		while (true) {
			int off = 0;
			int on = 0;
			for (int i = 0; i < agentCount; i++) {
				LampAgent agent = (LampAgent) host.getAgent("lamp" + i);
				boolean isOn = agent.isOnBlock();
				System.out.print(isOn ? "#" : "-");
				if (isOn) {
					on++;
				} else {
					off++;
				}
			}
			System.out.println("");
			System.out.println("on:" + on + " off:" + off + " percentage:"
					+ ((on * 100) / (on + off)) + "%");
			Thread.sleep(1000);
		}
	}
	
}
