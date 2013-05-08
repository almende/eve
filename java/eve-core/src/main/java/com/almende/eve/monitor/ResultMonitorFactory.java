package com.almende.eve.monitor;

import com.almende.eve.agent.Agent;
import com.almende.eve.rpc.jsonrpc.jackson.JOM;
import com.fasterxml.jackson.databind.node.ObjectNode;

public class ResultMonitorFactory {
	
	Agent myAgent=null;
	
	
	public ResultMonitorFactory(Agent agent){
		this.myAgent=agent;
	}
	/**
	 * Sets up a monitored RPC call subscription. Conveniency method, which can also be expressed as:
	 * new ResultMonitor(getId(), url,method,params).add(ResultMonitorConfigType config).add(ResultMonitorConfigType config).store();
	 * 
	 * @param url
	 * @param method
	 * @param params
	 * @param callbackMethod
	 * @param confs
	 * @return
	 */
	public String create(String url, String method, ObjectNode params,
			String callbackMethod, ResultMonitorConfigType... confs) {
		ResultMonitor monitor = new ResultMonitor(myAgent.getId(), url, method, params, callbackMethod);
		for (ResultMonitorConfigType config : confs) {
			monitor.add(config);
		}
		return monitor.store();
	}
	
	/**
	 * Gets an actual return value of this monitor subscription. If a cache is
	 * available,
	 * this will return the cached value if the maxAge filter allows this.
	 * Otherwise it will run the actual RPC call (similar to "send");
	 * 
	 * @param monitorId
	 * @param filter_parms
	 * @param returnType
	 * @return
	 * @throws Exception
	 */
	@SuppressWarnings("unchecked")
	public <T> T getResult(String monitorId, ObjectNode filter_parms,
			Class<T> returnType) throws Exception {
		T result = null;
		ResultMonitor monitor = ResultMonitor.getMonitorById(myAgent.getId(), monitorId);
		if (monitor != null) {
			if (monitor.hasCache()) {
				if (monitor.getCache() != null
						&& monitor.getCache().filter(filter_parms)) {
					result = (T) monitor.getCache().get();
				}
			}
			if (result == null) {
				result = myAgent.send(monitor.url, monitor.method, monitor.params,
						returnType);
				if (monitor.hasCache()) {
					monitor.getCache().store(result);
				}
			}
		} else {
			System.err.println("Failed to find monitor!" + monitorId);
		}
		return result;
		
	}
	
	/**
	 * Cancels a running monitor subscription.
	 * 
	 * @param monitorId
	 */
	public void cancel(String monitorId) {
		ResultMonitor monitor = ResultMonitor.getMonitorById(myAgent.getId(), monitorId);
		//TODO: Let the cancelation be managed by the original objects (Pushes/Polls/Caches, etc.)
		if (monitor != null) {
			for (String task : monitor.schedulerIds) {
				myAgent.getScheduler().cancelTask(task);
			}
			for (String remote : monitor.remoteIds) {
				ObjectNode params = JOM.createObjectNode();
				params.put("pushId", remote);
				try {
					myAgent.send(monitor.url, "unregisterPush", params);
				} catch (Exception e) {
					System.err.println("Failed to unregister Push");
					e.printStackTrace();
				}
			}
		}
		monitor.delete();
	}
	
}
