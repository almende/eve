package com.almende.eve.goldemo;

import java.io.Serializable;

import com.almende.eve.rpc.jsonrpc.jackson.JOM;
import com.fasterxml.jackson.core.JsonProcessingException;

public class CycleState implements Serializable {
	
	private static final long	serialVersionUID	= 3063215915628652369L;
	private int					cycle;
	private boolean				alive;
	
	public CycleState() {
	}
	
	public CycleState(int cycle, boolean alive) {
		this.cycle = cycle;
		this.alive = alive;
	}
	
	public int getCycle() {
		return cycle;
	}
	
	public void setCycle(int cycle) {
		this.cycle = cycle;
	}
	
	public boolean isAlive() {
		return alive;
	}
	
	public void setAlive(boolean alive) {
		this.alive = alive;
	}
	public String toString(){
		try {
			return JOM.getInstance().writeValueAsString(this);
		} catch (JsonProcessingException e) {
			return super.toString();
		}
	}
}
