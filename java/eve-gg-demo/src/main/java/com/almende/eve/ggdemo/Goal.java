package com.almende.eve.ggdemo;

import java.io.Serializable;
import java.util.UUID;

public class Goal implements Serializable {

	private static final long	serialVersionUID	= -463898073095891971L;
	private double goalPct = 70;
	private double percentage = 0;
	private int agentCnt = 0;
	private int ttl=0;
	private String id = UUID.randomUUID().toString();
	
	public double getGoalPct() {
		return goalPct;
	}
	public void setGoalPct(double goalPct) {
		this.goalPct = goalPct;
	}
	public double getPercentage() {
		return percentage;
	}
	public void setPercentage(double percentage) {
		this.percentage = percentage;
	}
	public String getId() {
		return id;
	}
	public void setId(String id) {
		this.id = id;
	}
	public int getAgentCnt() {
		return agentCnt;
	}
	public void setAgentCnt(int agentCnt) {
		this.agentCnt = agentCnt;
	}
	public int getTtl() {
		return ttl;
	}
	public void setTtl(int ttl) {
		this.ttl = ttl;
	}
	
	
}
