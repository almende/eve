package com.almende.eve.agent;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.almende.eve.entity.AgentMetaData;
import com.almende.eve.rpc.annotation.Access;
import com.almende.eve.rpc.annotation.AccessType;
import com.almende.eve.rpc.annotation.Name;
import com.almende.eve.rpc.annotation.Required;
import com.almende.util.TwigUtil;
import com.google.appengine.api.datastore.QueryResultIterator;
import com.google.appengine.api.datastore.Query.FilterOperator;
import com.google.code.twig.ObjectDatastore;
import com.google.code.twig.FindCommand.RootFindCommand;
import com.google.code.twig.annotation.AnnotationObjectDatastore;

@Access(AccessType.PUBLIC)
public class ManagementAgent extends Agent {
	@Override
	public void init() {
		TwigUtil.register(AgentMetaData.class);
	}
		
	/**
	 * Create a new agent. Will throw an exception if the agent already exists
	 * @param id
	 * @param type
	 * @return info
	 * @throws Exception
	 */
	public Map<String, Object> create(@Name("id") String id, @Name("type") String type) 
			throws Exception {
		try {
			getAgentHost().createAgent(type, id);	
		
			ObjectDatastore datastore = new AnnotationObjectDatastore();
			
			// remove any old registration
			AgentMetaData meta = datastore.load(AgentMetaData.class, id);
			if (meta != null) {
				datastore.delete(meta);
			}
			
			// store new registration
			meta = new AgentMetaData(type, id);
			datastore.store(meta);
			
			try {
				getEventsFactory().trigger("create", toInfo(meta));
			}
			catch (Exception e) {}
			
			return toInfo(meta);
		}
		catch (Exception e) {
			// failed to create. 
			// The agent probably already exists but is not registered.
			register(id);
			
			throw e;
		}
	}

	/**
	 * Register an existing agent.
	 * @param id
	 * @return info
	 * @throws Exception
	 */
	public Map<String, Object> register(@Name("id") String id) throws Exception {
		AgentHost factory = getAgentHost();
		Agent agent = factory.getAgent(id);
		if (agent != null) {
			ObjectDatastore datastore = new AnnotationObjectDatastore();
			
			// remove any old registration
			AgentMetaData meta = datastore.load(AgentMetaData.class, id);
			if (meta != null) {
				datastore.delete(meta);
			}
			
			// store new registration
			meta = new AgentMetaData(agent.getClass().getName(), agent.getId());
			datastore.store(meta);
			
			try {
				getEventsFactory().trigger("register", toInfo(meta));
			}
			catch (Exception e) {}
			
			return toInfo(meta);
		}
		return null;
	}
	
	/**
	 * Unregister an existing agent.
	 * @param id
	 * @throws Exception
	 */
	public void unregister(@Name("id") String id) throws Exception {
		ObjectDatastore datastore = new AnnotationObjectDatastore();
		AgentMetaData meta = datastore.load(AgentMetaData.class, id);
		if (meta != null) {
			try {
				getEventsFactory().trigger("unregister", toInfo(meta));
				datastore.delete(meta);
			}
			catch (Exception e) {}
		}
	}

	/**
	 * Delete an agent
	 * @param id
	 * @return
	 * @throws Exception
	 */
	public void delete(@Name("id") String id) throws Exception {
		ObjectDatastore datastore = new AnnotationObjectDatastore();
		AgentMetaData meta = datastore.load(AgentMetaData.class, id);
		if (meta != null) {
			try {
				getEventsFactory().trigger("delete", toInfo(meta));
			}
			catch (Exception e) {}
			
			datastore.delete(meta);
		}		

		getAgentHost().deleteAgent(id);
	}
	
	/**
	 * List all agents.
	 * @param type  filter by agent type. optional.
	 * @return agents     meta information of all agents: id, type, urls
	 * @throws Exception
	 */
	public List<Map<String, Object>> list(
			@Name("type") @Required(false) String type) throws Exception {
		ObjectDatastore datastore = new AnnotationObjectDatastore();
		RootFindCommand<AgentMetaData> command = datastore.find()
			.type(AgentMetaData.class);
		if (type != null) {
			command = command.addFilter("type", FilterOperator.EQUAL, type);
		}

		List<Map<String, Object>> info = new ArrayList<Map<String,Object>>();
		QueryResultIterator<AgentMetaData> it = command.now();
		while (it.hasNext()) {
			AgentMetaData meta = it.next();
			Map<String, Object> i = toInfo(meta);
			if (i != null) {
				info.add(i);
			}
			else {
				// agent does not exist anymore. Delete meta information.
				datastore.delete(meta);
			}
		}		
		
		return info;
	}
	
	/**
	 * Get information from a meta description of an agent. Returns null
	 * if the agent does not exist.
	 * @param meta
	 * @return info
	 * @throws Exception
	 */
	private Map<String, Object> toInfo(AgentMetaData meta) throws Exception {
		Map<String, Object> info = new HashMap<String, Object>();
		info.put("id", meta.getId());
		info.put("type", meta.getType());
		
		Agent agent = getAgentHost().getAgent(meta.getId());
		if (agent != null) {
			info.put("urls", agent.getUrls());
			return info;
		}
		else {
			return null;
		}		
	}

	/**
	 * Retrieve an agents info. 
	 * If the agent does not exist, null will be returned.
	 * @param id
	 * @return info
	 * @throws Exception
	 */
	public Map<String, Object> get(@Name("id") String id) throws Exception {
		ObjectDatastore datastore = new AnnotationObjectDatastore();
		AgentMetaData meta = datastore.load(AgentMetaData.class, id);
		if (meta != null) {
			return toInfo(meta);
		}
		else {
			Agent agent = getAgentHost().getAgent(id);
			if (agent != null) {
				return register(id);
			}
		}
		return null;
	}

	/**
	 * Test if an agent exists
	 * @param id
	 * @return exists
	 * @throws Exception
	 */
	public boolean exists(@Name("id") String id) throws Exception {
		Agent agent = getAgentHost().getAgent(id);
		return (agent != null);
	}
	
	@Override
	public String getDescription() {
		return "I can create, delete, and list agents.";
	}

	@Override
	public String getVersion() {
		return "0.2";
	}
}
