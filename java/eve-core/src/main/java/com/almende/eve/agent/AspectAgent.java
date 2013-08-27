package com.almende.eve.agent;

import java.util.logging.Level;
import java.util.logging.Logger;

import com.almende.eve.agent.annotation.Namespace;
import com.almende.eve.state.State;

/**
 * Base class for aspect based agents. These agents have a namespace "sub",
 * which is used to address the class that was given at instantiation time.
 * 
 * @author ludo
 * 
 * @param <T>
 */
public class AspectAgent<T> extends Agent implements AgentInterface {
	private static final Logger	LOG		= Logger.getLogger(AspectAgent.class
												.getCanonicalName());
	private State				myState	= null;
	private T					aspect	= null;
	
	@Override
	public void init() {
		myState = getState();
	}
	
	public void init(Class<? extends T> agentAspect) {
		myState = getState();
		myState.put("_aspectType", agentAspect.getName());
	}
	
	@SuppressWarnings("unchecked")
	@Namespace("aspect")
	public T getAspect() {
		if (aspect == null) {
			String AspectType = myState.get("_aspectType", String.class);
			try {
				aspect = (T) Class.forName(AspectType).getConstructor()
						.newInstance();
			} catch (Exception e) {
				LOG.log(Level.WARNING,"",e);
			}
		}
		return aspect;
	}
}
