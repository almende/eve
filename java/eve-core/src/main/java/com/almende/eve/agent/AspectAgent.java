package com.almende.eve.agent;

import com.almende.eve.agent.annotation.Namespace;
import com.almende.eve.rpc.annotation.Access;
import com.almende.eve.rpc.annotation.AccessType;
import com.almende.eve.state.State;

public class AspectAgent<T> extends Agent implements AgentInterface {
	private State myState = null;
	private T aspect = null;
	
	
	
	public void init(T agentAspect){
		this.aspect = agentAspect;
		myState = getState();
		myState.put("_aspectType", agentAspect.getClass().getName());
	}
	
	@SuppressWarnings("unchecked")
	@Namespace("sub")
	@Access(AccessType.PUBLIC)
	public T getAspect(){
		if (aspect == null){
			String AspectType = getState().get("_aspectType",String.class);
			try {
				aspect = (T) Class.forName(AspectType).getConstructor().newInstance();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		return aspect;
	}
}
