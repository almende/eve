package com.almende.eve.monitor;

import java.net.URI;
import java.util.List;

public interface ResultMonitor {

	String getId();

	void init();

	String getCallbackMethod();

	boolean hasCache();

	Cache getCache();

	URI getUrl();

	String getMethod();

	String getParams();

	void cancel();

	ResultMonitor add(ResultMonitorConfigType config);

	String store();

	void addCache(Cache config);

	void addPoll(Poll config);

	void addPush(Push config);

	void setId(String id);

	void setAgentId(String agentId);

	String getAgentId();

	void setUrl(URI url);

	void setMethod(String method);

	void setParams(String params);

	void setCallbackMethod(String callbackMethod);

	List<Poll> getPolls();

	void setPolls(List<Poll> polls);

	List<Push> getPushes();

	void setPushes(List<Push> pushes);

	String getCacheType();

	void setCacheType(String cacheType);
	
}
