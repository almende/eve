package com.almende.test;

import java.util.Set;

import junit.framework.TestCase;

import org.junit.Test;

import com.almende.eve.agent.AgentFactory;
import com.almende.eve.scheduler.ClockSchedulerFactory;
import com.almende.eve.state.FileStateFactory;
import com.almende.eve.transport.http.HttpService;
import com.almende.test.agents.TestSchedulerAgent;

public class TestScheduler extends TestCase {
	public static Integer count=33;
	
	@Test
	public void testScheduler() throws Exception {
		AgentFactory af = AgentFactory.getInstance();
		if (af == null){
			af = AgentFactory.createInstance();
			af.setStateFactory(new FileStateFactory(".eveagents"));
			af.addTransportService(new HttpService());
		}
		af.setSchedulerFactory(new ClockSchedulerFactory(af,""));
//		af.setSchedulerFactory(new RunnableSchedulerFactory(af,"_runnableScheduler"));
		String agentIds[] = { "myTest1", "myTest2", "myTest3" };

		for (String agentId : agentIds) {
			if (af.hasAgent(agentId)) {
				af.deleteAgent(agentId);
			}
			TestSchedulerAgent agent = (TestSchedulerAgent) af.createAgent(TestSchedulerAgent.class,
					agentId);
			System.err.println("Setup agent:"+agentId);
			agent.setTest(agentId, 1000);
			agent.setTest(agentIds[0], 1500);
			agent.setTest(agentIds[1], 3000);
			agent.setTest(agentIds[2], 4500);
			agent.setTest(agentId, 500);
			agent.setTest(agentIds[0], 2500);
			agent.setTest(agentIds[1], 3500);
			agent.setTest(agentIds[2], 4000);
			agent.setTest(agentId, 2000);
			agent.setTest(agentIds[0], 900);
			agent.setTest(agentIds[1], 3300);
			agent.setTest(agentIds[2], 2300);
			Set<String> taskList = agent.getScheduler().getTasks();
			System.err.println("Tasks list:"+taskList);
			agent.getScheduler().cancelTask(taskList.toArray(new String[0])[0]);
		}
		while (count > 0 ){
			Thread.sleep(1000);
		}
	}
}
