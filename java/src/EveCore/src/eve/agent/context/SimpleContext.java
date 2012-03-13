package eve.agent.context;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

// TODO: rename class SimpleContext, and make it stored (maybe TemporaryContext?)
@SuppressWarnings("serial")
public class SimpleContext implements AgentContext, Serializable {
	private String servletUrl = null;
	private String agentClass = null;
	private String id = null;
	private Map<String, Object> properties = new HashMap<String, Object>();
	
	public SimpleContext() {}

	public SimpleContext(Map<String, Object> properties) {
		if (properties != null) {
			this.properties = properties;
		}
	}

	@Override
	public void setId(String id) {
		this.id = id;
	}

	@Override
	public String getId() {
		return id;
	}

	@Override
	public void setServletUrlFromRequest (HttpServletRequest req) {
		// TODO: reckon with https
		servletUrl = "http://" + req.getServerName() + ":" + req.getServerPort() + 
			req.getContextPath() + req.getServletPath() + "/";
	}
	
	@Override
	public void setAgentClass(String agentClass) {
		this.agentClass = agentClass;
	}

	@Override
	public String getAgentUrl() {
		String agentUrl = null;
		if (servletUrl != null) {
			agentUrl = servletUrl;
			if (agentClass != null) {
				agentUrl += agentClass;
				if (id != null) {
					agentUrl += "/" + id;
				}
			}
		}
		
		return agentUrl;
	}
	
	@Override
	public Object get(String key) {
		return properties.get(key);
	}

	@Override
	public void put(String key, Object value) {
		properties.put(key, value);
	}

	@Override
	public boolean has(String key) {
		return properties.containsKey(key);
	}
}
