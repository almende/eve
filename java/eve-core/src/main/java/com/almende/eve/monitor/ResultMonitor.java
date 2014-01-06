package com.almende.eve.monitor;

import java.net.URI;
import java.util.List;

public interface ResultMonitor {

	String getId();

	void init();

	String getCallbackMethod();

	boolean hasCache();

	CacheInterface getCache();

	URI getUrl();

	String getMethod();

	String getParams();

	void cancel();

	ResultMonitor add(ResultMonitorConfigType config);

	String store();

	void addCache(CacheInterface config);

	void addPoll(PollInterface config);

	void addPush(PushInterface config);

	void setId(String id);

	void setAgentId(String agentId);

	String getAgentId();

	void setUrl(URI url);

	void setMethod(String method);

	void setParams(String params);

	void setCallbackMethod(String callbackMethod);

	List<PollInterface> getPolls();

	void setPolls(List<PollInterface> polls);

	List<PushInterface> getPushes();

	void setPushes(List<PushInterface> pushes);

	String getCacheType();

	void setCacheType(String cacheType);
	
}
