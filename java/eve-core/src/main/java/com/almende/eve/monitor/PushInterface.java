package com.almende.eve.monitor;

import java.io.IOException;

import com.almende.eve.agent.AgentInterface;
import com.almende.eve.rpc.jsonrpc.JSONRPCException;

public interface PushInterface extends ResultMonitorConfigType {

	void setInterval(int interval);

	String getEvent();

	void setEvent(String event);


	void setPushId(String pushId);
	int getInterval();

	String getPushId();

	void cancel(ResultMonitor monitor, AgentInterface agent)
			throws IOException, JSONRPCException;

	void init(ResultMonitor monitor, AgentInterface agent) throws IOException,
			JSONRPCException;

	PushInterface onChange();

	PushInterface onEvent(String event);

	PushInterface onEvent();

	PushInterface onInterval(int interval);
	
}
