package com.almende.eve.monitor;

import java.io.IOException;

import com.almende.eve.agent.AgentInterface;
import com.almende.eve.rpc.jsonrpc.JSONRPCException;

public interface Push extends ResultMonitorConfigType {

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

	Push onChange();

	Push onEvent(String event);

	Push onEvent();

	Push onInterval(int interval);
	
}
